package com.personalbookkeeping.ui.accessibility

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.personalbookkeeping.app.MainActivity
import com.personalbookkeeping.ui.settings.DataTransferScreen
import com.personalbookkeeping.ui.settings.SettingsUiState
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

class MainNavigationAccessibilityTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun rootNavigationAndPrimaryActionHaveAccessibleTextAndActions() {
        listOf("首页", "流水", "统计", "设置").forEach { label ->
            rule.onAllNodesWithText(label).assertAny(hasClickAction())
        }
        rule.onNodeWithText("＋记一笔").assertIsDisplayed().assertHasClickAction()
    }
}

class LargeFontAccessibilityTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun dataTransferActionsRemainReachableAt320DpAnd130PercentFont() {
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 1.3f)) {
                MaterialTheme {
                    Box(Modifier.width(320.dp).height(600.dp)) {
                        DataTransferScreen(
                            state = SettingsUiState(),
                            onBack = {},
                            onBackup = {},
                            onRestoreSelected = {},
                            onConfirmRestore = {},
                            onCancelRestore = {},
                            onExportCsv = { _, _: LocalDate, _: LocalDate -> },
                            onInputError = {},
                        )
                    }
                }
            }
        }

        rule.onNodeWithText("创建完整备份").assertIsDisplayed().assertHasClickAction()
        rule.onNodeWithText("导出 CSV").performScrollTo().assertIsDisplayed().assertHasClickAction()
    }
}
