package com.personalbookkeeping.domain.model

import com.personalbookkeeping.common.Money
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class InsightsModelsTest {
    @Test
    fun monthPeriodHandlesLeapYearAndYearBoundary() {
        val february = MonthPeriod(2024, 2)
        assertEquals("2024-02", february.key)
        assertEquals(LocalDate.of(2024, 2, 1).toEpochDay(), february.startEpochDay)
        assertEquals(LocalDate.of(2024, 3, 1).toEpochDay(), february.endExclusiveEpochDay)
        assertEquals(LocalDate.of(2024, 2, 29).toEpochDay(), february.endInclusiveEpochDay)
        assertEquals(MonthPeriod(2025, 1), MonthPeriod(2024, 12).next())
        assertEquals(MonthPeriod(2024, 12), MonthPeriod(2025, 1).previous())
    }

    @Test
    fun budgetThresholdsUseExactIntegerBoundaries() {
        val limit = Money.fromMinor(10_000)

        assertEquals(BudgetStatus.NORMAL, progress(limit, 7_999).status)
        assertEquals(BudgetStatus.NEAR_LIMIT, progress(limit, 8_000).status)
        assertEquals(BudgetStatus.NEAR_LIMIT, progress(limit, 9_999).status)
        assertEquals(BudgetStatus.EXCEEDED, progress(limit, 10_000).status)
        assertEquals(BudgetStatus.EXCEEDED, progress(limit, 12_500).status)
        assertEquals(-2_500L, progress(limit, 12_500).remaining.minorUnits)
    }

    @Test
    fun statisticsPeriodsUseNaturalWeekMonthAndYearBoundaries() {
        val anchor = LocalDate.of(2026, 7, 26)
        val week = StatisticsPeriod.from(StatisticsGranularity.WEEK, anchor)
        val month = week.withGranularity(StatisticsGranularity.MONTH)
        val year = week.withGranularity(StatisticsGranularity.YEAR)

        assertEquals(LocalDate.of(2026, 7, 20), week.startDate)
        assertEquals(LocalDate.of(2026, 7, 27), week.endExclusiveDate)
        assertEquals("2026年7月20日–26日", week.label)
        assertEquals(LocalDate.of(2026, 7, 1), month.startDate)
        assertEquals(LocalDate.of(2026, 8, 1), month.endExclusiveDate)
        assertEquals(LocalDate.of(2026, 1, 1), year.startDate)
        assertEquals(LocalDate.of(2027, 1, 1), year.endExclusiveDate)
        assertEquals(LocalDate.of(2022, 1, 1), year.comparisonPeriods().first().startDate)
    }

    private fun progress(limit: Money, used: Long) = BudgetCalculator.progress(
        categoryId = null,
        categoryName = null,
        limit = limit,
        used = Money.fromMinor(used),
    )
}
