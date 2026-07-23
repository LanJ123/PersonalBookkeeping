package com.personalbookkeeping.backup

import com.personalbookkeeping.common.NameNormalizer
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

object BackupValidator {
    private val idPattern = Regex("^[A-Za-z0-9][A-Za-z0-9._:-]{0,99}$")
    private val periodPattern = Regex("^[0-9]{4}-(0[1-9]|1[0-2])$")
    private val categoryScopePattern = Regex("^CATEGORY:[A-Za-z0-9][A-Za-z0-9._:-]{0,99}$")
    private val accountTypes = setOf("CASH", "BANK", "E_WALLET", "STORED_VALUE", "CREDIT_CARD", "OTHER")
    private val statuses = setOf("ACTIVE", "INACTIVE")
    private val categoryKinds = setOf("EXPENSE", "INCOME")
    private val transactionTypes = setOf("EXPENSE", "INCOME", "TRANSFER")
    private val themeModes = setOf("SYSTEM", "LIGHT", "DARK")
    private val amountRange = 1L..99_999_999_999L
    private val signedAmountRange = -99_999_999_999L..99_999_999_999L

    fun validateManifest(manifest: BackupManifestV1, data: BackupDataV1) {
        requireValid(manifest.appVersionName.isNotBlank() && manifest.appVersionName.length <= 50)
        requireValid(manifest.appVersionCode >= 1 && manifest.databaseSchemaVersion >= 1)
        parseInstant(manifest.createdAt)
        requireValid(manifest.counts == data.counts())
        requireValid(manifest.counts.ledgers == 1)
        requireValid(manifest.dataFile.path == BackupArchive.DATA_ENTRY)
        requireValid(manifest.dataFile.bytes in 2..BackupArchive.MAX_DATA_BYTES)
        requireValid(Regex("^[a-f0-9]{64}$").matches(manifest.dataFile.sha256))
    }

    fun validateData(data: BackupDataV1) {
        requireVersion(data.schemaVersion == 1)
        requireValid(data.accounts.isNotEmpty() && data.accounts.size <= 10_000)
        requireValid(data.categories.isNotEmpty() && data.categories.size <= 10_000)
        requireValid(data.transactions.size <= 1_000_000 && data.budgets.size <= 100_000)
        validateId(data.ledger.id)
        requireValid(data.ledger.name.isNotBlank() && data.ledger.name.length <= 40)
        requireValid(data.ledger.currencyCode == "CNY" && data.ledger.monthStartDay == 1)
        parseInstant(data.ledger.createdAt)
        parseInstant(data.ledger.updatedAt)

        requireUnique(data.accounts.map { it.id })
        requireUnique(data.categories.map { it.id })
        requireUnique(data.transactions.map { it.id })
        requireUnique(data.budgets.map { it.id })
        val accountIds = data.accounts.mapTo(mutableSetOf()) { it.id }
        val categories = data.categories.associateBy { it.id }

        data.accounts.forEach { account ->
            validateId(account.id)
            requireValid(account.name.isNotBlank() && account.name.length <= 40)
            requireValid(account.type in accountTypes && account.status in statuses)
            requireValid(account.openingBalanceMinor in signedAmountRange && account.sortOrder >= 0)
            parseInstant(account.createdAt)
            parseInstant(account.updatedAt)
        }
        val activeAccountNames = data.accounts.filter { it.status == "ACTIVE" }
            .map { NameNormalizer.activeKey(it.name) }
        requireValid(activeAccountNames.size == activeAccountNames.toSet().size)
        data.categories.forEach { category ->
            validateId(category.id)
            requireValid(category.kind in categoryKinds && category.status in statuses)
            requireValid(category.name.isNotBlank() && category.name.length <= 40)
            requireValid(category.iconKey.isNotBlank() && category.iconKey.length <= 40)
            requireValid(category.colorKey.isNotBlank() && category.colorKey.length <= 40)
            requireValid(category.sortOrder >= 0)
            parseInstant(category.createdAt)
            parseInstant(category.updatedAt)
        }
        val activeCategoryNames = data.categories.filter { it.status == "ACTIVE" }
            .map { it.kind to NameNormalizer.activeKey(it.name) }
        requireValid(activeCategoryNames.size == activeCategoryNames.toSet().size)
        data.transactions.forEach { transaction ->
            validateId(transaction.id)
            requireValid(transaction.type in transactionTypes && transaction.amountMinor in amountRange)
            requireValid(transaction.accountId in accountIds)
            transaction.categoryId?.let { requireValid(it in categories) }
            transaction.targetAccountId?.let { requireValid(it in accountIds) }
            when (transaction.type) {
                "EXPENSE", "INCOME" -> {
                    requireValid(transaction.categoryId != null && transaction.targetAccountId == null)
                    requireValid(categories[transaction.categoryId]?.kind == transaction.type)
                }
                "TRANSFER" -> requireValid(
                    transaction.categoryId == null &&
                        transaction.targetAccountId != null &&
                        transaction.targetAccountId != transaction.accountId,
                )
            }
            requireValid(transaction.note == null || transaction.note.length <= 500)
            parseInstant(transaction.occurredAt)
            parseInstant(transaction.createdAt)
            parseInstant(transaction.updatedAt)
            parseDate(transaction.localDate)
            try {
                ZoneId.of(transaction.zoneId)
            } catch (_: Exception) {
                invalid()
            }
        }
        data.budgets.forEach { budget ->
            validateId(budget.id)
            requireValid(periodPattern.matches(budget.periodKey) && budget.amountMinor in amountRange)
            try {
                YearMonth.parse(budget.periodKey)
            } catch (_: Exception) {
                invalid()
            }
            if (budget.scopeKey == "TOTAL") {
                requireValid(budget.categoryId == null)
            } else {
                requireValid(categoryScopePattern.matches(budget.scopeKey))
                requireValid(budget.categoryId != null && budget.categoryId in categories)
                requireValid(budget.scopeKey == "CATEGORY:${budget.categoryId}")
                requireValid(categories[budget.categoryId]?.kind == "EXPENSE")
            }
            parseInstant(budget.createdAt)
            parseInstant(budget.updatedAt)
        }
        requireValid(data.preferences.themeMode in themeModes)
        data.preferences.recentExpenseAccountId?.let { requireValid(it in accountIds) }
        data.preferences.recentIncomeAccountId?.let { requireValid(it in accountIds) }
        parseInstant(data.preferences.updatedAt)
    }

    private fun validateId(id: String) = requireValid(idPattern.matches(id))

    private fun requireUnique(ids: List<String>) = requireValid(ids.size == ids.toSet().size)

    private fun parseInstant(value: String) {
        try {
            Instant.parse(value)
        } catch (_: Exception) {
            invalid()
        }
    }

    private fun parseDate(value: String) {
        try {
            LocalDate.parse(value)
        } catch (_: Exception) {
            invalid()
        }
    }

    private fun requireVersion(condition: Boolean) {
        if (!condition) throw BackupFormatException(BackupFormatException.Reason.UNSUPPORTED_VERSION)
    }

    private fun requireValid(condition: Boolean) {
        if (!condition) invalid()
    }

    private fun invalid(): Nothing = throw BackupFormatException(BackupFormatException.Reason.INVALID_DATA)
}
