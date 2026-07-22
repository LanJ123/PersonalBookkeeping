package com.personalbookkeeping.domain.repository

import androidx.paging.PagingData
import com.personalbookkeeping.domain.model.LedgerFilterOptions
import com.personalbookkeeping.domain.model.LedgerTransaction
import com.personalbookkeeping.domain.model.TransactionFilter
import com.personalbookkeeping.domain.model.TransactionRecord
import kotlinx.coroutines.flow.Flow

interface LedgerRepository {
    fun pagedTransactions(filter: TransactionFilter): Flow<PagingData<LedgerTransaction>>
    fun observeTransaction(id: String): Flow<TransactionRecord?>
    fun observeFilterOptions(): Flow<LedgerFilterOptions>
    suspend fun getTransaction(id: String): TransactionRecord?
    suspend fun updateTransaction(record: TransactionRecord)
    suspend fun deleteTransaction(id: String): TransactionRecord?
    suspend fun restoreTransaction(record: TransactionRecord)
}
