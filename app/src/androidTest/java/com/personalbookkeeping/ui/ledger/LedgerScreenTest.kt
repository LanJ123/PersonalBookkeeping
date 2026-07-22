package com.personalbookkeeping.ui.ledger

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.personalbookkeeping.domain.model.TransactionFilter
import com.personalbookkeeping.ui.theme.BookkeepingTheme
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test

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
}
