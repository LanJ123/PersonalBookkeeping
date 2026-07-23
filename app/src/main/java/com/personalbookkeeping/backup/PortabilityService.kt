package com.personalbookkeeping.backup

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.room.withTransaction
import com.personalbookkeeping.BuildConfig
import com.personalbookkeeping.common.AppClock
import com.personalbookkeeping.common.NameNormalizer
import com.personalbookkeeping.database.AppDatabase
import com.personalbookkeeping.database.dao.PortableSnapshotRows
import com.personalbookkeeping.database.entity.AccountEntity
import com.personalbookkeeping.database.entity.AppPreferencesEntity
import com.personalbookkeeping.database.entity.BudgetEntity
import com.personalbookkeeping.database.entity.CategoryEntity
import com.personalbookkeeping.database.entity.LedgerEntity
import com.personalbookkeeping.database.entity.TransactionEntity
import com.personalbookkeeping.domain.model.AccountType
import com.personalbookkeeping.domain.usecase.CreateTransactionUseCase
import com.personalbookkeeping.export.CsvExportResult
import com.personalbookkeeping.export.CsvExporter
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PortabilityService(
    context: Context,
    private val database: AppDatabase,
    private val clock: AppClock,
    private val restoreFailureHook: (() -> Unit)? = null,
) {
    private val appContext = context.applicationContext
    private val dao get() = database.portabilityDao()
    private val mutationMutex = Mutex()
    private val pendingMutex = Any()
    private var pendingRestore: Pair<String, ValidatedBackup>? = null

    val hideAmounts: Flow<Boolean> = dao.observePreferences().filterNotNull().map { it.hideAmounts }

    suspend fun setHideAmounts(hidden: Boolean) {
        mutationMutex.withLock {
            check(dao.setHideAmounts(hidden, clock.now().toEpochMilli()) == 1)
        }
    }

    suspend fun createBackup(uri: Uri): BackupResult = mutationMutex.withLock {
        val output = requireNotNull(appContext.contentResolver.openOutputStream(uri, "w"))
        output.use { createBackupUnlocked(it) }.copy(fileName = displayName(uri))
    }

    suspend fun createBackup(output: OutputStream): BackupResult = mutationMutex.withLock {
        createBackupUnlocked(output)
    }

    suspend fun inspectBackup(uri: Uri): RestoreReview {
        val input = requireNotNull(appContext.contentResolver.openInputStream(uri))
        return input.use { inspectBackup(it) }
    }

    fun inspectBackup(input: InputStream): RestoreReview {
        val backup = BackupArchive.read(input)
        val token = UUID.randomUUID().toString()
        synchronized(pendingMutex) { pendingRestore = token to backup }
        return RestoreReview(
            token = token,
            createdAt = backup.manifest.createdAt,
            appVersionName = backup.manifest.appVersionName,
            counts = backup.manifest.counts,
        )
    }

    suspend fun restore(token: String) = mutationMutex.withLock {
        val backup = synchronized(pendingMutex) {
            pendingRestore?.takeIf { it.first == token }?.second
        } ?: throw BackupFormatException(BackupFormatException.Reason.INVALID_DATA)

        val rollbackData = database.withTransaction { dao.snapshot().toBackupData() }
        val rollbackFile = File(appContext.noBackupFilesDir, ROLLBACK_FILE_NAME)
        FileOutputStream(rollbackFile, false).use { output ->
            BackupArchive.write(
                data = rollbackData,
                appVersionName = BuildConfig.VERSION_NAME,
                appVersionCode = BuildConfig.VERSION_CODE,
                createdAt = clock.now().toString(),
                output = output,
            )
        }
        database.withTransaction {
            dao.clearTransactions()
            dao.clearBudgets()
            dao.clearPreferences()
            dao.clearCategories()
            dao.clearAccounts()
            dao.clearLedgers()
            restoreFailureHook?.invoke()
            val entities = backup.data.toEntities()
            dao.insertLedgerForRestore(entities.ledger)
            dao.insertAccountsForRestore(entities.accounts)
            dao.insertCategoriesForRestore(entities.categories)
            dao.insertTransactionsForRestore(entities.transactions)
            dao.insertBudgetsForRestore(entities.budgets)
            dao.insertPreferencesForRestore(entities.preferences)
        }
        synchronized(pendingMutex) { pendingRestore = null }
    }

    fun discardPendingRestore(token: String) {
        synchronized(pendingMutex) {
            if (pendingRestore?.first == token) pendingRestore = null
        }
    }

    suspend fun exportCsv(uri: Uri, startDate: LocalDate, endDateInclusive: LocalDate): CsvExportResult {
        require(!endDateInclusive.isBefore(startDate))
        val rows = database.withTransaction {
            dao.getCsvRows(startDate.toEpochDay(), endDateInclusive.plusDays(1).toEpochDay())
        }
        val output = requireNotNull(appContext.contentResolver.openOutputStream(uri, "w"))
        return output.use { CsvExporter.write(rows, it) }
    }

    fun rollbackSnapshotFile(): File = File(appContext.noBackupFilesDir, ROLLBACK_FILE_NAME)

    private suspend fun createBackupUnlocked(output: OutputStream): BackupResult {
        val data = database.withTransaction { dao.snapshot().toBackupData() }
        return BackupArchive.write(
            data = data,
            appVersionName = BuildConfig.VERSION_NAME,
            appVersionCode = BuildConfig.VERSION_CODE,
            createdAt = clock.now().toString(),
            output = output,
        )
    }

    private fun displayName(uri: Uri): String? = appContext.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null,
    )?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }

    private data class RestoreEntities(
        val ledger: LedgerEntity,
        val accounts: List<AccountEntity>,
        val categories: List<CategoryEntity>,
        val transactions: List<TransactionEntity>,
        val budgets: List<BudgetEntity>,
        val preferences: AppPreferencesEntity,
    )

    private fun PortableSnapshotRows.toBackupData(): BackupDataV1 = BackupDataV1(
        ledger = BackupLedger(
            id = ledger.id,
            name = ledger.name,
            currencyCode = ledger.currencyCode,
            monthStartDay = ledger.monthStartDay,
            createdAt = ledger.createdAtMs.isoInstant(),
            updatedAt = ledger.updatedAtMs.isoInstant(),
        ),
        accounts = accounts.map {
            BackupAccount(
                id = it.id,
                name = it.name,
                type = AccountType.fromStoredValue(it.type).name,
                openingBalanceMinor = it.openingBalanceMinor,
                includeInAssets = it.includeInAssets,
                status = it.status,
                sortOrder = it.sortOrder,
                createdAt = it.createdAtMs.isoInstant(),
                updatedAt = it.updatedAtMs.isoInstant(),
            )
        },
        categories = categories.map {
            BackupCategory(
                id = it.id,
                kind = it.kind,
                name = it.name,
                iconKey = it.iconKey,
                colorKey = it.colorKey,
                status = it.status,
                sortOrder = it.sortOrder,
                createdAt = it.createdAtMs.isoInstant(),
                updatedAt = it.updatedAtMs.isoInstant(),
            )
        },
        transactions = transactions.map {
            BackupTransaction(
                id = it.id,
                type = it.type,
                amountMinor = it.amountMinor,
                categoryId = it.categoryId,
                accountId = it.accountId,
                targetAccountId = it.targetAccountId,
                occurredAt = it.occurredAtMs.isoInstant(),
                zoneId = it.zoneId,
                localDate = LocalDate.ofEpochDay(it.localDateEpochDay).toString(),
                note = it.note,
                createdAt = it.createdAtMs.isoInstant(),
                updatedAt = it.updatedAtMs.isoInstant(),
            )
        },
        budgets = budgets.map {
            BackupBudget(
                id = it.id,
                periodKey = it.periodKey,
                scopeKey = it.scopeKey,
                categoryId = it.categoryId,
                amountMinor = it.amountMinor,
                createdAt = it.createdAtMs.isoInstant(),
                updatedAt = it.updatedAtMs.isoInstant(),
            )
        },
        preferences = BackupPreferences(
            themeMode = preferences.themeMode,
            hideAmounts = preferences.hideAmounts,
            recentExpenseAccountId = preferences.recentExpenseAccountId,
            recentIncomeAccountId = preferences.recentIncomeAccountId,
            updatedAt = preferences.updatedAtMs.isoInstant(),
        ),
    )

    private fun BackupDataV1.toEntities(): RestoreEntities {
        val ledgerId = CreateTransactionUseCase.DEFAULT_LEDGER_ID
        return RestoreEntities(
            ledger = LedgerEntity(
                id = ledgerId,
                name = ledger.name,
                currencyCode = ledger.currencyCode,
                monthStartDay = ledger.monthStartDay,
                createdAtMs = ledger.createdAt.epochMillis(),
                updatedAtMs = ledger.updatedAt.epochMillis(),
            ),
            accounts = accounts.map {
                AccountEntity(
                    id = it.id,
                    ledgerId = ledgerId,
                    name = it.name,
                    activeNameKey = if (it.status == "ACTIVE") NameNormalizer.activeKey(it.name) else null,
                    type = it.type,
                    openingBalanceMinor = it.openingBalanceMinor,
                    includeInAssets = it.includeInAssets,
                    status = it.status,
                    sortOrder = it.sortOrder,
                    createdAtMs = it.createdAt.epochMillis(),
                    updatedAtMs = it.updatedAt.epochMillis(),
                )
            },
            categories = categories.map {
                CategoryEntity(
                    id = it.id,
                    ledgerId = ledgerId,
                    kind = it.kind,
                    name = it.name,
                    activeNameKey = if (it.status == "ACTIVE") NameNormalizer.activeKey(it.name) else null,
                    iconKey = it.iconKey,
                    colorKey = it.colorKey,
                    status = it.status,
                    sortOrder = it.sortOrder,
                    createdAtMs = it.createdAt.epochMillis(),
                    updatedAtMs = it.updatedAt.epochMillis(),
                )
            },
            transactions = transactions.map {
                TransactionEntity(
                    id = it.id,
                    ledgerId = ledgerId,
                    type = it.type,
                    amountMinor = it.amountMinor,
                    categoryId = it.categoryId,
                    accountId = it.accountId,
                    targetAccountId = it.targetAccountId,
                    occurredAtMs = it.occurredAt.epochMillis(),
                    zoneId = it.zoneId,
                    localDateEpochDay = LocalDate.parse(it.localDate).toEpochDay(),
                    note = it.note,
                    createdAtMs = it.createdAt.epochMillis(),
                    updatedAtMs = it.updatedAt.epochMillis(),
                )
            },
            budgets = budgets.map {
                BudgetEntity(
                    id = it.id,
                    ledgerId = ledgerId,
                    periodKey = it.periodKey,
                    scopeKey = it.scopeKey,
                    categoryId = it.categoryId,
                    amountMinor = it.amountMinor,
                    createdAtMs = it.createdAt.epochMillis(),
                    updatedAtMs = it.updatedAt.epochMillis(),
                )
            },
            preferences = AppPreferencesEntity(
                ledgerId = ledgerId,
                themeMode = preferences.themeMode,
                hideAmounts = preferences.hideAmounts,
                recentExpenseAccountId = preferences.recentExpenseAccountId,
                recentIncomeAccountId = preferences.recentIncomeAccountId,
                updatedAtMs = preferences.updatedAt.epochMillis(),
            ),
        )
    }

    private fun Long.isoInstant(): String = Instant.ofEpochMilli(this).toString()
    private fun String.epochMillis(): Long = Instant.parse(this).toEpochMilli()

    companion object {
        const val ROLLBACK_FILE_NAME = "pre-restore.pbk"
    }
}
