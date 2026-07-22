package com.personalbookkeeping.database

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.personalbookkeeping.common.AppClock
import com.personalbookkeeping.database.entity.TransactionEntity
import com.personalbookkeeping.database.seed.InitialDataSeeder
import com.personalbookkeeping.domain.usecase.CreateTransactionUseCase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        database = AppDatabase.buildInMemory(ApplicationProvider.getApplicationContext())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun seedAndTransactionsProduceDerivedBalances() = runBlocking {
        InitialDataSeeder(database, AppClock { NOW }).seedIfNeeded()
        val dao = database.transactionDao()

        dao.insert(transaction("expense", "EXPENSE", 100, CASH_ID, null, EXPENSE_CATEGORY_ID))
        dao.insert(transaction("income", "INCOME", 250, BANK_ID, null, INCOME_CATEGORY_ID))
        dao.insert(transaction("transfer", "TRANSFER", 50, BANK_ID, CASH_ID, null))

        assertEquals(-50L, dao.getAccountBalanceMinor(CASH_ID))
        assertEquals(200L, dao.getAccountBalanceMinor(BANK_ID))
        assertEquals(3, dao.count())
    }

    @Test
    fun databaseTriggerRejectsInvalidTransferShape() = runBlocking {
        InitialDataSeeder(database, AppClock { NOW }).seedIfNeeded()

        val failure = runCatching {
            database.transactionDao().insert(
                transaction("invalid", "TRANSFER", 50, CASH_ID, CASH_ID, null),
            )
        }.exceptionOrNull()

        assertNotNull(failure)
        assertEquals(0, database.transactionDao().count())
    }

    private fun transaction(
        id: String,
        type: String,
        amount: Long,
        accountId: String,
        targetAccountId: String?,
        categoryId: String?,
    ) = TransactionEntity(
        id = id,
        ledgerId = CreateTransactionUseCase.DEFAULT_LEDGER_ID,
        type = type,
        amountMinor = amount,
        categoryId = categoryId,
        accountId = accountId,
        targetAccountId = targetAccountId,
        occurredAtMs = NOW.toEpochMilli(),
        zoneId = "Asia/Shanghai",
        localDateEpochDay = LocalDate.of(2026, 7, 22).toEpochDay(),
        note = null,
        createdAtMs = NOW.toEpochMilli(),
        updatedAtMs = NOW.toEpochMilli(),
    )

    companion object {
        private val NOW = Instant.parse("2026-07-22T04:00:00Z")
        private const val CASH_ID = "00000000-0000-0000-0000-000000000101"
        private const val BANK_ID = "00000000-0000-0000-0000-000000000102"
        private const val EXPENSE_CATEGORY_ID = "00000000-0000-0000-0000-000000000201"
        private const val INCOME_CATEGORY_ID = "00000000-0000-0000-0000-000000000301"
    }
}
