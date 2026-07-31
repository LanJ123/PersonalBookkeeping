package com.personalbookkeeping.ui.management

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.personalbookkeeping.ui.theme.BookkeepingTheme
import org.junit.Rule
import org.junit.Test

class ManagementScreensTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun accountsUsesLargeTitleAndBottomAddButton() {
        composeRule.setContent {
            BookkeepingTheme {
                AccountsScreen(
                    state = ManagementUiState(isLoading = false),
                    snackbarHostState = SnackbarHostState(),
                    onSave = { _, _, _, _, _ -> },
                    onDeactivate = {},
                    onMove = { _, _ -> },
                    onMessageConsumed = {},
                )
            }
        }

        composeRule.onNodeWithText("账户管理").assertIsDisplayed()
        composeRule.onNodeWithText("‹ 返回").assertDoesNotExist()
        composeRule.onNodeWithText("新增").assertDoesNotExist()
        composeRule.onNodeWithTag("account-add-fab").assertIsDisplayed()
    }

    @Test
    fun categoriesUsesLargeTitleAndBottomAddButton() {
        composeRule.setContent {
            BookkeepingTheme {
                CategoriesScreen(
                    state = ManagementUiState(isLoading = false),
                    snackbarHostState = SnackbarHostState(),
                    onSave = { _, _, _ -> },
                    onDeactivate = {},
                    onMove = { _, _ -> },
                    onMessageConsumed = {},
                )
            }
        }

        composeRule.onNodeWithText("分类管理").assertIsDisplayed()
        composeRule.onNodeWithText("‹ 返回").assertDoesNotExist()
        composeRule.onNodeWithText("新增").assertDoesNotExist()
        composeRule.onNodeWithTag("category-add-fab").assertIsDisplayed()
    }
}
