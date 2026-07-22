package com.personalbookkeeping.domain.repository

import com.personalbookkeeping.domain.model.EditorOptions
import com.personalbookkeeping.domain.model.NewTransaction
import com.personalbookkeeping.domain.model.RecentTransaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    suspend fun initialize()

    fun observeEditorOptions(): Flow<EditorOptions>

    fun observeRecentTransactions(limit: Int = 20): Flow<List<RecentTransaction>>

    suspend fun create(transaction: NewTransaction): String
}
