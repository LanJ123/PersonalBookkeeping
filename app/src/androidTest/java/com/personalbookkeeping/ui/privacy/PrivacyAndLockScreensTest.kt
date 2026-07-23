package com.personalbookkeeping.ui.privacy

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.personalbookkeeping.common.Money
import com.personalbookkeeping.ui.security.LockedScreen
import com.personalbookkeeping.ui.theme.BookkeepingTheme
import org.junit.Rule
import org.junit.Test

class PrivacyAndLockScreensTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun amountPrivacyDoesNotExposeRealAmountInSemantics() {
        composeRule.setContent {
            BookkeepingTheme {
                AmountPrivacyProvider(hidden = true) {
                    Text(Money.fromMinor(1_234).displayCny())
                }
            }
        }

        composeRule.onNodeWithText("••••").assertIsDisplayed()
        composeRule.onNodeWithText("¥12.34").assertDoesNotExist()
    }

    @Test
    fun lockedScreenContainsNoBusinessContent() {
        composeRule.setContent {
            BookkeepingTheme {
                LockedScreen(ready = true, message = null, onUnlock = {})
            }
        }

        composeRule.onNodeWithContentDescription("应用已锁定").assertIsDisplayed()
        composeRule.onNodeWithText("系统认证解锁").assertIsDisplayed()
        composeRule.onNodeWithText("本月支出").assertDoesNotExist()
        composeRule.onNodeWithText("流水").assertDoesNotExist()
    }
}
