package com.personalbookkeeping.database.seed

import androidx.room.withTransaction
import com.personalbookkeeping.common.AppClock
import com.personalbookkeeping.database.AppDatabase
import com.personalbookkeeping.database.entity.AccountEntity
import com.personalbookkeeping.database.entity.AppPreferencesEntity
import com.personalbookkeeping.database.entity.CategoryEntity
import com.personalbookkeeping.database.entity.LedgerEntity
import com.personalbookkeeping.domain.usecase.CreateTransactionUseCase

class InitialDataSeeder(
    private val database: AppDatabase,
    private val clock: AppClock,
) {
    suspend fun seedIfNeeded() {
        database.withTransaction {
            val seedDao = database.seedDao()
            if (seedDao.ledgerCount() > 0) return@withTransaction

            val now = clock.now().toEpochMilli()
            seedDao.insertLedger(
                LedgerEntity(
                    id = CreateTransactionUseCase.DEFAULT_LEDGER_ID,
                    name = "默认账本",
                    currencyCode = "CNY",
                    monthStartDay = 1,
                    createdAtMs = now,
                    updatedAtMs = now,
                ),
            )
            seedDao.insertAccounts(defaultAccounts(now))
            seedDao.insertCategories(defaultCategories(now))
            seedDao.insertPreferences(
                AppPreferencesEntity(
                    ledgerId = CreateTransactionUseCase.DEFAULT_LEDGER_ID,
                    themeMode = "SYSTEM",
                    hideAmounts = false,
                    recentExpenseAccountId = CASH_ACCOUNT_ID,
                    recentIncomeAccountId = BANK_ACCOUNT_ID,
                    updatedAtMs = now,
                ),
            )
        }
    }

    private fun defaultAccounts(now: Long) = listOf(
        AccountEntity(
            id = CASH_ACCOUNT_ID,
            ledgerId = CreateTransactionUseCase.DEFAULT_LEDGER_ID,
            name = "现金",
            activeNameKey = "现金",
            type = "CASH",
            openingBalanceMinor = 0,
            includeInAssets = true,
            status = "ACTIVE",
            sortOrder = 0,
            createdAtMs = now,
            updatedAtMs = now,
        ),
        AccountEntity(
            id = BANK_ACCOUNT_ID,
            ledgerId = CreateTransactionUseCase.DEFAULT_LEDGER_ID,
            name = "银行卡",
            activeNameKey = "银行卡",
            type = "BANK",
            openingBalanceMinor = 0,
            includeInAssets = true,
            status = "ACTIVE",
            sortOrder = 1,
            createdAtMs = now,
            updatedAtMs = now,
        ),
    )

    private fun defaultCategories(now: Long): List<CategoryEntity> {
        val expenseNames = listOf("餐饮", "交通", "购物", "居住", "娱乐", "其他")
        val incomeNames = listOf("工资", "奖金", "退款", "其他收入")
        return expenseNames.mapIndexed { index, name ->
            category(
                id = "00000000-0000-0000-0000-${(201 + index).toString().padStart(12, '0')}",
                kind = "EXPENSE",
                name = name,
                sortOrder = index,
                now = now,
            )
        } + incomeNames.mapIndexed { index, name ->
            category(
                id = "00000000-0000-0000-0000-${(301 + index).toString().padStart(12, '0')}",
                kind = "INCOME",
                name = name,
                sortOrder = index,
                now = now,
            )
        }
    }

    private fun category(
        id: String,
        kind: String,
        name: String,
        sortOrder: Int,
        now: Long,
    ) = CategoryEntity(
        id = id,
        ledgerId = CreateTransactionUseCase.DEFAULT_LEDGER_ID,
        kind = kind,
        name = name,
        activeNameKey = name,
        iconKey = name,
        colorKey = "DEFAULT_${sortOrder % 6}",
        status = "ACTIVE",
        sortOrder = sortOrder,
        createdAtMs = now,
        updatedAtMs = now,
    )

    companion object {
        const val CASH_ACCOUNT_ID = "00000000-0000-0000-0000-000000000101"
        const val BANK_ACCOUNT_ID = "00000000-0000-0000-0000-000000000102"
    }
}
