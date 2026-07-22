package com.personalbookkeeping.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.personalbookkeeping.common.MoneyParseFailure
import com.personalbookkeeping.domain.model.AccountOption
import com.personalbookkeeping.domain.model.CategoryKind
import com.personalbookkeeping.domain.model.CategoryOption
import com.personalbookkeeping.domain.model.RecentTransaction
import com.personalbookkeeping.domain.model.TransactionType
import com.personalbookkeeping.domain.repository.TransactionRepository
import com.personalbookkeeping.domain.usecase.CreateTransactionCommand
import com.personalbookkeeping.domain.usecase.CreateTransactionResult
import com.personalbookkeeping.domain.usecase.CreateTransactionUseCase
import com.personalbookkeeping.domain.validation.TransactionValidationError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SaveStatus {
    IDLE,
    SAVED,
    FAILED,
}

data class TransactionEditorUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val amountText: String = "",
    val note: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val accounts: List<AccountOption> = emptyList(),
    val categories: List<CategoryOption> = emptyList(),
    val selectedAccountId: String? = null,
    val selectedTargetAccountId: String? = null,
    val selectedCategoryId: String? = null,
    val amountError: MoneyParseFailure? = null,
    val validationErrors: Set<TransactionValidationError> = emptySet(),
    val saveStatus: SaveStatus = SaveStatus.IDLE,
    val recentTransactions: List<RecentTransaction> = emptyList(),
) {
    val visibleCategories: List<CategoryOption>
        get() {
            val kind = when (type) {
                TransactionType.EXPENSE -> CategoryKind.EXPENSE
                TransactionType.INCOME -> CategoryKind.INCOME
                TransactionType.TRANSFER -> return emptyList()
            }
            return categories.filter { it.kind == kind }
        }
}

class TransactionEditorViewModel(
    private val createTransaction: CreateTransactionUseCase,
    private val repository: TransactionRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(TransactionEditorUiState())
    val state: StateFlow<TransactionEditorUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                repository.initialize()
                combine(
                    repository.observeEditorOptions(),
                    repository.observeRecentTransactions(),
                ) { options, recent -> options to recent }
                    .collect { (options, recent) ->
                        mutableState.update { current ->
                            current.copy(
                                isLoading = false,
                                accounts = options.accounts,
                                categories = options.categories,
                                recentTransactions = recent,
                            ).withValidSelections()
                        }
                    }
            } catch (_: Exception) {
                mutableState.update { it.copy(isLoading = false, saveStatus = SaveStatus.FAILED) }
            }
        }
    }

    fun onAmountChanged(value: String) {
        if (value.length > MAX_AMOUNT_INPUT_LENGTH) return
        mutableState.update {
            it.copy(
                amountText = value,
                amountError = null,
                validationErrors = emptySet(),
                saveStatus = SaveStatus.IDLE,
            )
        }
    }

    fun onNoteChanged(value: String) {
        if (value.length > MAX_NOTE_INPUT_LENGTH) return
        mutableState.update {
            it.copy(note = value, validationErrors = emptySet(), saveStatus = SaveStatus.IDLE)
        }
    }

    fun onTypeSelected(type: TransactionType) {
        mutableState.update {
            it.copy(
                type = type,
                amountError = null,
                validationErrors = emptySet(),
                saveStatus = SaveStatus.IDLE,
            ).withValidSelections(forceCategoryReset = true)
        }
    }

    fun onAccountSelected(accountId: String) {
        mutableState.update {
            it.copy(
                selectedAccountId = accountId,
                validationErrors = emptySet(),
                saveStatus = SaveStatus.IDLE,
            )
        }
    }

    fun onTargetAccountSelected(accountId: String) {
        mutableState.update {
            it.copy(
                selectedTargetAccountId = accountId,
                validationErrors = emptySet(),
                saveStatus = SaveStatus.IDLE,
            )
        }
    }

    fun onCategorySelected(categoryId: String) {
        mutableState.update {
            it.copy(
                selectedCategoryId = categoryId,
                validationErrors = emptySet(),
                saveStatus = SaveStatus.IDLE,
            )
        }
    }

    fun save() {
        val snapshot = mutableState.value
        if (snapshot.isSaving) return
        mutableState.update {
            it.copy(
                isSaving = true,
                amountError = null,
                validationErrors = emptySet(),
                saveStatus = SaveStatus.IDLE,
            )
        }
        viewModelScope.launch {
            try {
                when (
                    val result = createTransaction(
                        CreateTransactionCommand(
                            amountText = snapshot.amountText,
                            type = snapshot.type,
                            categoryId = snapshot.selectedCategoryId,
                            accountId = snapshot.selectedAccountId,
                            targetAccountId = snapshot.selectedTargetAccountId,
                            note = snapshot.note,
                        ),
                    )
                ) {
                    is CreateTransactionResult.InvalidAmount -> mutableState.update {
                        it.copy(isSaving = false, amountError = result.reason)
                    }

                    is CreateTransactionResult.InvalidTransaction -> mutableState.update {
                        it.copy(isSaving = false, validationErrors = result.errors)
                    }

                    is CreateTransactionResult.Success -> mutableState.update {
                        it.copy(
                            isSaving = false,
                            amountText = "",
                            note = "",
                            amountError = null,
                            validationErrors = emptySet(),
                            saveStatus = SaveStatus.SAVED,
                        )
                    }
                }
            } catch (_: Exception) {
                mutableState.update { it.copy(isSaving = false, saveStatus = SaveStatus.FAILED) }
            }
        }
    }

    private fun TransactionEditorUiState.withValidSelections(
        forceCategoryReset: Boolean = false,
    ): TransactionEditorUiState {
        val accountId = selectedAccountId.takeIf { selected -> accounts.any { it.id == selected } }
            ?: accounts.firstOrNull()?.id
        val targetId = selectedTargetAccountId.takeIf { selected -> accounts.any { it.id == selected } }
            ?: accounts.firstOrNull { it.id != accountId }?.id
        val availableCategories = visibleCategories
        val categoryId = if (forceCategoryReset) {
            availableCategories.firstOrNull()?.id
        } else {
            selectedCategoryId.takeIf { selected -> availableCategories.any { it.id == selected } }
                ?: availableCategories.firstOrNull()?.id
        }
        return copy(
            selectedAccountId = accountId,
            selectedTargetAccountId = targetId,
            selectedCategoryId = categoryId,
        )
    }

    companion object {
        private const val MAX_AMOUNT_INPUT_LENGTH = 16
        private const val MAX_NOTE_INPUT_LENGTH = 500
    }
}

class TransactionEditorViewModelFactory(
    private val createTransaction: CreateTransactionUseCase,
    private val repository: TransactionRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(TransactionEditorViewModel::class.java))
        return TransactionEditorViewModel(createTransaction, repository) as T
    }
}
