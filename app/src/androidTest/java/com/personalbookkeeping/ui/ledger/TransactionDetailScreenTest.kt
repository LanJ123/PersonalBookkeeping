package com.personalbookkeeping.ui.ledger

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.personalbookkeeping.ui.theme.BookkeepingTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TransactionDetailScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun detailUsesContentTitleWithoutBackButton() {
        composeRule.setContent {
            BookkeepingTheme {
                TransactionDetailScreen(
                    state = TransactionDetailUiState(),
                    onEdit = {},
                    onDelete = {},
                )
            }
        }

        composeRule.onNodeWithText("流水详情").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("‹ 返回").fetchSemanticsNodes().isEmpty())
    }
}
