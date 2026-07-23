package com.personalbookkeeping.ui.settings

import android.accessibilityservice.AccessibilityService
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.personalbookkeeping.app.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivitySafTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun createDocumentLauncherStartsAndCancellationReturnsWithoutCrash() {
        composeRule.onNodeWithText("设置").performClick()
        composeRule.onNodeWithText("数据与备份").performClick()
        composeRule.onNodeWithText("创建完整备份").performClick()
        composeRule.onNodeWithText("选择保存位置").performClick()
        composeRule.waitForIdle()

        InstrumentationRegistry.getInstrumentation().uiAutomation
            .performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)

        composeRule.waitUntil(5_000) {
            runCatching { composeRule.onNodeWithText("数据与备份").assertIsDisplayed() }.isSuccess
        }
        composeRule.onNodeWithText("数据与备份").assertIsDisplayed()
    }
}
