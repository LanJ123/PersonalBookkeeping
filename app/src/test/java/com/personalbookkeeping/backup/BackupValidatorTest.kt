package com.personalbookkeeping.backup

import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupValidatorTest {
    @Test
    fun acceptsAllSupportedTransactionBudgetAndStatusShapes() {
        val base = goldenData()
        val secondAccount = base.accounts.single().copy(id = "account-bank", name = "银行卡")
        val incomeCategory = base.categories.single().copy(
            id = "category-salary",
            kind = "INCOME",
            name = "工资",
        )
        val income = base.transactions.single().copy(
            id = "transaction-income",
            type = "INCOME",
            categoryId = incomeCategory.id,
        )
        val transfer = base.transactions.single().copy(
            id = "transaction-transfer",
            type = "TRANSFER",
            categoryId = null,
            targetAccountId = secondAccount.id,
        )
        val totalBudget = base.budgets.single().copy(
            id = "budget-total",
            scopeKey = "TOTAL",
            categoryId = null,
        )
        val valid = base.copy(
            accounts = base.accounts + secondAccount,
            categories = base.categories + incomeCategory,
            transactions = listOf(base.transactions.single(), income, transfer),
            budgets = base.budgets + totalBudget,
            preferences = base.preferences.copy(
                themeMode = "DARK",
                recentExpenseAccountId = null,
                recentIncomeAccountId = secondAccount.id,
            ),
        )

        BackupValidator.validateData(valid)
        BackupValidator.validateData(
            valid.copy(
                accounts = valid.accounts.map { it.copy(status = "INACTIVE") },
                categories = valid.categories.map { it.copy(status = "INACTIVE") },
                preferences = valid.preferences.copy(themeMode = "LIGHT"),
            ),
        )
    }

    @Test
    fun rejectsInvalidTopLevelIdentityAndDuplicateShapes() {
        val base = goldenData()
        val invalid = listOf(
            base.copy(accounts = emptyList()),
            base.copy(categories = emptyList()),
            base.copy(ledger = base.ledger.copy(id = "/invalid")),
            base.copy(ledger = base.ledger.copy(name = "")),
            base.copy(ledger = base.ledger.copy(name = "x".repeat(41))),
            base.copy(ledger = base.ledger.copy(currencyCode = "USD")),
            base.copy(ledger = base.ledger.copy(monthStartDay = 2)),
            base.copy(ledger = base.ledger.copy(createdAt = "not-an-instant")),
            base.copy(ledger = base.ledger.copy(updatedAt = "not-an-instant")),
            base.copy(accounts = base.accounts + base.accounts.single()),
            base.copy(categories = base.categories + base.categories.single()),
            base.copy(transactions = base.transactions + base.transactions.single()),
            base.copy(budgets = base.budgets + base.budgets.single()),
            base.copy(
                accounts = base.accounts + base.accounts.single().copy(id = "account-two"),
            ),
            base.copy(
                categories = base.categories + base.categories.single().copy(id = "category-two"),
            ),
        )

        invalid.forEach(::assertInvalid)
        val versionError = assertThrows(BackupFormatException::class.java) {
            BackupValidator.validateData(base.copy(schemaVersion = 2))
        }
        assertEquals(BackupFormatException.Reason.UNSUPPORTED_VERSION, versionError.reason)
    }

    @Test
    fun rejectsInvalidAccountCategoryTransactionBudgetAndPreferenceFields() {
        val base = goldenData()
        val account = base.accounts.single()
        val category = base.categories.single()
        val transaction = base.transactions.single()
        val budget = base.budgets.single()
        val invalid = listOf(
            base.copy(accounts = listOf(account.copy(id = " bad"))),
            base.copy(accounts = listOf(account.copy(name = ""))),
            base.copy(accounts = listOf(account.copy(name = "x".repeat(41)))),
            base.copy(accounts = listOf(account.copy(type = "CRYPTO"))),
            base.copy(accounts = listOf(account.copy(status = "DELETED"))),
            base.copy(accounts = listOf(account.copy(openingBalanceMinor = Long.MAX_VALUE))),
            base.copy(accounts = listOf(account.copy(sortOrder = -1))),
            base.copy(accounts = listOf(account.copy(createdAt = "bad"))),
            base.copy(accounts = listOf(account.copy(updatedAt = "bad"))),
            base.copy(categories = listOf(category.copy(id = " bad"))),
            base.copy(categories = listOf(category.copy(kind = "TRANSFER"))),
            base.copy(categories = listOf(category.copy(status = "DELETED"))),
            base.copy(categories = listOf(category.copy(name = ""))),
            base.copy(categories = listOf(category.copy(iconKey = ""))),
            base.copy(categories = listOf(category.copy(colorKey = ""))),
            base.copy(categories = listOf(category.copy(sortOrder = -1))),
            base.copy(categories = listOf(category.copy(createdAt = "bad"))),
            base.copy(categories = listOf(category.copy(updatedAt = "bad"))),
            base.copy(transactions = listOf(transaction.copy(id = " bad"))),
            base.copy(transactions = listOf(transaction.copy(type = "UNKNOWN"))),
            base.copy(transactions = listOf(transaction.copy(amountMinor = 0))),
            base.copy(transactions = listOf(transaction.copy(accountId = "missing"))),
            base.copy(transactions = listOf(transaction.copy(categoryId = "missing"))),
            base.copy(transactions = listOf(transaction.copy(targetAccountId = account.id))),
            base.copy(transactions = listOf(transaction.copy(categoryId = null))),
            base.copy(transactions = listOf(transaction.copy(targetAccountId = account.id))),
            base.copy(transactions = listOf(transaction.copy(note = "x".repeat(501)))),
            base.copy(transactions = listOf(transaction.copy(occurredAt = "bad"))),
            base.copy(transactions = listOf(transaction.copy(createdAt = "bad"))),
            base.copy(transactions = listOf(transaction.copy(updatedAt = "bad"))),
            base.copy(transactions = listOf(transaction.copy(localDate = "2026-02-30"))),
            base.copy(transactions = listOf(transaction.copy(zoneId = "Mars/Base"))),
            base.copy(budgets = listOf(budget.copy(id = " bad"))),
            base.copy(budgets = listOf(budget.copy(periodKey = "2026-13"))),
            base.copy(budgets = listOf(budget.copy(amountMinor = 0))),
            base.copy(budgets = listOf(budget.copy(scopeKey = "TOTAL", categoryId = category.id))),
            base.copy(budgets = listOf(budget.copy(scopeKey = "CATEGORY:missing", categoryId = "missing"))),
            base.copy(budgets = listOf(budget.copy(scopeKey = "CATEGORY:other", categoryId = category.id))),
            base.copy(budgets = listOf(budget.copy(createdAt = "bad"))),
            base.copy(budgets = listOf(budget.copy(updatedAt = "bad"))),
            base.copy(preferences = base.preferences.copy(themeMode = "AUTO")),
            base.copy(preferences = base.preferences.copy(recentExpenseAccountId = "missing")),
            base.copy(preferences = base.preferences.copy(recentIncomeAccountId = "missing")),
            base.copy(preferences = base.preferences.copy(updatedAt = "bad")),
        )

        invalid.forEach(::assertInvalid)
    }

    @Test
    fun manifestValidationRejectsEveryContractMismatch() {
        val data = goldenData()
        val valid = BackupManifestV1(
            appVersionName = "1.0.0",
            appVersionCode = 1,
            databaseSchemaVersion = 1,
            createdAt = "2026-07-23T00:00:00Z",
            counts = data.counts(),
            dataFile = BackupDataFile(bytes = 100, sha256 = "a".repeat(64)),
        )
        BackupValidator.validateManifest(valid, data)

        val invalid = listOf(
            valid.copy(appVersionName = ""),
            valid.copy(appVersionName = "x".repeat(51)),
            valid.copy(appVersionCode = 0),
            valid.copy(databaseSchemaVersion = 0),
            valid.copy(createdAt = "bad"),
            valid.copy(counts = valid.counts.copy(accounts = valid.counts.accounts + 1)),
            valid.copy(counts = valid.counts.copy(ledgers = 2)),
            valid.copy(dataFile = valid.dataFile.copy(path = "other.json")),
            valid.copy(dataFile = valid.dataFile.copy(bytes = 1)),
            valid.copy(dataFile = valid.dataFile.copy(bytes = BackupArchive.MAX_DATA_BYTES + 1)),
            valid.copy(dataFile = valid.dataFile.copy(sha256 = "NOT-A-SHA")),
        )
        invalid.forEach { manifest ->
            val error = assertThrows(BackupFormatException::class.java) {
                BackupValidator.validateManifest(manifest, data)
            }
            assertEquals(BackupFormatException.Reason.INVALID_DATA, error.reason)
        }
    }

    private fun assertInvalid(data: BackupDataV1) {
        val error = assertThrows(BackupFormatException::class.java) {
            BackupValidator.validateData(data)
        }
        assertEquals(BackupFormatException.Reason.INVALID_DATA, error.reason)
    }

    private fun goldenData(): BackupDataV1 {
        val resource = requireNotNull(javaClass.getResourceAsStream("/fixtures/backup-v1-golden-data.json"))
        return resource.use { BackupArchive.json.decodeFromString(it.readBytes().toString(Charsets.UTF_8)) }
    }
}
