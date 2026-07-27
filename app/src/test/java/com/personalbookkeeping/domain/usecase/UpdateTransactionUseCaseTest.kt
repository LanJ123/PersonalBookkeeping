package com.personalbookkeeping.domain.usecase

import androidx.paging.PagingData
import com.personalbookkeeping.common.AppClock
import com.personalbookkeeping.common.Money
import com.personalbookkeeping.domain.model.LedgerFilterOptions
import com.personalbookkeeping.domain.model.LedgerTransaction
import com.personalbookkeeping.domain.model.TransactionFilter
import com.personalbookkeeping.domain.model.TransactionRecord
import com.personalbookkeeping.domain.model.TransactionType
import com.personalbookkeeping.domain.repository.LedgerRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateTransactionUseCaseTest {
    @Test
    fun `updates occurrence time zone and local date together`() = runBlocking {
        val repository = FakeLedgerRepository(record())
        val useCase = UpdateTransactionUseCase(repository, AppClock { UPDATED_AT })

        val result = useCase(
            UpdateTransactionCommand(
                id = RECORD_ID,
                amountText = "25.50",
                type = TransactionType.EXPENSE,
                categoryId = "food",
                accountId = "cash",
                targetAccountId = null,
                note = "晚餐",
                occurredAt = SELECTED_TIME,
                zoneId = SELECTED_ZONE,
            ),
        )

        assertEquals(UpdateTransactionResult.Success, result)
        val updated = requireNotNull(repository.updated)
        assertEquals(SELECTED_TIME, updated.occurredAt)
        assertEquals(SELECTED_ZONE, updated.zoneId)
        assertEquals(LocalDate.of(2026, 1, 1).toEpochDay(), updated.localDateEpochDay)
        assertEquals(UPDATED_AT, updated.updatedAt)
    }

    private fun record() = TransactionRecord(
        id = RECORD_ID,
        ledgerId = CreateTransactionUseCase.DEFAULT_LEDGER_ID,
        type = TransactionType.EXPENSE,
        amount = Money.fromMinor(1_000),
        categoryId = "food",
        categoryName = "餐饮",
        accountId = "cash",
        accountName = "现金",
        targetAccountId = null,
        targetAccountName = null,
        occurredAt = Instant.parse("2026-07-26T10:00:00Z"),
        zoneId = ZoneId.of("Asia/Shanghai"),
        localDateEpochDay = LocalDate.of(2026, 7, 26).toEpochDay(),
        note = null,
        createdAt = Instant.parse("2026-07-26T10:00:00Z"),
        updatedAt = Instant.parse("2026-07-26T10:00:00Z"),
    )

    private class FakeLedgerRepository(
        private val current: TransactionRecord,
    ) : LedgerRepository {
        var updated: TransactionRecord? = null

        override fun pagedTransactions(filter: TransactionFilter): Flow<PagingData<LedgerTransaction>> =
            flowOf(PagingData.empty())

        override fun observeTransaction(id: String): Flow<TransactionRecord?> = flowOf(current)

        override fun observeFilterOptions(): Flow<LedgerFilterOptions> =
            flowOf(LedgerFilterOptions(emptyList(), emptyList()))

        override suspend fun getTransaction(id: String): TransactionRecord? =
            current.takeIf { it.id == id }

        override suspend fun updateTransaction(record: TransactionRecord) {
            updated = record
        }

        override suspend fun deleteTransaction(id: String): TransactionRecord? = null

        override suspend fun restoreTransaction(record: TransactionRecord) = Unit
    }

    companion object {
        private const val RECORD_ID = "transaction-id"
        private val UPDATED_AT = Instant.parse("2026-07-26T12:00:00Z")
        private val SELECTED_TIME = Instant.parse("2025-12-31T16:30:00Z")
        private val SELECTED_ZONE = ZoneId.of("Asia/Shanghai")
    }
}
