package com.personalbookkeeping.data.repository

import androidx.room.withTransaction
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.personalbookkeeping.common.Money
import com.personalbookkeeping.database.AppDatabase
import com.personalbookkeeping.database.entity.TransactionEntity
import com.personalbookkeeping.database.seed.InitialDataSeeder
import com.personalbookkeeping.domain.model.AccountOption
import com.personalbookkeeping.domain.model.CategoryKind
import com.personalbookkeeping.domain.model.CategoryOption
import com.personalbookkeeping.domain.model.EditorOptions
import com.personalbookkeeping.domain.model.NewTransaction
import com.personalbookkeeping.domain.model.LedgerFilterOptions
import com.personalbookkeeping.domain.model.LedgerTransaction
import com.personalbookkeeping.domain.model.TransactionFilter
import com.personalbookkeeping.domain.model.TransactionRecord
import com.personalbookkeeping.domain.model.RecentTransaction
import com.personalbookkeeping.domain.model.TransactionType
import com.personalbookkeeping.domain.repository.TransactionRepository
import com.personalbookkeeping.domain.repository.LedgerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId

class OfflineTransactionRepository(
    private val database: AppDatabase,
    private val initialDataSeeder: InitialDataSeeder,
) : TransactionRepository, LedgerRepository {
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
                    zoneId = ZoneId.of(row.zoneId),
                    localDateEpochDay = row.localDateEpochDay,
                )
            }
        }

    override suspend fun create(transaction: NewTransaction): String {
        database.withTransaction {
            database.transactionDao().insert(transaction.toEntity())
        }
        return transaction.id
    }

    override fun pagedTransactions(filter: TransactionFilter): Flow<PagingData<LedgerTransaction>> =
        Pager(PagingConfig(pageSize = 30, prefetchDistance = 10, enablePlaceholders = false)) {
            database.transactionDao().pagingSource(
                ledgerId = com.personalbookkeeping.domain.usecase.CreateTransactionUseCase.DEFAULT_LEDGER_ID,
                notePattern = filter.noteQuery.trim().escapeLike(),
                type = filter.type?.name,
                accountId = filter.accountId,
                categoryId = filter.categoryId,
                fromEpochDay = filter.fromEpochDay,
                toEpochDay = filter.toEpochDay,
            )
        }.flow.map { data -> data.map { it.toDomain() } }

    override fun observeTransaction(id: String): Flow<TransactionRecord?> =
        database.transactionDao().observeById(id).map { it?.toDomain() }

    override fun observeFilterOptions(): Flow<LedgerFilterOptions> =
        observeEditorOptions().map { LedgerFilterOptions(it.accounts, it.categories) }

    override suspend fun getTransaction(id: String): TransactionRecord? =
        database.transactionDao().getById(id)?.toDomain()

    override suspend fun updateTransaction(record: TransactionRecord) {
        database.withTransaction { database.transactionDao().update(record.toEntity()) }
    }

    override suspend fun deleteTransaction(id: String): TransactionRecord? = database.withTransaction {
        val snapshot = database.transactionDao().getById(id)?.toDomain() ?: return@withTransaction null
        database.transactionDao().deleteById(id)
        snapshot
    }

    override suspend fun restoreTransaction(record: TransactionRecord) {
        database.withTransaction { database.transactionDao().insert(record.toEntity()) }
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

    private fun com.personalbookkeeping.database.dao.LedgerTransactionRow.toDomain() =
        LedgerTransaction(
            id = id,
            type = TransactionType.valueOf(type),
            amount = Money.fromMinor(amountMinor),
            categoryId = categoryId,
            categoryName = categoryName,
            accountId = accountId,
            accountName = accountName,
            targetAccountId = targetAccountId,
            targetAccountName = targetAccountName,
            occurredAt = Instant.ofEpochMilli(occurredAtMs),
            zoneId = ZoneId.of(zoneId),
            localDateEpochDay = localDateEpochDay,
            note = note,
            dailyExpense = Money.fromMinor(dailyExpenseMinor),
            dailyIncome = Money.fromMinor(dailyIncomeMinor),
        )

    private fun com.personalbookkeeping.database.dao.TransactionDetailRow.toDomain() =
        TransactionRecord(
            id = id,
            ledgerId = ledgerId,
            type = TransactionType.valueOf(type),
            amount = Money.fromMinor(amountMinor),
            categoryId = categoryId,
            categoryName = categoryName,
            accountId = accountId,
            accountName = accountName,
            targetAccountId = targetAccountId,
            targetAccountName = targetAccountName,
            occurredAt = Instant.ofEpochMilli(occurredAtMs),
            zoneId = ZoneId.of(zoneId),
            localDateEpochDay = localDateEpochDay,
            note = note,
            createdAt = Instant.ofEpochMilli(createdAtMs),
            updatedAt = Instant.ofEpochMilli(updatedAtMs),
        )

    private fun TransactionRecord.toEntity() = TransactionEntity(
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
        updatedAtMs = updatedAt.toEpochMilli(),
    )

    private fun String.escapeLike(): String = replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")
}
