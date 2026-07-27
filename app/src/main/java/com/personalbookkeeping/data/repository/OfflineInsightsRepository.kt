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
import com.personalbookkeeping.domain.model.HomeOverview
import com.personalbookkeeping.domain.model.MonthPeriod
import com.personalbookkeeping.domain.model.MonthlyInsights
import com.personalbookkeeping.domain.model.MonthlySummary
import com.personalbookkeeping.domain.model.PeriodExpenseComparison
import com.personalbookkeeping.domain.model.RecentTransaction
import com.personalbookkeeping.domain.model.StatisticsGranularity
import com.personalbookkeeping.domain.model.StatisticsInsights
import com.personalbookkeeping.domain.model.StatisticsPeriod
import com.personalbookkeeping.domain.model.StatisticsTrendPoint
import com.personalbookkeeping.domain.model.TransactionType
import com.personalbookkeeping.domain.repository.InsightsRepository
import com.personalbookkeeping.domain.usecase.CreateTransactionUseCase
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class OfflineInsightsRepository(
    private val database: AppDatabase,
    private val clock: AppClock,
    private val idGenerator: IdGenerator,
) : InsightsRepository {
    private val ledgerId = CreateTransactionUseCase.DEFAULT_LEDGER_ID

    override fun observeHome(period: MonthPeriod, todayEpochDay: Long): Flow<HomeOverview> =
        combine(
            database.insightsDao().observeMonthlySummary(
                ledgerId,
                period.startEpochDay,
                period.endExclusiveEpochDay,
            ),
            database.insightsDao().observeMonthlySummary(
                ledgerId,
                todayEpochDay,
                todayEpochDay + 1,
            ),
            database.insightsDao().observeRecentInPeriod(
                ledgerId,
                period.startEpochDay,
                period.endExclusiveEpochDay,
                Int.MAX_VALUE,
            ),
        ) { month, today, transactionRows ->
            HomeOverview(
                period = period,
                summary = MonthlySummary(
                    income = Money.fromMinor(month.incomeMinor),
                    expense = Money.fromMinor(month.expenseMinor),
                    balance = Money.fromMinor(month.incomeMinor - month.expenseMinor),
                    transactionCount = month.transactionCount,
                ),
                todayExpense = Money.fromMinor(today.expenseMinor),
                transactions = transactionRows.map {
                    RecentTransaction(
                        id = it.id,
                        type = TransactionType.valueOf(it.type),
                        amount = Money.fromMinor(it.amountMinor),
                        categoryName = it.categoryName,
                        accountName = it.accountName,
                        targetAccountName = it.targetAccountName,
                        occurredAt = Instant.ofEpochMilli(it.occurredAtMs),
                        zoneId = ZoneId.of(it.zoneId),
                        localDateEpochDay = it.localDateEpochDay,
                    )
                },
            )
        }

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
                CategorySpending(
                    it.categoryId,
                    it.categoryName,
                    Money.fromMinor(it.amountMinor),
                    it.transactionCount,
                )
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
                        zoneId = ZoneId.of(it.zoneId),
                        localDateEpochDay = it.localDateEpochDay,
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

    override fun observeStatistics(period: StatisticsPeriod): Flow<StatisticsInsights> {
        val dao = database.insightsDao()
        val categoryComposition = combine(
            dao.observeCategoryComposition(
                ledgerId,
                TransactionType.EXPENSE.name,
                period.startEpochDay,
                period.endExclusiveEpochDay,
            ),
            dao.observeCategoryComposition(
                ledgerId,
                TransactionType.INCOME.name,
                period.startEpochDay,
                period.endExclusiveEpochDay,
            ),
        ) { expenses, incomes -> expenses to incomes }
        val currentPeriod = combine(
            dao.observeMonthlySummary(ledgerId, period.startEpochDay, period.endExclusiveEpochDay),
            categoryComposition,
            dao.observeDailyTrend(ledgerId, period.startEpochDay, period.endExclusiveEpochDay),
        ) { summary, categories, trend -> Triple(summary, categories, trend) }
        val comparisonPeriods = period.comparisonPeriods()
        val comparisonTrend = dao.observeDailyTrend(
            ledgerId,
            comparisonPeriods.first().startEpochDay,
            period.endExclusiveEpochDay,
        )

        return combine(currentPeriod, comparisonTrend) { current, comparisonDaily ->
            val (summaryRow, categoryRows, dailyRows) = current
            val (expenseCategoryRows, incomeCategoryRows) = categoryRows
            val summary = MonthlySummary(
                income = Money.fromMinor(summaryRow.incomeMinor),
                expense = Money.fromMinor(summaryRow.expenseMinor),
                balance = Money.fromMinor(summaryRow.incomeMinor - summaryRow.expenseMinor),
                transactionCount = summaryRow.transactionCount,
            )
            StatisticsInsights(
                period = period,
                summary = summary,
                averageDailyExpense = Money.fromMinor(
                    summary.expense.minorUnits / period.averageDayCount(
                        clock.now().atZone(ZoneId.systemDefault()).toLocalDate(),
                    ),
                ),
                averageDailyIncome = Money.fromMinor(
                    summary.income.minorUnits / period.averageDayCount(
                        clock.now().atZone(ZoneId.systemDefault()).toLocalDate(),
                    ),
                ),
                categories = expenseCategoryRows.map {
                    CategorySpending(
                        it.categoryId,
                        it.categoryName,
                        Money.fromMinor(it.amountMinor),
                        it.transactionCount,
                    )
                },
                incomeCategories = incomeCategoryRows.map {
                    CategorySpending(
                        it.categoryId,
                        it.categoryName,
                        Money.fromMinor(it.amountMinor),
                        it.transactionCount,
                    )
                },
                trend = buildTrend(
                    period,
                    dailyRows.associate {
                        it.localDateEpochDay to (it.incomeMinor to it.expenseMinor)
                    },
                ),
                comparisons = comparisonPeriods.map { comparison ->
                    val comparisonRows = comparisonDaily.asSequence().filter {
                        it.localDateEpochDay >= comparison.startEpochDay &&
                            it.localDateEpochDay < comparison.endExclusiveEpochDay
                    }.toList()
                    PeriodExpenseComparison(
                        label = comparison.shortLabel(),
                        expense = Money.fromMinor(
                            comparisonRows.sumOf { it.expenseMinor },
                        ),
                        income = Money.fromMinor(comparisonRows.sumOf { it.incomeMinor }),
                    )
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

    private fun buildTrend(
        period: StatisticsPeriod,
        amountsByDay: Map<Long, Pair<Long, Long>>,
    ): List<StatisticsTrendPoint> = when (period.granularity) {
        StatisticsGranularity.WEEK,
        StatisticsGranularity.MONTH,
        -> (period.startEpochDay until period.endExclusiveEpochDay).map { epochDay ->
            val date = LocalDate.ofEpochDay(epochDay)
            val amounts = amountsByDay[epochDay] ?: (0L to 0L)
            StatisticsTrendPoint(
                label = if (period.granularity == StatisticsGranularity.WEEK) {
                    "周${"一二三四五六日"[date.dayOfWeek.value - 1]}"
                } else {
                    "${date.dayOfMonth}日"
                },
                income = Money.fromMinor(amounts.first),
                expense = Money.fromMinor(amounts.second),
            )
        }

        StatisticsGranularity.YEAR -> (1..12).map { month ->
            val start = LocalDate.of(period.startDate.year, month, 1)
            val end = start.plusMonths(1)
            val matching = amountsByDay.asSequence().filter { (epochDay, _) ->
                epochDay >= start.toEpochDay() && epochDay < end.toEpochDay()
            }
            var income = 0L
            var expense = 0L
            matching.forEach { (_, amounts) ->
                income += amounts.first
                expense += amounts.second
            }
            StatisticsTrendPoint(
                label = "${month}月",
                income = Money.fromMinor(income),
                expense = Money.fromMinor(expense),
            )
        }
    }

    private fun StatisticsPeriod.shortLabel(): String = when (granularity) {
        StatisticsGranularity.WEEK -> "${startDate.monthValue}/${startDate.dayOfMonth}"
        StatisticsGranularity.MONTH -> "${startDate.monthValue}月"
        StatisticsGranularity.YEAR -> startDate.year.toString()
    }

    companion object {
        private const val TOTAL_SCOPE = "TOTAL"
    }
}
