package com.personalbookkeeping.database

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.personalbookkeeping.backup.BackupArchive
import com.personalbookkeeping.backup.PortabilityService
import com.personalbookkeeping.common.AppClock
import com.personalbookkeeping.database.entity.TransactionEntity
import com.personalbookkeeping.database.seed.InitialDataSeeder
import com.personalbookkeeping.domain.model.ThemeMode
import com.personalbookkeeping.domain.usecase.CreateTransactionUseCase
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PortabilityServiceTest {
    private lateinit var database: AppDatabase
    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun setUp() {
        database = AppDatabase.buildInMemory(context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun backupClearRestoreRoundTripAndRollbackSnapshot() = runBlocking {
        seedWithTransaction()
        val service = PortabilityService(context, database, AppClock { NOW })
        val bytes = ByteArrayOutputStream().also { service.createBackup(it) }.toByteArray()

        database.transactionDao().deleteById(TRANSACTION_ID)
        assertEquals(0, database.transactionDao().count())
        val review = service.inspectBackup(ByteArrayInputStream(bytes))
        service.restore(review.token)

        assertEquals(1, database.transactionDao().count())
        val rollback = service.rollbackSnapshotFile()
        assertNotNull(rollback.takeIf { it.isFile })
        assertEquals(0, BackupArchive.read(rollback.inputStream()).data.transactions.size)
    }

    @Test
    fun injectedRestoreFailureLeavesOriginalDatabaseUnchanged() = runBlocking {
        seedWithTransaction()
        val sourceService = PortabilityService(context, database, AppClock { NOW })
        val bytes = ByteArrayOutputStream().also { sourceService.createBackup(it) }.toByteArray()
        val failingService = PortabilityService(
            context = context,
            database = database,
            clock = AppClock { NOW },
            restoreFailureHook = { error("injected restore failure") },
        )
        val review = failingService.inspectBackup(ByteArrayInputStream(bytes))

        val failure = runCatching { failingService.restore(review.token) }.exceptionOrNull()

        assertNotNull(failure)
        assertEquals(1, database.transactionDao().count())
        assertEquals(4_250L, database.transactionDao().getAccountBalanceMinor(CASH_ID))
    }

    @Test
    fun csvQueryUsesInclusiveDateRangeAndAscendingRows() = runBlocking {
        seedWithTransaction()
        val day = LocalDate.of(2026, 7, 23).toEpochDay()
        database.transactionDao().insert(
            TransactionEntity(
                id = "transaction-later",
                ledgerId = CreateTransactionUseCase.DEFAULT_LEDGER_ID,
                type = "INCOME",
                amountMinor = 500,
                categoryId = INCOME_CATEGORY_ID,
                accountId = CASH_ID,
                targetAccountId = null,
                occurredAtMs = NOW.plusSeconds(1).toEpochMilli(),
                zoneId = "Asia/Shanghai",
                localDateEpochDay = day,
                note = null,
                createdAtMs = NOW.plusSeconds(1).toEpochMilli(),
                updatedAtMs = NOW.plusSeconds(1).toEpochMilli(),
            ),
        )

        val selected = database.portabilityDao().getCsvRows(day, day + 1)
        val previous = database.portabilityDao().getCsvRows(day - 1, day)

        assertEquals(listOf(4_250L, 500L), selected.map { it.amountMinor })
        assertEquals(0, previous.size)
    }

    @Test
    fun displayPreferencesPersistAndAreObserved() = runBlocking {
        InitialDataSeeder(database, AppClock { NOW }).seedIfNeeded()
        val service = PortabilityService(context, database, AppClock { NOW })

        service.setThemeMode(ThemeMode.DARK)
        service.setHideAmounts(true)

        assertEquals(ThemeMode.DARK, service.themeMode.first())
        assertEquals(true, service.hideAmounts.first())
        assertEquals("DARK", database.portabilityDao().getPreferences()?.themeMode)
    }

    private suspend fun seedWithTransaction() {
        InitialDataSeeder(database, AppClock { NOW }).seedIfNeeded()
        database.transactionDao().insert(
            TransactionEntity(
                id = TRANSACTION_ID,
                ledgerId = CreateTransactionUseCase.DEFAULT_LEDGER_ID,
                type = "INCOME",
                amountMinor = 4_250,
                categoryId = INCOME_CATEGORY_ID,
                accountId = CASH_ID,
                targetAccountId = null,
                occurredAtMs = NOW.toEpochMilli(),
                zoneId = "Asia/Shanghai",
                localDateEpochDay = LocalDate.of(2026, 7, 23).toEpochDay(),
                note = "备份往返",
                createdAtMs = NOW.toEpochMilli(),
                updatedAtMs = NOW.toEpochMilli(),
            ),
        )
    }

    companion object {
        private val NOW = Instant.parse("2026-07-23T08:00:00Z")
        private const val CASH_ID = "00000000-0000-0000-0000-000000000101"
        private const val INCOME_CATEGORY_ID = "00000000-0000-0000-0000-000000000301"
        private const val TRANSACTION_ID = "transaction-portability-test"
    }
}
