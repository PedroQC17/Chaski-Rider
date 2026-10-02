package com.example.chaskirider.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ChaskiColors = lightColorScheme(
    primary = Orange, onPrimary = Color.White, secondary = Orange,
    primaryContainer = Color(0xFFFFE8DC), onPrimaryContainer = TextDark,
    secondaryContainer = Color(0xFFFFEEE5), onSecondaryContainer = Orange,
    surfaceTint = Color.Transparent, surfaceVariant = Color(0xFFFFF8F3),
    surfaceContainerHigh = Color.White, surfaceContainerLow = Color.White,
    background = BackgroundLight, surface = Color.White,
    onSurface = TextDark, onBackground = TextDark, outline = BorderLight
)

@Composable
fun ChaskiRiderTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ChaskiColors, typography = Typography, content = content)
}
