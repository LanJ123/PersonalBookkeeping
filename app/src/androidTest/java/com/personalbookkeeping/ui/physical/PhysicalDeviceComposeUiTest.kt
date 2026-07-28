package com.personalbookkeeping.ui.physical

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Physical-device compatibility lane for Compose UI.
 *
 * Some vendor Android 16 builds deadlock while Espresso installs its main-loop
 * synchronizer. These tests deliberately avoid Compose/Espresso rules and drive
 * the same Compose semantics tree through UiAutomator instead.
 */
@RunWith(AndroidJUnit4::class)
class PhysicalDeviceComposeUiTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instrumentation)

    @Before
    fun launchApp() {
        device.wakeUp()
        device.pressHome()
        val launchResult = device.executeShellCommand(
            "am start -W -f 0x10008000 -n $TARGET_PACKAGE/.MainActivity",
        )
        check(launchResult.contains("Status: ok")) {
            "无法自动启动应用：$launchResult"
        }
        waitForText("＋记一笔")
    }

    @After
    fun leaveAppIdle() {
        device.pressHome()
    }

    @Test
    fun rootNavigationReachesEveryComposeScreen() {
        clickBottomTab("流水")
        waitForText("搜索备注")

        clickBottomTab("统计")
        waitForText("上一月")

        clickBottomTab("设置")
        waitForText("账户管理")
        waitForText("隐私与安全")
    }

    @Test
    fun privacyControlsReflectChangesImmediately() {
        openPrivacy()

        val hideAmounts = waitForResource(HIDE_AMOUNTS_TAG)
        val originalHidden = hideAmounts.isChecked
        hideAmounts.click()
        assertTrue(
            "隐藏金额开关点击后未即时刷新",
            waitForChecked(HIDE_AMOUNTS_TAG, !originalHidden),
        )
        waitForResource(HIDE_AMOUNTS_TAG).click()
        assertTrue(
            "隐藏金额开关未恢复原状态",
            waitForChecked(HIDE_AMOUNTS_TAG, originalHidden),
        )

        val selectedTheme = THEME_TAGS.firstOrNull { resource ->
            device.findObject(By.res(resource))?.isActive == true
        }
        val alternateTheme = THEME_TAGS.first { it != selectedTheme }
        waitForResource(alternateTheme).click()
        assertTrue(
            "主题选项点击后未即时刷新",
            waitForActive(alternateTheme),
        )
        selectedTheme?.let {
            waitForResource(it).click()
            assertTrue("主题选项未恢复原状态", waitForActive(it))
        }
    }

    @Test
    fun transactionEditorSavesAndShowsFeedback() {
        clickText("＋记一笔")
        waitForText("金额")

        val amount = waitForResource(AMOUNT_FIELD_TAG)
        amount.click()
        amount.text = SAVE_AMOUNT
        device.pressBack()

        findSaveButton().click()
        assertNotNull(
            "保存后未显示成功反馈",
            device.wait(Until.findObject(By.res(SAVE_SUCCESS_TAG)), UI_TIMEOUT_MS),
        )
    }

    @Test
    fun backupWarningCanBeCancelledWithoutLeavingScreen() {
        openDataTransfer()
        clickText("创建完整备份")
        waitForText("备份包含个人财务数据且未加密，请保存到可信位置。")
        clickText("取消")
        waitForText("数据与备份")
        assertTrue(device.hasObject(By.text("创建完整备份")))
    }

    @Test
    fun systemDocumentPickerLaunchesAndReturnsToComposeScreen() {
        openDataTransfer()
        clickText("创建完整备份")
        clickText("选择保存位置")

        assertTrue(
            "系统文件选择器未在 30 秒内启动",
            waitUntil(DOCUMENT_PICKER_TIMEOUT_MS) {
                device.currentPackageName in DOCUMENTS_UI_PACKAGES
            },
        )
        device.pressBack()
        waitForText("数据与备份")
        assertEquals(TARGET_PACKAGE, device.currentPackageName)
    }

    private fun openPrivacy() {
        clickBottomTab("设置")
        clickText("隐私与安全")
        waitForText("隐藏金额")
    }

    private fun openDataTransfer() {
        clickBottomTab("设置")
        clickText("数据与备份")
        waitForText("完整备份")
    }

    private fun clickBottomTab(text: String) {
        waitForText(text)
        val tab = device.findObjects(By.text(text))
            .maxByOrNull { it.visibleBounds.centerY() }
            ?: error("未找到底部导航：$text")
        tab.click()
    }

    private fun clickText(text: String) {
        waitForText(text).click()
    }

    private fun waitForText(text: String): UiObject2 =
        device.wait(Until.findObject(By.text(text)), UI_TIMEOUT_MS)
            ?: error("未找到文本：$text")

    private fun waitForResource(resource: String): UiObject2 =
        device.wait(Until.findObject(By.res(resource)), UI_TIMEOUT_MS)
            ?: error("未找到 Compose 测试标记：$resource")

    private fun waitForChecked(resource: String, expected: Boolean): Boolean =
        waitUntil { device.findObject(By.res(resource))?.isChecked == expected }

    private fun waitForActive(resource: String): Boolean =
        waitUntil { device.findObject(By.res(resource))?.isActive == true }

    private fun waitUntil(
        timeoutMs: Long = UI_TIMEOUT_MS,
        condition: () -> Boolean,
    ): Boolean {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) return true
            SystemClock.sleep(POLL_INTERVAL_MS)
        }
        return condition()
    }

    private val UiObject2.isActive: Boolean
        get() = isChecked || isSelected

    private fun findSaveButton(): UiObject2 {
        repeat(MAX_SAVE_BUTTON_SCROLLS) {
            device.wait(Until.findObject(By.res(SAVE_BUTTON_TAG)), SHORT_UI_TIMEOUT_MS)
                ?.let { return it }
            device.swipe(
                device.displayWidth / 2,
                device.displayHeight * 3 / 4,
                device.displayWidth / 2,
                device.displayHeight / 4,
                40,
            )
            device.waitForIdle()
        }
        return waitForResource(SAVE_BUTTON_TAG)
    }

    private companion object {
        const val TARGET_PACKAGE = "com.personalbookkeeping.app"
        const val HIDE_AMOUNTS_TAG = "privacy-hide-amounts"
        const val AMOUNT_FIELD_TAG = "transaction-amount"
        const val SAVE_BUTTON_TAG = "transaction-save"
        const val SAVE_SUCCESS_TAG = "transaction-save-success"
        const val SAVE_AMOUNT = "23.45"
        const val UI_TIMEOUT_MS = 15_000L
        const val SHORT_UI_TIMEOUT_MS = 2_000L
        const val DOCUMENT_PICKER_TIMEOUT_MS = 30_000L
        const val POLL_INTERVAL_MS = 100L
        const val MAX_SAVE_BUTTON_SCROLLS = 4
        val THEME_TAGS = listOf(
            "privacy-theme-system",
            "privacy-theme-light",
            "privacy-theme-dark",
        )
        val DOCUMENTS_UI_PACKAGES = setOf(
            "com.android.documentsui",
            "com.google.android.documentsui",
        )
    }
}
