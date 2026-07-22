package com.personalbookkeeping.data.repository

import androidx.room.withTransaction
import com.personalbookkeeping.common.Money
import com.personalbookkeeping.database.AppDatabase
import com.personalbookkeeping.database.entity.TransactionEntity
import com.personalbookkeeping.database.seed.InitialDataSeeder
import com.personalbookkeeping.domain.model.AccountOption
import com.personalbookkeeping.domain.model.CategoryKind
import com.personalbookkeeping.domain.model.CategoryOption
import com.personalbookkeeping.domain.model.EditorOptions
import com.personalbookkeeping.domain.model.NewTransaction
import com.personalbookkeeping.domain.model.RecentTransaction
import com.personalbookkeeping.domain.model.TransactionType
import com.personalbookkeeping.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant

class OfflineTransactionRepository(
    private val database: AppDatabase,
    private val initialDataSeeder: InitialDataSeeder,
) : TransactionRepository {
    override suspend fun initialize() {
        initialDataSeeder.seedIfNeeded()
    }

    override fun observeEditorOptions(): Flow<EditorOptions> = combine(
        database.optionDao().observeActiveAccounts(),
        database.optionDao().observeActiveCategories(),
    ) { accounts, categories ->
        EditorOptions(
            accounts = accounts.map { AccountOption(id = it.id, name = it.name) },
            categories = categories.map {
                CategoryOption(
                    id = it.id,
                    name = it.name,
                    kind = CategoryKind.valueOf(it.kind),
                )
            },
        )
    }

    override fun observeRecentTransactions(limit: Int): Flow<List<RecentTransaction>> =
        database.transactionDao().observeRecent(limit).map { rows ->
            rows.map { row ->
                RecentTransaction(
                    id = row.id,
                    type = TransactionType.valueOf(row.type),
                    amount = Money.fromMinor(row.amountMinor),
                    categoryName = row.categoryName,
                    accountName = row.accountName,
                    targetAccountName = row.targetAccountName,
                    occurredAt = Instant.ofEpochMilli(row.occurredAtMs),
                )
            }
        }

    override suspend fun create(transaction: NewTransaction): String {
        database.withTransaction {
            database.transactionDao().insert(transaction.toEntity())
        }
        return transaction.id
    }

    private fun NewTransaction.toEntity() = TransactionEntity(
        id = id,
        ledgerId = ledgerId,
        type = type.name,
        amountMinor = amount.minorUnits,
        categoryId = categoryId,
        accountId = accountId,
        targetAccountId = targetAccountId,
        occurredAtMs = occurredAt.toEpochMilli(),
        zoneId = zoneId.id,
        localDateEpochDay = localDateEpochDay,
        note = note,
        createdAtMs = createdAt.toEpochMilli(),
        updatedAtMs = createdAt.toEpochMilli(),
    )
}
