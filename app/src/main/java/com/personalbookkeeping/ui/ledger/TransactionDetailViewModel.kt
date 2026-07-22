package com.personalbookkeeping.ui.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.personalbookkeeping.domain.model.TransactionRecord
import com.personalbookkeeping.domain.repository.LedgerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class TransactionDetailUiState(
    val isLoading: Boolean = true,
    val transaction: TransactionRecord? = null,
)

class TransactionDetailViewModel(
    repository: LedgerRepository,
    transactionId: String,
) : ViewModel() {
    val state: StateFlow<TransactionDetailUiState> = repository.observeTransaction(transactionId)
        .map { TransactionDetailUiState(isLoading = false, transaction = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionDetailUiState())
}

class TransactionDetailViewModelFactory(
    private val repository: LedgerRepository,
    private val transactionId: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(TransactionDetailViewModel::class.java))
        return TransactionDetailViewModel(repository, transactionId) as T
    }
}
