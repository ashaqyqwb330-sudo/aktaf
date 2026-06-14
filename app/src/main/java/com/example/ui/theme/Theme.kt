package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DayColorScheme = lightColorScheme(
    primary = WarmGold,
    secondary = GoldLight,
    background = DeepObsidian, // Deep Obsidian background
    surface = SurfaceDark, // Deep obsidian surface
    onPrimary = DeepObsidian,
    onSecondary = TextLight,
    onBackground = TextLight,
    onSurface = TextLight,
    error = RedAccent
)

private val NightColorScheme = darkColorScheme(
    primary = WarmGold,
    secondary = GoldLight,
    background = DeepObsidian, // Deep Obsidian background
    surface = CardGradStart, // Dark surface
    onPrimary = DeepObsidian,
    onSecondary = TextLight,
    onBackground = TextLight,
    onSurface = TextLight,
    error = RedAccent
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) NightColorScheme else DayColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun EktefaaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MyApplicationTheme(darkTheme = darkTheme, content = content)
}
