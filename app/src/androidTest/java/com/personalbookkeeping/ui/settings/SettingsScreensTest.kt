package com.personalbookkeeping.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import com.personalbookkeeping.backup.BackupCounts
import com.personalbookkeeping.backup.RestoreReview
import com.personalbookkeeping.domain.model.ThemeMode
import com.personalbookkeeping.ui.theme.BookkeepingTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsScreensTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun childScreensUseContentTitlesWithoutVisibleBackButton() {
        composeRule.setContent {
            BookkeepingTheme {
                DataTransferScreen(
                    state = SettingsUiState(),
                    onBackup = {},
                    onRestoreSelected = {},
                    onConfirmRestore = {},
                    onCancelRestore = {},
                    onExportCsv = { _, _, _ -> },
                    onInputError = {},
                )
            }
        }

        composeRule.onNodeWithText("数据与备份").assertIsDisplayed()
        composeRule.onNodeWithText("‹ 返回").assertDoesNotExist()
    }

    @Test
    fun backupWarningDistinguishesUnencryptedBackupFromCsv() {
        composeRule.setContent {
            BookkeepingTheme {
                DataTransferScreen(
                    state = SettingsUiState(),
                    onBackup = {}, onRestoreSelected = {}, onConfirmRestore = {},
                    onCancelRestore = {}, onExportCsv = { _, _, _ -> }, onInputError = {},
                )
            }
        }

        composeRule.onNodeWithText("创建完整备份").performClick()
        composeRule.onNodeWithText("备份包含个人财务数据且未加密，请保存到可信位置。").assertIsDisplayed()
        composeRule.onNodeWithText("CSV 仅用于查看和分析，不能用于完整恢复。").assertIsDisplayed()
    }

    @Test
    fun restoreReviewShowsCountsBeforeConfirmation() {
        val review = RestoreReview(
            token = "token",
            createdAt = "2026-07-23T08:00:00Z",
            appVersionName = "0.1.0-dev",
            counts = BackupCounts(accounts = 2, categories = 10, transactions = 7, budgets = 1),
        )
        composeRule.setContent {
            BookkeepingTheme {
                DataTransferScreen(
                    state = SettingsUiState(restoreReview = review),
                    onBackup = {}, onRestoreSelected = {}, onConfirmRestore = {},
                    onCancelRestore = {}, onExportCsv = { _, _, _ -> }, onInputError = {},
                )
            }
        }

        composeRule.onAllNodesWithText("确认恢复").assertCountEquals(2)
        composeRule.onNodeWithText("流水 7 · 预算 1", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("恢复将替换当前数据，并先在本机创建回滚快照。", substring = true).assertIsDisplayed()
    }

    @Test
    fun privacyScreenShowsUnavailableAuthenticationFeedback() {
        composeRule.setContent {
            BookkeepingTheme {
                PrivacySettingsScreen(
                    amountsHidden = false,
                    themeMode = ThemeMode.SYSTEM,
                    appLockEnabled = false,
                    appLockMessage = "设备尚未配置可用的屏幕锁或生物识别",
                    onAmountsHiddenChanged = {},
                    onThemeModeChanged = {},
                    onAppLockChanged = {},
                )
            }
        }

        composeRule.onNodeWithText("设备尚未配置可用的屏幕锁或生物识别").assertIsDisplayed()
    }

    @Test
    fun privacyScreenExposesAllThemeModesAndReportsSelection() {
        var selected: ThemeMode? = null
        composeRule.setContent {
            BookkeepingTheme {
                PrivacySettingsScreen(
                    amountsHidden = false,
                    themeMode = ThemeMode.SYSTEM,
                    appLockEnabled = false,
                    appLockMessage = null,
                    onAmountsHiddenChanged = {},
                    onThemeModeChanged = { selected = it },
                    onAppLockChanged = {},
                )
            }
        }

        composeRule.onNodeWithText("跟随系统").assertIsDisplayed()
        composeRule.onNodeWithText("浅色").assertIsDisplayed()
        composeRule.onNodeWithText("深色").performClick()
        composeRule.runOnIdle { assertEquals(ThemeMode.DARK, selected) }
    }
}
