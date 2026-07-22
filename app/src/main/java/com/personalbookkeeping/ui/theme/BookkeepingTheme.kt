package com.personalbookkeeping.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF7B4B2A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBC5),
    background = Color(0xFFFFF9F4),
    surface = Color(0xFFFFF9F4),
    surfaceVariant = Color(0xFFF3DED2),
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB68A),
    onPrimary = Color(0xFF4A280F),
    primaryContainer = Color(0xFF623A20),
    background = Color(0xFF18120E),
    surface = Color(0xFF18120E),
    surfaceVariant = Color(0xFF51443C),
    error = Color(0xFFFFB4AB),
)

@Composable
fun BookkeepingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
