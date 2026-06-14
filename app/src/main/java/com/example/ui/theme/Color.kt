package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

val WarmGold = Color(0xFFC59A28) // Rich Dark Gold
val GoldLight = Color(0xFFF3D573) // Glowing Gold Light
val WarmWhite = Color(0xFF0F1626) // Deep Blue Background instead of WarmWhite
val DeepObsidian = Color(0xFF08090C) // Absolute Deep obsidian background
val SurfaceWarm = Color(0xFF16223F) // Dark Blue Surface
val MidnightBlue = Color(0xFF090D1A) // Very Deep Blue/Black
val SurfaceDark = Color(0xFF131B31) // Luxurious Navy Surface
val DarkNavyShadow = Color(0xFF04060C)
val RedAccent = Color(0xFFD34545)
val GreenAccent = Color(0xFF43C586)
val SilverGray = Color(0xFF8C9BB0)
val Gold = WarmGold
val Silver = SilverGray

// Immersive UI Additions
val ImmersiveBg = Color(0xFF080C1E) // Premium deep dark cinematic blue
val CardGradStart = Color(0xFF131B31)
val CardGradEnd = Color(0xFF0B1020)
val TextLight = Color(0xFFFFFBF5)
val TextMuted = Color(0xFF90A3BC)

// Hoisted reactive states for dynamic design customized in Design Oasis
object GlobalDesignConfig {
    var activeThemeIdx by androidx.compose.runtime.mutableStateOf(0)
    var strokeThickness by androidx.compose.runtime.mutableStateOf(2.5f)
    var blurRadiusState by androidx.compose.runtime.mutableStateOf(10f)
    var starCountState by androidx.compose.runtime.mutableStateOf(15)
    var starSpeedState by androidx.compose.runtime.mutableStateOf(2.0f)
    var shimmerEnabled by androidx.compose.runtime.mutableStateOf(true)
    var interactiveConstellations by androidx.compose.runtime.mutableStateOf(true)
    var activeCornerShapeIdx by androidx.compose.runtime.mutableStateOf(0)

    fun getPrimaryColor(): Color {
        return when (activeThemeIdx) {
            0 -> Color(0xFFC59A28) // Rich Dark Gold (WarmGold)
            1 -> Color(0xFF43C586) // Emerald Green
            2 -> Color(0xFFD34545) // Ruby Crimson
            3 -> Color(0xFF9E77ED) // Royal Violet
            else -> Color(0xFFE2B83E) // Hyper Cyber Gold
        }
    }

    fun getBgColors(): List<Color> {
        return when (activeThemeIdx) {
            0 -> listOf(Color(0xFF04060C), Color(0xFF090D1A), Color(0xFF0E162D))
            1 -> listOf(Color(0xFF020E08), Color(0xFF041E12), Color(0xFF092C1D))
            2 -> listOf(Color(0xFF0C0202), Color(0xFF1E0404), Color(0xFF2E0909))
            3 -> listOf(Color(0xFF09040E), Color(0xFF160925), Color(0xFF220E37))
            else -> listOf(Color(0xFF070502), Color(0xFF1A1206), Color(0xFF281C09))
        }
    }
}


