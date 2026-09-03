package com.iykyk.app.presentation.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Background Gradients (Deep atmospheric dark space navy)
val BgDark = Color(0xFF090714)
val BgDarkSecondary = Color(0xFF130E26)
val BgDarkTertiary = Color(0xFF0E0B1A)

val BackgroundGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF090714),
        Color(0xFF130E26),
        Color(0xFF0E0B1A)
    )
)

val SurfaceGlass = Color(0xFF19142E)
val SurfaceDark = Color(0xFF18132F)
val SurfaceDarkStroke = Color(0xFF2C244C)
val SurfaceElevated = Color(0xFF211A3E)
val SurfaceCard = Color(0xFF1D1737)

// Accent Colors matching Sample UI
val PrimaryPurple = Color(0xFF7A4DF2)
val PrimaryPink = Color(0xFFFF3B81)
val PrimaryPinkLight = Color(0xFFFF6584)
val AccentCyan = Color(0xFF42E8E0)
val AccentGold = Color(0xFFFFD15C)
val AccentBlue = Color(0xFF4C82FB)
val SuccessGreen = Color(0xFF10B981)

// Gradients
val PrimaryGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF7A4DF2),
        Color(0xFFFF3B81)
    )
)

val RingGradient = Brush.sweepGradient(
    listOf(
        Color(0xFF7A4DF2),
        Color(0xFF42E8E0),
        Color(0xFFFF3B81),
        Color(0xFF7A4DF2)
    )
)

val CardGlowGradient = Brush.radialGradient(
    colors = listOf(
        Color(0x337A4DF2),
        Color(0x00000000)
    )
)

// Text Colors
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFA5A5C7)
val TextMuted = Color(0xFF6E6E8D)
val TextAccentPink = Color(0xFFFF3B81)
val TextAccentCyan = Color(0xFF42E8E0)
