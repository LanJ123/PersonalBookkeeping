package com.personalbookkeeping.data.repository

import androidx.room.withTransaction
import com.personalbookkeeping.common.AppClock
import com.personalbookkeeping.common.IdGenerator
import com.personalbookkeeping.common.Money
import com.personalbookkeeping.database.AppDatabase
import com.personalbookkeeping.database.dao.BudgetRow
import com.personalbookkeeping.database.entity.BudgetEntity
import com.personalbookkeeping.domain.model.BudgetCalculator
import com.personalbookkeeping.domain.model.CategoryKind
import com.personalbookkeeping.domain.model.CategoryOption
import com.personalbookkeeping.domain.model.CategorySpending
import com.personalbookkeeping.domain.model.DailyTrend
import com.personalbookkeeping.domain.model.MonthPeriod
import com.personalbookkeeping.domain.model.MonthlyInsights
import com.personalbookkeeping.domain.model.MonthlySummary
import com.personalbookkeeping.domain.model.RecentTransaction
import com.personalbookkeeping.domain.model.TransactionType
import com.personalbookkeeping.domain.repository.InsightsRepository
import com.personalbookkeeping.domain.usecase.CreateTransactionUseCase
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class OfflineInsightsRepository(
    private val database: AppDatabase,
    private val clock: AppClock,
    private val idGenerator: IdGenerator,
) : InsightsRepository {
    private val ledgerId = CreateTransactionUseCase.DEFAULT_LEDGER_ID

    override fun observeInsights(period: MonthPeriod): Flow<MonthlyInsights> {
        val dao = database.insightsDao()
        val aggregates = combine(
            dao.observeMonthlySummary(ledgerId, period.startEpochDay, period.endExclusiveEpochDay),
            dao.observeCategorySpending(ledgerId, period.startEpochDay, period.endExclusiveEpochDay),
            dao.observeDailyTrend(ledgerId, period.startEpochDay, period.endExclusiveEpochDay),
        ) { summary, categories, trend -> Triple(summary, categories, trend) }
        val supporting = combine(
            dao.observeRecentInPeriod(ledgerId, period.startEpochDay, period.endExclusiveEpochDay, 5),
            dao.observeBudgets(ledgerId, period.key),
            database.optionDao().observeActiveCategories(),
        ) { recent, budgets, options -> Triple(recent, budgets, options) }

        return combine(aggregates, supporting) { aggregate, support ->
            val (summaryRow, categoryRows, trendRows) = aggregate
            val (recentRows, budgetRows, optionRows) = support
            val summary = MonthlySummary(
                income = Money.fromMinor(summaryRow.incomeMinor),
                expense = Money.fromMinor(summaryRow.expenseMinor),
                balance = Money.fromMinor(summaryRow.incomeMinor - summaryRow.expenseMinor),
                transactionCount = summaryRow.transactionCount,
            )
            val categorySpending = categoryRows.map {
                CategorySpending(it.categoryId, it.categoryName, Money.fromMinor(it.amountMinor))
            }
            val spendingByCategory = categorySpending.associate { it.categoryId to it.amount.minorUnits }
            MonthlyInsights(
                period = period,
                summary = summary,
                categories = categorySpending,
                dailyTrend = trendRows.map {
                    DailyTrend(it.localDateEpochDay, Money.fromMinor(it.incomeMinor), Money.fromMinor(it.expenseMinor))
                },
                recentTransactions = recentRows.map {
                    RecentTransaction(
                        id = it.id,
                        type = TransactionType.valueOf(it.type),
                        amount = Money.fromMinor(it.amountMinor),
                        categoryName = it.categoryName,
                        accountName = it.accountName,
                        targetAccountName = it.targetAccountName,
                        occurredAt = Instant.ofEpochMilli(it.occurredAtMs),
                    )
                },
                totalBudget = budgetRows.firstOrNull { it.scopeKey == TOTAL_SCOPE }
                    ?.toProgress(summary.expense.minorUnits),
                categoryBudgets = budgetRows.filter { it.categoryId != null }.map {
                    it.toProgress(spendingByCategory[it.categoryId] ?: 0L)
                },
                expenseCategories = optionRows.filter { it.kind == CategoryKind.EXPENSE.name }.map {
                    CategoryOption(it.id, it.name, CategoryKind.EXPENSE)
                },
            )
        }
    }

    override suspend fun setBudget(period: MonthPeriod, categoryId: String?, amount: Money) {
        require(amount.minorUnits > 0) { "预算必须大于零" }
        val scopeKey = scopeKey(categoryId)
        database.withTransaction {
            val dao = database.insightsDao()
            val existing = dao.getBudget(ledgerId, period.key, scopeKey)
            val now = clock.now().toEpochMilli()
            if (existing == null) {
                dao.insertBudget(
                    BudgetEntity(
                        id = idGenerator.newId(),
                        ledgerId = ledgerId,
                        periodKey = period.key,
                        scopeKey = scopeKey,
                        categoryId = categoryId,
                        amountMinor = amount.minorUnits,
                        createdAtMs = now,
                        updatedAtMs = now,
                    ),
                )
            } else {
                dao.updateBudget(existing.copy(amountMinor = amount.minorUnits, updatedAtMs = now))
            }
        }
    }

    override suspend fun clearBudget(period: MonthPeriod, categoryId: String?) {
        database.insightsDao().deleteBudget(ledgerId, period.key, scopeKey(categoryId))
    }

    private fun BudgetRow.toProgress(usedMinor: Long) = BudgetCalculator.progress(
        categoryId = categoryId,
        categoryName = categoryName,
        limit = Money.fromMinor(amountMinor),
        used = Money.fromMinor(usedMinor),
    )

    private fun scopeKey(categoryId: String?) = categoryId?.let { "CATEGORY:$it" } ?: TOTAL_SCOPE

    companion object {
        private const val TOTAL_SCOPE = "TOTAL"
    }
}
