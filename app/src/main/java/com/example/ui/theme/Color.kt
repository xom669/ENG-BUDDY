package com.example.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// EngBuddy v2.0 Base Canvas ("Liquid Glass" Spec)
val ObsidianMatte = Color(0xFF0B0C0E)       // Dark Mode Default Canvas
val ObsidianGlassSurface = Color(0xFF14161A)
val ObsidianElevated = Color(0xFF1C1F26)

val WarmAlabaster = Color(0xFFF8F9FA)       // Light Mode Canvas
val AlabasterGlassSurface = Color(0xFFFFFFFF)
val AlabasterVariant = Color(0xFFEEF1F6)

// Google Fluid 4-Color Palette (Red, Green, Yellow, Blue)
val GoogleBlue = Color(0xFF4285F4)
val GoogleRed = Color(0xFFEA4335)
val GoogleYellow = Color(0xFFFBBC04)
val GoogleGreen = Color(0xFF34A853)

// Legacy aliases for compatibility across modules
val GeminiDarkBackground = ObsidianMatte
val GeminiDarkSurface = ObsidianGlassSurface
val GeminiDarkSurfaceVariant = ObsidianElevated
val GeminiDarkOnBackground = Color(0xFFF1F3F4)
val GeminiDarkOnSurface = Color(0xFFF1F3F4)
val GeminiDarkSecondaryText = Color(0xFF9AA0A6)
val GeminiDarkOutline = Color(0xFF2D3139)

val GeminiLightBackground = WarmAlabaster
val GeminiLightSurface = AlabasterGlassSurface
val GeminiLightSurfaceVariant = AlabasterVariant
val GeminiLightOnBackground = Color(0xFF181A1C)
val GeminiLightOnSurface = Color(0xFF181A1C)
val GeminiLightOnSurfaceVariant = Color(0xFF5F6368)
val GeminiLightOutline = Color(0xFFDADCE0)

val GeminiBlue = GoogleBlue
val GeminiPurple = Color(0xFF8AB4F8)
val GeminiPeach = GoogleYellow
val GeminiCoral = GoogleRed

val StatusSafeGreen = GoogleGreen
val StatusWarningAmber = GoogleYellow
val StatusDangerRed = GoogleRed

val GoogleFluidFourColors = listOf(
    GoogleBlue,
    GoogleRed,
    GoogleYellow,
    GoogleGreen
)

/**
 * 135-degree 4-Color Fluid Gradient (#4285F4 -> #EA4335 -> #FBBC04 -> #34A853).
 * Used at 100% opacity for active 2dp glowing borders and at 14-15% opacity for ambient liquid blobs.
 */
fun googleFluidGradientBrush(
    alpha: Float = 1f,
    start: Offset = Offset.Zero,
    end: Offset = Offset(900f, 900f)
): Brush = Brush.linearGradient(
    colors = listOf(
        GoogleBlue.copy(alpha = alpha),
        GoogleRed.copy(alpha = alpha),
        GoogleYellow.copy(alpha = alpha),
        GoogleGreen.copy(alpha = alpha)
    ),
    start = start,
    end = end
)

fun geminiAuroraBrush(
    start: Offset = Offset.Zero,
    end: Offset = Offset(800f, 300f)
): Brush = googleFluidGradientBrush(alpha = 1f, start = start, end = end)

/**
 * Specular 1dp Rim Border for Liquid Glass cards:
 * Top-left highlight fading to subtle translucent edge.
 */
fun liquidGlassRimBrush(isDark: Boolean): Brush = Brush.linearGradient(
    colors = if (isDark) {
        listOf(
            Color.White.copy(alpha = 0.20f),
            Color.White.copy(alpha = 0.08f),
            Color.White.copy(alpha = 0.03f)
        )
    } else {
        listOf(
            Color.Black.copy(alpha = 0.12f),
            Color.Black.copy(alpha = 0.05f),
            Color.Black.copy(alpha = 0.08f)
        )
    }
)
