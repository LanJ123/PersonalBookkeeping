package com.personalbookkeeping.ui.transaction

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.personalbookkeeping.ui.theme.BookkeepingTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TransactionEditorScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun initialScreenShowsEditorAndLoadingState() {
        composeRule.setContent {
            BookkeepingTheme {
                TransactionEditorScreen(
                    state = TransactionEditorUiState(),
                    onAmountChanged = {},
                    onNoteChanged = {},
                    onTypeSelected = {},
                    onAccountSelected = {},
                    onTargetAccountSelected = {},
                    onCategorySelected = {},
                    onDateSelected = {},
                    onTimeSelected = { _, _ -> },
                    onSave = {},
                )
            }
        }

        composeRule.onNodeWithText("记一笔").assertIsDisplayed()
        composeRule.onNodeWithText("正在准备本地账本…").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("最近流水").fetchSemanticsNodes().isEmpty())
    }
}
