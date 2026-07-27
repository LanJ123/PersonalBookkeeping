package com.personalbookkeeping.ui.settings

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.personalbookkeeping.app.MainActivity
import org.junit.Rule
import org.junit.Test

class MainActivitySettingsStateTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun persistedPrivacySelectionsImmediatelyUpdateVisibleControlState() {
        composeRule.onNodeWithText("设置").performClick()
        composeRule.onNodeWithText("隐私与安全").performClick()

        val hideAmounts = composeRule.onNodeWithTag("privacy-hide-amounts")
        hideAmounts.assertIsOff()
        hideAmounts.performClick()
        hideAmounts.assertIsOn()
        hideAmounts.performClick()
        hideAmounts.assertIsOff()

        val darkTheme = composeRule.onNodeWithTag("privacy-theme-dark")
        darkTheme.performClick()
        darkTheme.assertIsSelected()

        val systemTheme = composeRule.onNodeWithTag("privacy-theme-system")
        systemTheme.performClick()
        systemTheme.assertIsSelected()
    }
}
