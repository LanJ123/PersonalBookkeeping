package com.personalbookkeeping.domain.repository

import com.personalbookkeeping.domain.model.AccountType
import com.personalbookkeeping.domain.model.CategoryKind
import com.personalbookkeeping.domain.model.ManagedAccount
import com.personalbookkeeping.domain.model.ManagedCategory
import com.personalbookkeeping.domain.model.ManagementResult
import com.personalbookkeeping.domain.model.MoveDirection
import kotlinx.coroutines.flow.Flow

interface ManagementRepository {
    fun observeAccounts(): Flow<List<ManagedAccount>>
    fun observeCategories(kind: CategoryKind): Flow<List<ManagedCategory>>

    suspend fun saveAccount(
        id: String?,
        name: String,
        type: AccountType,
        openingBalanceText: String,
        includeInAssets: Boolean,
    ): ManagementResult

    suspend fun deactivateAccount(id: String): ManagementResult
    suspend fun moveAccount(id: String, direction: MoveDirection): ManagementResult

    suspend fun saveCategory(id: String?, kind: CategoryKind, name: String): ManagementResult
    suspend fun deactivateCategory(id: String): ManagementResult
    suspend fun moveCategory(id: String, direction: MoveDirection): ManagementResult
}
