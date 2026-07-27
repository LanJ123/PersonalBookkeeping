package com.personalbookkeeping.benchmark

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import androidx.room.withTransaction
import com.personalbookkeeping.database.AppDatabase
import com.personalbookkeeping.database.entity.AccountEntity
import com.personalbookkeeping.database.entity.AppPreferencesEntity
import com.personalbookkeeping.database.entity.CategoryEntity
import com.personalbookkeeping.database.entity.LedgerEntity
import com.personalbookkeeping.database.entity.TransactionEntity
import com.personalbookkeeping.domain.usecase.CreateTransactionUseCase
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class BenchmarkDataProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        BenchmarkUiSignals.configure(requireNotNull(context).applicationContext)
        return true
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle = when (method) {
        METHOD_SEED -> seed()
        METHOD_RESET_UI_SIGNAL -> Bundle().apply {
            val signal = requireNotNull(arg) { "signal name is required" }
            putInt(KEY_GENERATION, BenchmarkUiSignals.reset(signal))
            putString(KEY_SIGNAL_PATH, BenchmarkUiSignals.signalPath(signal))
            putBoolean(KEY_RESET, true)
        }
        METHOD_UI_SIGNAL_STATUS -> Bundle().apply {
            val signal = requireNotNull(arg) { "signal name is required" }
            putBoolean(KEY_MARKED, BenchmarkUiSignals.isMarked(signal))
            putInt(KEY_GENERATION, BenchmarkUiSignals.generation(signal))
        }
        else -> Bundle().apply { putString(KEY_ERROR, "unsupported method") }
    }

    private fun seed(): Bundle {
        val appContext = requireNotNull(context).applicationContext
        val startedAt = SystemClock.elapsedRealtime()
        val database = AppDatabase.build(appContext)
        val today = LocalDate.now(ZoneOffset.UTC)
        val baseInstant = today.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val transactions = List(TRANSACTION_COUNT) { index ->
            val type = when {
                index % 20 == 0 -> "TRANSFER"
                index % 5 == 0 -> "INCOME"
                else -> "EXPENSE"
            }
            val day = today.minusDays((index % 400).toLong())
            val occurredAt = baseInstant - index * 60_000L
            TransactionEntity(
                id = "benchmark-transaction-$index",
                ledgerId = LEDGER_ID,
                type = type,
                amountMinor = 100L + index % 50_000,
                categoryId = when (type) {
                    "EXPENSE" -> EXPENSE_CATEGORY_ID
                    "INCOME" -> INCOME_CATEGORY_ID
                    else -> null
                },
                accountId = CASH_ACCOUNT_ID,
                targetAccountId = if (type == "TRANSFER") BANK_ACCOUNT_ID else null,
                occurredAtMs = occurredAt,
                zoneId = "UTC",
                localDateEpochDay = day.toEpochDay(),
                note = if (index % 50 == 0) "基准备注 $index" else null,
                createdAtMs = occurredAt,
                updatedAtMs = occurredAt,
            )
        }
        runBlocking(Dispatchers.IO) {
            database.withTransaction {
                val dao = database.portabilityDao()
                dao.clearTransactions()
                dao.clearBudgets()
                dao.clearPreferences()
                dao.clearCategories()
                dao.clearAccounts()
                dao.clearLedgers()
                dao.insertLedgerForRestore(
                    LedgerEntity(
                        id = LEDGER_ID,
                        name = "性能基准账本",
                        currencyCode = "CNY",
                        monthStartDay = 1,
                        createdAtMs = baseInstant,
                        updatedAtMs = baseInstant,
                    ),
                )
                dao.insertAccountsForRestore(
                    listOf(
                        AccountEntity(
                            id = CASH_ACCOUNT_ID,
                            ledgerId = LEDGER_ID,
                            name = "基准现金",
                            activeNameKey = "基准现金",
                            type = "CASH",
                            openingBalanceMinor = 1_000_000,
                            includeInAssets = true,
                            status = "ACTIVE",
                            sortOrder = 0,
                            createdAtMs = baseInstant,
                            updatedAtMs = baseInstant,
                        ),
                        AccountEntity(
                            id = BANK_ACCOUNT_ID,
                            ledgerId = LEDGER_ID,
                            name = "基准银行卡",
                            activeNameKey = "基准银行卡",
                            type = "BANK",
                            openingBalanceMinor = 2_000_000,
                            includeInAssets = true,
                            status = "ACTIVE",
                            sortOrder = 1,
                            createdAtMs = baseInstant,
                            updatedAtMs = baseInstant,
                        ),
                    ),
                )
                dao.insertCategoriesForRestore(
                    listOf(
                        CategoryEntity(
                            id = EXPENSE_CATEGORY_ID,
                            ledgerId = LEDGER_ID,
                            kind = "EXPENSE",
                            name = "基准支出",
                            activeNameKey = "基准支出",
                            iconKey = "other",
                            colorKey = "slate",
                            status = "ACTIVE",
                            sortOrder = 0,
                            createdAtMs = baseInstant,
                            updatedAtMs = baseInstant,
                        ),
                        CategoryEntity(
                            id = INCOME_CATEGORY_ID,
                            ledgerId = LEDGER_ID,
                            kind = "INCOME",
                            name = "基准收入",
                            activeNameKey = "基准收入",
                            iconKey = "income",
                            colorKey = "green",
                            status = "ACTIVE",
                            sortOrder = 0,
                            createdAtMs = baseInstant,
                            updatedAtMs = baseInstant,
                        ),
                    ),
                )
                dao.insertTransactionsForRestore(transactions)
                dao.insertPreferencesForRestore(
                    AppPreferencesEntity(
                        ledgerId = LEDGER_ID,
                        themeMode = "SYSTEM",
                        hideAmounts = false,
                        recentExpenseAccountId = CASH_ACCOUNT_ID,
                        recentIncomeAccountId = BANK_ACCOUNT_ID,
                        updatedAtMs = baseInstant,
                    ),
                )
            }
        }
        database.close()
        return Bundle().apply {
            putInt(KEY_COUNT, TRANSACTION_COUNT)
            putLong(KEY_ELAPSED_MS, SystemClock.elapsedRealtime() - startedAt)
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    companion object {
        const val METHOD_SEED = "seed"
        const val METHOD_RESET_UI_SIGNAL = "reset-ui-signal"
        const val METHOD_UI_SIGNAL_STATUS = "ui-signal-status"
        const val KEY_GENERATION = "generation"
        const val KEY_SIGNAL_PATH = "signalPath"
        const val KEY_COUNT = "count"
        const val KEY_ELAPSED_MS = "elapsed_ms"
        const val KEY_ERROR = "error"
        const val KEY_RESET = "reset"
        const val KEY_MARKED = "marked"
        const val TRANSACTION_COUNT = 10_000
        private const val LEDGER_ID = CreateTransactionUseCase.DEFAULT_LEDGER_ID
        private const val CASH_ACCOUNT_ID = "benchmark-cash"
        private const val BANK_ACCOUNT_ID = "benchmark-bank"
        private const val EXPENSE_CATEGORY_ID = "benchmark-expense"
        private const val INCOME_CATEGORY_ID = "benchmark-income"
    }
}
