package com.personalbookkeeping.domain.usecase

import com.personalbookkeeping.common.AppClock
import com.personalbookkeeping.common.IdGenerator
import com.personalbookkeeping.common.MoneyParseFailure
import com.personalbookkeeping.domain.model.EditorOptions
import com.personalbookkeeping.domain.model.NewTransaction
import com.personalbookkeeping.domain.model.RecentTransaction
import com.personalbookkeeping.domain.model.TransactionType
import com.personalbookkeeping.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class CreateTransactionUseCaseTest {
    private val repository = FakeTransactionRepository()
    private val useCase = CreateTransactionUseCase(
        repository = repository,
        clock = AppClock { NOW },
        idGenerator = IdGenerator { "transaction-id" },
        zoneIdProvider = { ZONE_ID },
    )

    @Test
    fun `creates expense with exact minor units normalized note and stable local date`() = runBlocking {
        val result = useCase(
            validCommand().copy(amountText = "25.50", note = "  午饭  "),
        )

        assertEquals(CreateTransactionResult.Success("transaction-id"), result)
        val created = repository.created.single()
        assertEquals(2_550L, created.amount.minorUnits)
        assertEquals("午饭", created.note)
        assertEquals(LocalDate.of(2026, 7, 22).toEpochDay(), created.localDateEpochDay)
        assertEquals(ZONE_ID, created.zoneId)
    }

    @Test
    fun `invalid amount is rejected before repository write`() = runBlocking {
        val result = useCase(validCommand().copy(amountText = "1.001"))

        assertEquals(
            CreateTransactionResult.InvalidAmount(MoneyParseFailure.TOO_MANY_FRACTION_DIGITS),
            result,
        )
        assertTrue(repository.created.isEmpty())
    }

    @Test
    fun `transfer removes stale category and persists both account ends`() = runBlocking {
        val result = useCase(
            validCommand().copy(
                type = TransactionType.TRANSFER,
                categoryId = "stale-category",
                targetAccountId = "target-account",
            ),
        )

        assertTrue(result is CreateTransactionResult.Success)
        val created = repository.created.single()
        assertNull(created.categoryId)
        assertEquals("source-account", created.accountId)
        assertEquals("target-account", created.targetAccountId)
    }

    @Test
    fun `selected occurrence time and zone override clock defaults`() = runBlocking {
        val selectedTime = Instant.parse("2025-12-31T16:30:00Z")
        val selectedZone = ZoneId.of("Asia/Shanghai")

        useCase(
            validCommand().copy(
                occurredAt = selectedTime,
                zoneId = selectedZone,
            ),
        )

        val created = repository.created.single()
        assertEquals(selectedTime, created.occurredAt)
        assertEquals(selectedZone, created.zoneId)
        assertEquals(LocalDate.of(2026, 1, 1).toEpochDay(), created.localDateEpochDay)
    }

    private fun validCommand() = CreateTransactionCommand(
        amountText = "1.00",
        type = TransactionType.EXPENSE,
        categoryId = "category",
        accountId = "source-account",
        targetAccountId = null,
        note = "",
    )

    private class FakeTransactionRepository : TransactionRepository {
        val created = mutableListOf<NewTransaction>()

        override suspend fun initialize() = Unit

        override fun observeEditorOptions(): Flow<EditorOptions> = flowOf(
            EditorOptions(accounts = emptyList(), categories = emptyList()),
        )

        override fun observeRecentTransactions(limit: Int): Flow<List<RecentTransaction>> =
            flowOf(emptyList())

        override suspend fun create(transaction: NewTransaction): String {
            created += transaction
            return transaction.id
        }
    }

    companion object {
        private val NOW = Instant.parse("2026-07-22T05:30:00Z")
        private val ZONE_ID = ZoneId.of("Asia/Shanghai")
    }
}
