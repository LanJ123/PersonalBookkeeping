package com.personalbookkeeping.database

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.personalbookkeeping.common.AppClock
import com.personalbookkeeping.common.IdGenerator
import com.personalbookkeeping.data.repository.OfflineManagementRepository
import com.personalbookkeeping.data.repository.OfflineTransactionRepository
import com.personalbookkeeping.database.entity.TransactionEntity
import com.personalbookkeeping.database.seed.InitialDataSeeder
import com.personalbookkeeping.domain.usecase.CreateTransactionUseCase
import com.personalbookkeeping.domain.model.AccountType
import com.personalbookkeeping.domain.model.CategoryKind
import com.personalbookkeeping.domain.model.ManagementResult
import com.personalbookkeeping.domain.model.MoveDirection
import com.personalbookkeeping.domain.model.TransactionType
import androidx.paging.PagingSource
import kotlinx.coroutines.flow.first
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

    @Test
    fun pagingCombinesFiltersEscapesWildcardsAndMatchesTransferBothSides() = runBlocking {
        InitialDataSeeder(database, AppClock { NOW }).seedIfNeeded()
        val dao = database.transactionDao()
        dao.insert(transaction("literal", "EXPENSE", 100, CASH_ID, null, EXPENSE_CATEGORY_ID).copy(note = "100%_完成"))
        dao.insert(transaction("other", "EXPENSE", 200, BANK_ID, null, EXPENSE_CATEGORY_ID).copy(note = "普通记录"))
        dao.insert(transaction("transfer", "TRANSFER", 50, BANK_ID, CASH_ID, null).copy(note = "转入现金"))

        val literalLoaded = dao.pagingSource(
            CreateTransactionUseCase.DEFAULT_LEDGER_ID,
            "\\%\\_",
            null,
            null,
            null,
            null,
            null,
        ).load(PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false))
        val literalPage = literalLoaded as PagingSource.LoadResult.Page<Int, com.personalbookkeeping.database.dao.LedgerTransactionRow>
        assertEquals(listOf("literal"), literalPage.data.map { it.id })
        val loaded = dao.pagingSource(
            CreateTransactionUseCase.DEFAULT_LEDGER_ID,
            "转入",
            TransactionType.TRANSFER.name,
            CASH_ID,
            null,
            null,
            null,
        ).load(PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false))
        val page = loaded as PagingSource.LoadResult.Page<Int, com.personalbookkeeping.database.dao.LedgerTransactionRow>
        assertEquals(listOf("transfer"), page.data.map { it.id })
        assertEquals(0L, page.data.single().dailyExpenseMinor)
    }

    @Test
    fun accountAndCategoryManagementEnforcesNamesLastActiveAndSorting() = runBlocking {
        InitialDataSeeder(database, AppClock { NOW }).seedIfNeeded()
        var idSequence = 0
        val repository = OfflineManagementRepository(
            database,
            AppClock { NOW },
            IdGenerator { "generated-${++idSequence}" },
        )

        val duplicate = repository.saveAccount(null, " 现 金 ", AccountType.CASH, "0", true)
        assertEquals(ManagementResult.DuplicateName, duplicate)
        val added = repository.saveAccount(null, "零钱", AccountType.CASH, "-10.50", true)
        assertEquals("generated-1", (added as ManagementResult.Success).id)
        repository.moveAccount("generated-1", MoveDirection.UP)
        repository.moveAccount("generated-1", MoveDirection.UP)
        assertEquals("generated-1", repository.observeAccounts().first().first { it.status.name == "ACTIVE" }.id)

        val categories = repository.observeCategories(CategoryKind.INCOME).first()
        categories.dropLast(1).forEach { repository.deactivateCategory(it.id) }
        assertEquals(
            ManagementResult.LastActiveItem,
            repository.deactivateCategory(categories.last().id),
        )
    }

    @Test
    fun editDeleteAndRestoreReplaceBalanceEffectExactlyOnce() = runBlocking {
        val seeder = InitialDataSeeder(database, AppClock { NOW })
        seeder.seedIfNeeded()
        val dao = database.transactionDao()
        dao.insert(transaction("editable", "EXPENSE", 100, CASH_ID, null, EXPENSE_CATEGORY_ID))
        val repository = OfflineTransactionRepository(database, seeder)
        val before = requireNotNull(repository.getTransaction("editable"))

        repository.updateTransaction(
            before.copy(
                type = TransactionType.INCOME,
                amount = com.personalbookkeeping.common.Money.fromMinor(250),
                categoryId = INCOME_CATEGORY_ID,
                categoryName = "工资",
            ),
        )
        assertEquals(250L, dao.getAccountBalanceMinor(CASH_ID))
        val snapshot = requireNotNull(repository.deleteTransaction("editable"))
        assertEquals(0L, dao.getAccountBalanceMinor(CASH_ID))
        repository.restoreTransaction(snapshot)
        assertEquals(250L, dao.getAccountBalanceMinor(CASH_ID))
        assertEquals(1, dao.count())
    }

    @Test
    fun deactivatedItemsLeaveNewOptionsButRemainVisibleInHistory() = runBlocking {
        val seeder = InitialDataSeeder(database, AppClock { NOW })
        seeder.seedIfNeeded()
        database.transactionDao().insert(transaction("historical", "EXPENSE", 100, CASH_ID, null, EXPENSE_CATEGORY_ID))
        val management = OfflineManagementRepository(database, AppClock { NOW }, IdGenerator { "unused" })
        assertEquals(ManagementResult.Success(CASH_ID), management.deactivateAccount(CASH_ID))
        val transactions = OfflineTransactionRepository(database, seeder)

        assertEquals(false, transactions.observeEditorOptions().first().accounts.any { it.id == CASH_ID })
        assertEquals("现金", transactions.getTransaction("historical")?.accountName)
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
