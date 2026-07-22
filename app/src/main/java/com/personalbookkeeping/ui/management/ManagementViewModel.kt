package com.personalbookkeeping.ui.management

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.personalbookkeeping.domain.model.AccountType
import com.personalbookkeeping.domain.model.CategoryKind
import com.personalbookkeeping.domain.model.ManagedAccount
import com.personalbookkeeping.domain.model.ManagedCategory
import com.personalbookkeeping.domain.model.ManagementResult
import com.personalbookkeeping.domain.model.MoveDirection
import com.personalbookkeeping.domain.repository.ManagementRepository
import com.personalbookkeeping.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ManagementUiState(
    val isLoading: Boolean = true,
    val accounts: List<ManagedAccount> = emptyList(),
    val expenseCategories: List<ManagedCategory> = emptyList(),
    val incomeCategories: List<ManagedCategory> = emptyList(),
    val message: String? = null,
)

class ManagementViewModel(
    private val repository: ManagementRepository,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ManagementUiState())
    val state: StateFlow<ManagementUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { transactionRepository.initialize() }
                .onFailure { mutableState.update { it.copy(isLoading = false, message = "本地账本初始化失败") } }
            combine(
                repository.observeAccounts(),
                repository.observeCategories(CategoryKind.EXPENSE),
                repository.observeCategories(CategoryKind.INCOME),
            ) { accounts, expenses, incomes -> Triple(accounts, expenses, incomes) }
                .collect { (accounts, expenses, incomes) ->
                    mutableState.update {
                        it.copy(
                            isLoading = false,
                            accounts = accounts,
                            expenseCategories = expenses,
                            incomeCategories = incomes,
                        )
                    }
                }
        }
    }

    fun saveAccount(id: String?, name: String, type: AccountType, opening: String, include: Boolean) =
        operate { repository.saveAccount(id, name, type, opening, include) }

    fun deactivateAccount(id: String) = operate { repository.deactivateAccount(id) }
    fun moveAccount(id: String, direction: MoveDirection) = operate { repository.moveAccount(id, direction) }
    fun saveCategory(id: String?, kind: CategoryKind, name: String) =
        operate { repository.saveCategory(id, kind, name) }
    fun deactivateCategory(id: String) = operate { repository.deactivateCategory(id) }
    fun moveCategory(id: String, direction: MoveDirection) = operate { repository.moveCategory(id, direction) }
    fun consumeMessage() = mutableState.update { it.copy(message = null) }

    private fun operate(block: suspend () -> ManagementResult) {
        viewModelScope.launch {
            val result = runCatching { block() }.getOrElse {
                mutableState.update { state -> state.copy(message = "保存失败，请重试") }
                return@launch
            }
            val message = when (result) {
                is ManagementResult.Success -> "已保存"
                ManagementResult.EmptyName -> "名称不能为空"
                ManagementResult.DuplicateName -> "活动项目中已有同名项"
                ManagementResult.LastActiveItem -> "至少要保留一个活动项目"
                ManagementResult.InvalidBalance -> "期初余额格式不正确"
                ManagementResult.NotFound -> "项目不存在或已变更"
            }
            mutableState.update { it.copy(message = message) }
        }
    }
}

class ManagementViewModelFactory(
    private val repository: ManagementRepository,
    private val transactionRepository: TransactionRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ManagementViewModel::class.java))
        return ManagementViewModel(repository, transactionRepository) as T
    }
}
