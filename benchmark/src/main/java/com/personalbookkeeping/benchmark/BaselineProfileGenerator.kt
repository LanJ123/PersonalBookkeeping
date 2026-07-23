package com.personalbookkeeping.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun criticalUserJourneys() = baselineProfileRule.collect(
        packageName = TARGET_PACKAGE,
        includeInStartupProfile = false,
    ) {
        pressHome()
        startActivityAndWait()
        listOf("流水", "统计", "设置", "首页").forEach { label ->
            val tab = device.wait(Until.findObject(By.text(label)), 5_000)
            checkNotNull(tab).click()
            device.waitForIdle()
        }
    }
}
