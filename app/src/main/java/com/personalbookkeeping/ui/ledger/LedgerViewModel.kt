package com.personalbookkeeping.ui.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.personalbookkeeping.domain.model.LedgerFilterOptions
import com.personalbookkeeping.domain.model.LedgerTransaction
import com.personalbookkeeping.domain.model.TransactionFilter
import com.personalbookkeeping.domain.model.TransactionRecord
import com.personalbookkeeping.domain.model.TransactionType
import com.personalbookkeeping.domain.repository.LedgerRepository
import com.personalbookkeeping.domain.repository.TransactionRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LedgerUiState(
    val filter: TransactionFilter = TransactionFilter(),
    val options: LedgerFilterOptions = LedgerFilterOptions(emptyList(), emptyList()),
    val isInitializing: Boolean = true,
    val lastDeleted: TransactionRecord? = null,
    val message: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class LedgerViewModel(
    private val ledgerRepository: LedgerRepository,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(LedgerUiState())
    val state: StateFlow<LedgerUiState> = mutableState.asStateFlow()
    val transactions: Flow<PagingData<LedgerTransaction>> = mutableState
        .flatMapLatest { ledgerRepository.pagedTransactions(it.filter) }
        .cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            runCatching { transactionRepository.initialize() }
                .onFailure { mutableState.update { it.copy(isInitializing = false, message = "本地账本初始化失败") } }
            ledgerRepository.observeFilterOptions().collect { options ->
                mutableState.update { it.copy(options = options, isInitializing = false) }
            }
        }
    }

    fun setNoteQuery(value: String) = changeFilter { copy(noteQuery = value.take(100)) }
    fun setType(type: TransactionType?) = changeFilter { copy(type = type) }
    fun setAccount(id: String?) = changeFilter { copy(accountId = id) }
    fun setCategory(id: String?) = changeFilter { copy(categoryId = id) }

    fun setDateRange(fromText: String, toText: String): Boolean {
        val from = fromText.trim().takeIf { it.isNotEmpty() }?.let { runCatching { LocalDate.parse(it).toEpochDay() }.getOrNull() }
        val to = toText.trim().takeIf { it.isNotEmpty() }?.let { runCatching { LocalDate.parse(it).toEpochDay() }.getOrNull() }
        if ((fromText.isNotBlank() && from == null) || (toText.isNotBlank() && to == null) ||
            (from != null && to != null && from > to)
        ) {
            mutableState.update { it.copy(message = "日期格式应为 YYYY-MM-DD，且开始日期不能晚于结束日期") }
            return false
        }
        changeFilter { copy(fromEpochDay = from, toEpochDay = to) }
        return true
    }

    fun clearFilters() {
        mutableState.update { it.copy(filter = TransactionFilter()) }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            val deleted = ledgerRepository.deleteTransaction(id)
            mutableState.update {
                it.copy(lastDeleted = deleted, message = if (deleted == null) "流水不存在或已删除" else null)
            }
        }
    }

    fun restoreLastDeleted() {
        val snapshot = mutableState.value.lastDeleted ?: return
        viewModelScope.launch {
            runCatching { ledgerRepository.restoreTransaction(snapshot) }
                .onSuccess { mutableState.update { it.copy(lastDeleted = null, message = "已撤销删除") } }
                .onFailure { mutableState.update { it.copy(message = "撤销失败，请重新记账") } }
        }
    }

    fun consumeDeleted() = mutableState.update { it.copy(lastDeleted = null) }
    fun consumeMessage() = mutableState.update { it.copy(message = null) }

    private fun changeFilter(block: TransactionFilter.() -> TransactionFilter) {
        mutableState.update { it.copy(filter = it.filter.block()) }
    }
}

class LedgerViewModelFactory(
    private val ledgerRepository: LedgerRepository,
    private val transactionRepository: TransactionRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(LedgerViewModel::class.java))
        return LedgerViewModel(ledgerRepository, transactionRepository) as T
    }
}
