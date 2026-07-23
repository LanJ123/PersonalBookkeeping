package com.personalbookkeeping.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
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

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        check(
            device.wait(
                Until.hasObject(By.pkg("com.google.android.documentsui")),
                30_000,
            ),
        ) {
            "系统文件选择器未在 30 秒内启动"
        }
        device.pressBack()

        composeRule.waitUntil(15_000) {
            runCatching { composeRule.onNodeWithText("数据与备份").assertIsDisplayed() }.isSuccess
        }
        composeRule.onNodeWithText("数据与备份").assertIsDisplayed()
    }
}
