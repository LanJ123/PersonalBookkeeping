package com.personalbookkeeping.ui.privacy

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.personalbookkeeping.common.Money

val LocalAmountsHidden = staticCompositionLocalOf { false }

@Composable
fun AmountPrivacyProvider(hidden: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAmountsHidden provides hidden, content = content)
}

@Composable
fun Money.displayCny(showPositiveSign: Boolean = false): String =
    if (LocalAmountsHidden.current) "••••" else formatCny(showPositiveSign)
