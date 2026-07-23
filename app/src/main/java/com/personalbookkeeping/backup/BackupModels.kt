package com.personalbookkeeping.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupManifestV1(
    val format: String = FORMAT,
    val formatVersion: Int = VERSION,
    val appVersionName: String,
    val appVersionCode: Int,
    val databaseSchemaVersion: Int,
    val createdAt: String,
    val currencyCode: String = CURRENCY,
    val counts: BackupCounts,
    val dataFile: BackupDataFile,
) {
    companion object {
        const val FORMAT = "personal-bookkeeping-backup"
        const val VERSION = 1
        const val CURRENCY = "CNY"
    }
}

@Serializable
data class BackupCounts(
    val ledgers: Int = 1,
    val accounts: Int,
    val categories: Int,
    val transactions: Int,
    val budgets: Int,
)

@Serializable
data class BackupDataFile(
    val path: String = BackupArchive.DATA_ENTRY,
    val bytes: Long,
    val sha256: String,
)

@Serializable
data class BackupDataV1(
    val schemaVersion: Int = 1,
    val ledger: BackupLedger,
    val accounts: List<BackupAccount>,
    val categories: List<BackupCategory>,
    val transactions: List<BackupTransaction>,
    val budgets: List<BackupBudget>,
    val preferences: BackupPreferences,
)

@Serializable
data class BackupLedger(
    val id: String,
    val name: String,
    val currencyCode: String,
    val monthStartDay: Int,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class BackupAccount(
    val id: String,
    val name: String,
    val type: String,
    val openingBalanceMinor: Long,
    val includeInAssets: Boolean,
    val status: String,
    val sortOrder: Int,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class BackupCategory(
    val id: String,
    val kind: String,
    val name: String,
    val iconKey: String,
    val colorKey: String,
    val status: String,
    val sortOrder: Int,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class BackupTransaction(
    val id: String,
    val type: String,
    val amountMinor: Long,
    val categoryId: String?,
    val accountId: String,
    val targetAccountId: String?,
    val occurredAt: String,
    val zoneId: String,
    val localDate: String,
    val note: String?,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class BackupBudget(
    val id: String,
    val periodKey: String,
    val scopeKey: String,
    val categoryId: String?,
    val amountMinor: Long,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class BackupPreferences(
    val themeMode: String,
    val hideAmounts: Boolean,
    val recentExpenseAccountId: String?,
    val recentIncomeAccountId: String?,
    val updatedAt: String,
)

data class ValidatedBackup(
    val manifest: BackupManifestV1,
    val data: BackupDataV1,
)

data class RestoreReview(
    val token: String,
    val createdAt: String,
    val appVersionName: String,
    val counts: BackupCounts,
)

data class BackupResult(
    val bytes: Long,
    val createdAt: String,
    val counts: BackupCounts,
    val fileName: String? = null,
)

class BackupFormatException(val reason: Reason) : Exception(reason.name) {
    enum class Reason {
        UNSUPPORTED_FORMAT,
        UNSUPPORTED_VERSION,
        CORRUPT_ARCHIVE,
        INTEGRITY_MISMATCH,
        INVALID_DATA,
        TOO_LARGE,
    }
}
