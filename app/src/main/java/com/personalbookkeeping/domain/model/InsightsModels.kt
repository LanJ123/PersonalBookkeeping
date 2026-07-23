package com.personalbookkeeping.domain.model

import com.personalbookkeeping.common.Money
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

data class MonthPeriod(val year: Int, val month: Int) {
    init {
        require(month in 1..12) { "月份必须在 1 到 12 之间" }
    }

    private val value: YearMonth get() = YearMonth.of(year, month)
    val key: String get() = String.format(Locale.ROOT, "%04d-%02d", year, month)
    val label: String get() = "${year}年${month}月"
    val startEpochDay: Long get() = value.atDay(1).toEpochDay()
    val endExclusiveEpochDay: Long get() = value.plusMonths(1).atDay(1).toEpochDay()
    val endInclusiveEpochDay: Long get() = endExclusiveEpochDay - 1

    fun previous(): MonthPeriod = from(value.minusMonths(1))
    fun next(): MonthPeriod = from(value.plusMonths(1))

    companion object {
        fun from(value: YearMonth): MonthPeriod = MonthPeriod(value.year, value.monthValue)
        fun from(value: LocalDate): MonthPeriod = from(YearMonth.from(value))
    }
}

data class MonthlySummary(
    val income: Money,
    val expense: Money,
    val balance: Money,
    val transactionCount: Int,
)

data class CategorySpending(
    val categoryId: String,
    val categoryName: String,
    val amount: Money,
)

data class DailyTrend(
    val epochDay: Long,
    val income: Money,
    val expense: Money,
)

enum class BudgetStatus { NORMAL, NEAR_LIMIT, EXCEEDED }

data class BudgetProgress(
    val categoryId: String?,
    val categoryName: String?,
    val limit: Money,
    val used: Money,
    val remaining: Money,
    val status: BudgetStatus,
) {
    val progressFraction: Float
        get() = if (limit.minorUnits <= 0) 0f else
            (used.minorUnits.toDouble() / limit.minorUnits.toDouble()).coerceIn(0.0, 1.0).toFloat()
}

data class MonthlyInsights(
    val period: MonthPeriod,
    val summary: MonthlySummary,
    val categories: List<CategorySpending>,
    val dailyTrend: List<DailyTrend>,
    val recentTransactions: List<RecentTransaction>,
    val totalBudget: BudgetProgress?,
    val categoryBudgets: List<BudgetProgress>,
    val expenseCategories: List<CategoryOption>,
) {
    companion object {
        fun empty(period: MonthPeriod) = MonthlyInsights(
            period = period,
            summary = MonthlySummary(Money.fromMinor(0), Money.fromMinor(0), Money.fromMinor(0), 0),
            categories = emptyList(),
            dailyTrend = emptyList(),
            recentTransactions = emptyList(),
            totalBudget = null,
            categoryBudgets = emptyList(),
            expenseCategories = emptyList(),
        )
    }
}

object BudgetCalculator {
    fun progress(categoryId: String?, categoryName: String?, limit: Money, used: Money): BudgetProgress {
        require(limit.minorUnits > 0) { "预算必须大于零" }
        val usedMinor = used.minorUnits.coerceAtLeast(0)
        val limitMinor = limit.minorUnits
        val status = when {
            usedMinor >= limitMinor -> BudgetStatus.EXCEEDED
            usedMinor * 5 >= limitMinor * 4 -> BudgetStatus.NEAR_LIMIT
            else -> BudgetStatus.NORMAL
        }
        return BudgetProgress(
            categoryId = categoryId,
            categoryName = categoryName,
            limit = limit,
            used = Money.fromMinor(usedMinor),
            remaining = Money.fromMinor(limitMinor - usedMinor),
            status = status,
        )
    }
}
