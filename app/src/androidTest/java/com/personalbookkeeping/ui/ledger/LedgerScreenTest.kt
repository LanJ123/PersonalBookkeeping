package com.personalbookkeeping.ui.ledger

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.personalbookkeeping.common.Money
import com.personalbookkeeping.domain.model.LedgerTransaction
import com.personalbookkeeping.domain.model.MonthPeriod
import com.personalbookkeeping.domain.model.TransactionFilter
import com.personalbookkeeping.domain.model.TransactionType
import com.personalbookkeeping.ui.theme.BookkeepingTheme
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class LedgerScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun filteredEmptyStateOffersClearAction() {
        composeRule.setContent {
            BookkeepingTheme {
                val pagingFlow = remember { flowOf(PagingData.empty<com.personalbookkeeping.domain.model.LedgerTransaction>()) }
                val items = pagingFlow
                    .collectAsLazyPagingItems()
                LedgerScreen(
                    state = LedgerUiState(isInitializing = false, filter = TransactionFilter(noteQuery = "不存在")),
                    transactions = items,
                    snackbarHostState = remember { SnackbarHostState() },
                    onQueryChanged = {}, onTypeChanged = {}, onAccountChanged = {},
                    onCategoryChanged = {}, onDateChanged = { _, _ -> true },
                    onMonthSelected = { _, _ -> true }, onPreviousMonth = {}, onNextMonth = {},
                    onClearMonth = {},
                    onClearFilters = {}, onTransactionClick = {}, onRestore = {},
                    onDeletedConsumed = {}, onMessageConsumed = {},
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("没有符合条件的流水").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("没有符合条件的流水").assertIsDisplayed()
        composeRule.onNodeWithText("清除筛选").assertIsDisplayed()
    }

    @Test
    fun selectedMonthShowsYearMonthAndNavigationActions() {
        composeRule.setContent {
            BookkeepingTheme {
                val pagingFlow = remember {
                    flowOf(PagingData.empty<com.personalbookkeeping.domain.model.LedgerTransaction>())
                }
                LedgerScreen(
                    state = LedgerUiState(
                        isInitializing = false,
                        selectedMonth = MonthPeriod(2026, 7),
                        filter = TransactionFilter(
                            fromEpochDay = MonthPeriod(2026, 7).startEpochDay,
                            toEpochDay = MonthPeriod(2026, 7).endInclusiveEpochDay,
                        ),
                    ),
                    transactions = pagingFlow.collectAsLazyPagingItems(),
                    snackbarHostState = remember { SnackbarHostState() },
                    onQueryChanged = {}, onTypeChanged = {}, onAccountChanged = {},
                    onCategoryChanged = {}, onDateChanged = { _, _ -> true },
                    onMonthSelected = { _, _ -> true }, onPreviousMonth = {}, onNextMonth = {},
                    onClearMonth = {}, onClearFilters = {}, onTransactionClick = {},
                    onRestore = {}, onDeletedConsumed = {}, onMessageConsumed = {},
                )
            }
        }

        composeRule.onNodeWithText("2026年7月").assertIsDisplayed()
        composeRule.onNodeWithText("上月").assertIsDisplayed()
        composeRule.onNodeWithText("下月").assertIsDisplayed()
    }

    @Test
    fun transactionsAreGroupedIntoDailyCardsWithTotalsAndTimes() {
        val july25 = LocalDate.of(2026, 7, 25).toEpochDay()
        val july24 = LocalDate.of(2026, 7, 24).toEpochDay()
        val records = listOf(
            ledgerTransaction(
                id = "expense-1",
                epochDay = july25,
                hour = 20,
                minute = 57,
                amountMinor = 3_488,
                category = "购物",
                dailyExpenseMinor = 11_066,
                dailyIncomeMinor = 10_000,
            ),
            ledgerTransaction(
                id = "expense-2",
                epochDay = july25,
                hour = 20,
                minute = 32,
                amountMinor = 7_578,
                category = "购物",
                dailyExpenseMinor = 11_066,
                dailyIncomeMinor = 10_000,
            ),
            ledgerTransaction(
                id = "income-1",
                epochDay = july24,
                hour = 11,
                minute = 13,
                amountMinor = 10_000,
                category = "其他",
                type = TransactionType.INCOME,
                dailyExpenseMinor = 0,
                dailyIncomeMinor = 10_000,
            ),
        )

        composeRule.setContent {
            BookkeepingTheme {
                val pagingFlow = remember { flowOf(PagingData.from(records)) }
                LedgerScreen(
                    state = LedgerUiState(isInitializing = false),
                    transactions = pagingFlow.collectAsLazyPagingItems(),
                    snackbarHostState = remember { SnackbarHostState() },
                    onQueryChanged = {}, onTypeChanged = {}, onAccountChanged = {},
                    onCategoryChanged = {}, onDateChanged = { _, _ -> true },
                    onMonthSelected = { _, _ -> true }, onPreviousMonth = {}, onNextMonth = {},
                    onClearMonth = {}, onClearFilters = {}, onTransactionClick = {},
                    onRestore = {}, onDeletedConsumed = {}, onMessageConsumed = {},
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("ledger-day-$july25").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("ledger-day-$july25").assertIsDisplayed()
        composeRule.onAllNodesWithText("7月25日 星期六").assertCountEquals(1)
        composeRule.onNodeWithText("支 ¥110.66  收 ¥100.00").assertIsDisplayed()
        composeRule.onNodeWithText("20:57 · 现金").assertIsDisplayed()
        composeRule.onNodeWithTag("ledger-list")
            .performScrollToNode(hasTestTag("ledger-day-$july24"))
        composeRule.onNodeWithTag("ledger-day-$july24").assertIsDisplayed()
        composeRule.onNodeWithText("支 ¥0.00  收 ¥100.00").assertIsDisplayed()
    }

    private fun ledgerTransaction(
        id: String,
        epochDay: Long,
        hour: Int,
        minute: Int,
        amountMinor: Long,
        category: String,
        type: TransactionType = TransactionType.EXPENSE,
        dailyExpenseMinor: Long,
        dailyIncomeMinor: Long,
    ) = LedgerTransaction(
        id = id,
        type = type,
        amount = Money.fromMinor(amountMinor),
        categoryId = "category-$category",
        categoryName = category,
        accountId = "cash",
        accountName = "现金",
        targetAccountId = null,
        targetAccountName = null,
        occurredAt = LocalDate.ofEpochDay(epochDay)
            .atTime(hour, minute)
            .toInstant(ZoneOffset.UTC),
        zoneId = ZoneOffset.UTC,
        localDateEpochDay = epochDay,
        note = null,
        dailyExpense = Money.fromMinor(dailyExpenseMinor),
        dailyIncome = Money.fromMinor(dailyIncomeMinor),
    )
}
