package com.personalbookkeeping.ui.insights

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.personalbookkeeping.common.Money
import com.personalbookkeeping.domain.model.BudgetCalculator
import com.personalbookkeeping.domain.model.CategoryOption
import com.personalbookkeeping.domain.model.CategoryKind
import com.personalbookkeeping.domain.model.CategorySpending
import com.personalbookkeeping.domain.model.DailyTrend
import com.personalbookkeeping.domain.model.MonthPeriod
import com.personalbookkeeping.domain.model.MonthlyInsights
import com.personalbookkeeping.domain.model.MonthlySummary
import com.personalbookkeeping.ui.theme.BookkeepingTheme
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

class InsightsScreensTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeShowsEmptyStateAndExplicitBudgetWarning() {
        val state = state(
            MonthlyInsights.empty(PERIOD).copy(
                totalBudget = BudgetCalculator.progress(null, null, Money.fromMinor(10_000), Money.fromMinor(8_000)),
            ),
        )
        composeRule.setContent {
            BookkeepingTheme {
                HomeScreen(
                    state = state,
                    snackbarHostState = remember { SnackbarHostState() },
                    onPreviousMonth = {}, onNextMonth = {}, onViewAll = {},
                    onTransactionClick = {}, onBudgets = {}, onMessageConsumed = {},
                )
            }
        }

        composeRule.onNodeWithText("接近预算").assertIsDisplayed()
        composeRule.onNodeWithText("点击右下角，记下第一笔").assertIsDisplayed()
    }

    @Test
    fun statisticsShowsRankingShareAndTrendAlternative() {
        val insights = MonthlyInsights.empty(PERIOD).copy(
            summary = MonthlySummary(Money.fromMinor(2_000), Money.fromMinor(10_000), Money.fromMinor(-8_000), 2),
            categories = listOf(
                CategorySpending("food", "餐饮", Money.fromMinor(6_000)),
                CategorySpending("traffic", "交通", Money.fromMinor(4_000)),
            ),
            dailyTrend = listOf(
                DailyTrend(LocalDate.of(2026, 7, 1).toEpochDay(), Money.fromMinor(2_000), Money.fromMinor(10_000)),
            ),
            expenseCategories = listOf(CategoryOption("food", "餐饮", CategoryKind.EXPENSE)),
        )
        composeRule.setContent {
            BookkeepingTheme {
                StatisticsScreen(state(insights), {}, {}, { _, _ -> })
            }
        }

        composeRule.onNodeWithText("餐饮").assertIsDisplayed()
        composeRule.onNodeWithText("60%").assertIsDisplayed()
        composeRule.onNodeWithText("每日收支趋势").assertIsDisplayed()
        composeRule.onNodeWithText("共 1 个有收支记录的日期；图形仅作趋势辅助，准确金额以汇总和流水为准。").assertIsDisplayed()
    }

    private fun state(insights: MonthlyInsights) = InsightsUiState(PERIOD, insights, isLoading = false)

    companion object {
        private val PERIOD = MonthPeriod(2026, 7)
    }
}
