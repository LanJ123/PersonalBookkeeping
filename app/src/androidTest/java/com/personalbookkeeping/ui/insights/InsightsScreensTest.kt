package com.personalbookkeeping.ui.insights

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.personalbookkeeping.common.Money
import com.personalbookkeeping.domain.model.CategoryOption
import com.personalbookkeeping.domain.model.CategoryKind
import com.personalbookkeeping.domain.model.CategorySpending
import com.personalbookkeeping.domain.model.MonthPeriod
import com.personalbookkeeping.domain.model.MonthlySummary
import com.personalbookkeeping.domain.model.PeriodExpenseComparison
import com.personalbookkeeping.domain.model.RecentTransaction
import com.personalbookkeeping.domain.model.StatisticsGranularity
import com.personalbookkeeping.domain.model.StatisticsInsights
import com.personalbookkeeping.domain.model.StatisticsPeriod
import com.personalbookkeeping.domain.model.StatisticsTrendPoint
import com.personalbookkeeping.domain.model.TransactionType
import com.personalbookkeeping.ui.theme.BookkeepingTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Rule
import org.junit.Test

class InsightsScreensTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeShowsSummaryAndAllCurrentMonthTransactions() {
        val state = HomeUiState(
            period = PERIOD,
            summary = MonthlySummary(
                income = Money.fromMinor(200_000),
                expense = Money.fromMinor(120_000),
                balance = Money.fromMinor(80_000),
                transactionCount = 12,
            ),
            todayExpense = Money.fromMinor(2_550),
            transactions = listOf(
                RecentTransaction(
                    id = "tx-1",
                    type = TransactionType.EXPENSE,
                    amount = Money.fromMinor(2_550),
                    categoryName = "餐饮",
                    accountName = "现金",
                    targetAccountName = null,
                    occurredAt = Instant.parse("2026-07-26T02:35:00Z"),
                    zoneId = ZoneId.of("Asia/Shanghai"),
                    localDateEpochDay = LocalDate.of(2026, 7, 26).toEpochDay(),
                ),
                RecentTransaction(
                    id = "tx-2",
                    type = TransactionType.INCOME,
                    amount = Money.fromMinor(10_000),
                    categoryName = "工资",
                    accountName = "银行卡",
                    targetAccountName = null,
                    occurredAt = Instant.parse("2026-07-26T01:00:00Z"),
                    zoneId = ZoneId.of("Asia/Shanghai"),
                    localDateEpochDay = LocalDate.of(2026, 7, 26).toEpochDay(),
                ),
                RecentTransaction(
                    id = "tx-3",
                    type = TransactionType.EXPENSE,
                    amount = Money.fromMinor(1_000),
                    categoryName = "交通",
                    accountName = "现金",
                    targetAccountName = null,
                    occurredAt = Instant.parse("2026-07-25T01:00:00Z"),
                    zoneId = ZoneId.of("Asia/Shanghai"),
                    localDateEpochDay = LocalDate.of(2026, 7, 25).toEpochDay(),
                ),
            ),
            isLoading = false,
        )
        composeRule.setContent {
            BookkeepingTheme {
                HomeScreen(state = state, onTransactionClick = {})
            }
        }

        composeRule.onNodeWithText("今日支出").assertIsDisplayed()
        composeRule.onNodeWithText("本月支出").assertIsDisplayed()
        composeRule.onNodeWithText("本月收入").assertIsDisplayed()
        composeRule.onNodeWithText("本月流水（3笔）").assertIsDisplayed()
        composeRule.onNodeWithText("7月26日 星期日").assertIsDisplayed()
        composeRule.onNodeWithText("支 ¥25.50  收 ¥100.00").assertIsDisplayed()
        composeRule.onNodeWithTag("home-list").performScrollToNode(hasText("餐饮"))
        composeRule.onNodeWithText("餐饮").assertIsDisplayed()
    }

    @Test
    fun statisticsShowsRankingShareAndTrendAlternative() {
        val statisticsPeriod = StatisticsPeriod.from(
            StatisticsGranularity.MONTH,
            LocalDate.of(2026, 7, 1),
        )
        val insights = StatisticsInsights.empty(statisticsPeriod).copy(
            summary = MonthlySummary(Money.fromMinor(2_000), Money.fromMinor(10_000), Money.fromMinor(-8_000), 2),
            categories = listOf(
                CategorySpending("food", "餐饮", Money.fromMinor(6_000), 2),
                CategorySpending("traffic", "交通", Money.fromMinor(4_000), 1),
            ),
            incomeCategories = listOf(
                CategorySpending("salary", "工资", Money.fromMinor(2_000), 1),
            ),
            trend = listOf(
                StatisticsTrendPoint("1日", Money.fromMinor(2_000), Money.fromMinor(10_000)),
            ),
            comparisons = listOf(
                PeriodExpenseComparison("6月", Money.fromMinor(8_000), Money.fromMinor(1_000)),
                PeriodExpenseComparison("7月", Money.fromMinor(10_000), Money.fromMinor(2_000)),
            ),
        )
        composeRule.setContent {
            BookkeepingTheme {
                var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
                StatisticsScreen(
                    state = StatisticsUiState(
                        statisticsPeriod,
                        insights,
                        type = selectedType,
                        isLoading = false,
                    ),
                    onGranularitySelected = {},
                    onTypeSelected = { selectedType = it },
                    onPreviousPeriod = {},
                    onNextPeriod = {},
                    onCategoryClick = { _, _, _ -> },
                )
            }
        }

        composeRule.onNodeWithTag("statistics-list")
            .performScrollToNode(hasText("餐饮"))
        composeRule.onNodeWithText("餐饮").assertIsDisplayed()
        composeRule.onNodeWithText("60%").assertIsDisplayed()
        composeRule.onNodeWithText("2笔").assertIsDisplayed()
        composeRule.onNodeWithTag("statistics-list")
            .performScrollToNode(hasTestTag("statistics-type-income"))
        composeRule.onNodeWithTag("statistics-type-income").performClick()
        composeRule.onNodeWithTag("statistics-list").performScrollToNode(hasText("工资"))
        composeRule.onNodeWithText("工资").assertIsDisplayed()
        composeRule.onNodeWithText("1笔").assertIsDisplayed()
        composeRule.onNodeWithText("月收入趋势").assertIsDisplayed()
        composeRule.onNodeWithText("日均收入").assertIsDisplayed()
        composeRule.onNodeWithText("比上月收入").assertIsDisplayed()
        composeRule.onNodeWithTag("statistics-list").performScrollToNode(hasText("月收入对比"))
        composeRule.onNodeWithText("月收入对比").assertIsDisplayed()
    }

    companion object {
        private val PERIOD = MonthPeriod(2026, 7)
    }
}
