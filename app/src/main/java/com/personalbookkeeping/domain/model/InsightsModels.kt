package com.personalbookkeeping.domain.model

import com.personalbookkeeping.common.Money
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
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

data class HomeOverview(
    val period: MonthPeriod,
    val summary: MonthlySummary,
    val todayExpense: Money,
    val transactions: List<RecentTransaction> = emptyList(),
)

data class CategorySpending(
    val categoryId: String,
    val categoryName: String,
    val amount: Money,
    val transactionCount: Int = 0,
)

data class DailyTrend(
    val epochDay: Long,
    val income: Money,
    val expense: Money,
)

enum class StatisticsGranularity {
    WEEK,
    MONTH,
    YEAR,
}

data class StatisticsPeriod(
    val granularity: StatisticsGranularity,
    val anchorEpochDay: Long,
) {
    private val anchor: LocalDate get() = LocalDate.ofEpochDay(anchorEpochDay)

    val startDate: LocalDate
        get() = when (granularity) {
            StatisticsGranularity.WEEK ->
                anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            StatisticsGranularity.MONTH -> anchor.withDayOfMonth(1)
            StatisticsGranularity.YEAR -> anchor.withDayOfYear(1)
        }

    val endExclusiveDate: LocalDate
        get() = when (granularity) {
            StatisticsGranularity.WEEK -> startDate.plusWeeks(1)
            StatisticsGranularity.MONTH -> startDate.plusMonths(1)
            StatisticsGranularity.YEAR -> startDate.plusYears(1)
        }

    val startEpochDay: Long get() = startDate.toEpochDay()
    val endExclusiveEpochDay: Long get() = endExclusiveDate.toEpochDay()
    val endInclusiveEpochDay: Long get() = endExclusiveEpochDay - 1

    val label: String
        get() = when (granularity) {
            StatisticsGranularity.WEEK -> weekLabel(startDate, endExclusiveDate.minusDays(1))
            StatisticsGranularity.MONTH -> "${startDate.year}年${startDate.monthValue}月"
            StatisticsGranularity.YEAR -> "${startDate.year}年"
        }

    fun previous(): StatisticsPeriod = shift(-1)
    fun next(): StatisticsPeriod = shift(1)

    fun shift(amount: Long): StatisticsPeriod {
        val shifted = when (granularity) {
            StatisticsGranularity.WEEK -> startDate.plusWeeks(amount)
            StatisticsGranularity.MONTH -> startDate.plusMonths(amount)
            StatisticsGranularity.YEAR -> startDate.plusYears(amount)
        }
        return copy(anchorEpochDay = shifted.toEpochDay())
    }

    fun withGranularity(value: StatisticsGranularity): StatisticsPeriod =
        StatisticsPeriod(value, anchorEpochDay)

    fun comparisonPeriods(count: Int = 5): List<StatisticsPeriod> {
        require(count > 0) { "对比周期数量必须大于零" }
        return (count - 1 downTo 0).map { shift(-it.toLong()) }
    }

    fun averageDayCount(today: LocalDate): Int {
        val lastDate = endExclusiveDate.minusDays(1)
        val effectiveEnd = when {
            today < startDate -> lastDate
            today > lastDate -> lastDate
            else -> today
        }
        return (effectiveEnd.toEpochDay() - startDate.toEpochDay() + 1).toInt().coerceAtLeast(1)
    }

    companion object {
        fun from(granularity: StatisticsGranularity, date: LocalDate): StatisticsPeriod =
            StatisticsPeriod(granularity, date.toEpochDay())

        private fun weekLabel(start: LocalDate, end: LocalDate): String = when {
            start.year != end.year ->
                "${start.format(DateTimeFormatter.ofPattern("yyyy年M月d日"))}–" +
                    end.format(DateTimeFormatter.ofPattern("yyyy年M月d日"))
            start.monthValue != end.monthValue ->
                "${start.format(DateTimeFormatter.ofPattern("yyyy年M月d日"))}–" +
                    end.format(DateTimeFormatter.ofPattern("M月d日"))
            else -> "${start.year}年${start.monthValue}月${start.dayOfMonth}日–${end.dayOfMonth}日"
        }
    }
}

data class StatisticsTrendPoint(
    val label: String,
    val income: Money,
    val expense: Money,
)

data class PeriodExpenseComparison(
    val label: String,
    val expense: Money,
    val income: Money = Money.fromMinor(0),
)

data class StatisticsInsights(
    val period: StatisticsPeriod,
    val summary: MonthlySummary,
    val averageDailyExpense: Money,
    val averageDailyIncome: Money,
    val categories: List<CategorySpending>,
    val incomeCategories: List<CategorySpending>,
    val trend: List<StatisticsTrendPoint>,
    val comparisons: List<PeriodExpenseComparison>,
) {
    companion object {
        fun empty(period: StatisticsPeriod) = StatisticsInsights(
            period = period,
            summary = MonthlySummary(Money.fromMinor(0), Money.fromMinor(0), Money.fromMinor(0), 0),
            averageDailyExpense = Money.fromMinor(0),
            averageDailyIncome = Money.fromMinor(0),
            categories = emptyList(),
            incomeCategories = emptyList(),
            trend = emptyList(),
            comparisons = emptyList(),
        )
    }
}

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
