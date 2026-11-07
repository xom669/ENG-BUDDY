package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class ThemeMode {
    SYSTEM,
    DARK,
    LIGHT
}

private val GeminiDarkColorScheme = darkColorScheme(
    primary = GeminiBlue,
    onPrimary = Color(0xFF061E49),
    primaryContainer = Color(0xFF1B3A6B),
    onPrimaryContainer = Color(0xFFD3E3FD),
    secondary = GeminiPurple,
    onSecondary = Color(0xFF281344),
    secondaryContainer = Color(0xFF3E275B),
    onSecondaryContainer = Color(0xFFEFDBFF),
    tertiary = GeminiCoral,
    onTertiary = Color.White,
    background = GeminiDarkBackground,
    onBackground = GeminiDarkOnBackground,
    surface = GeminiDarkSurface,
    onSurface = GeminiDarkOnSurface,
    surfaceVariant = GeminiDarkSurfaceVariant,
    onSurfaceVariant = GeminiDarkSecondaryText,
    outline = GeminiDarkOutline,
    error = StatusDangerRed
)

private val GeminiLightColorScheme = lightColorScheme(
    primary = Color(0xFF1B6EF3),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E3FD),
    onPrimaryContainer = Color(0xFF041E49),
    secondary = Color(0xFF7B4EA3),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3E5F5),
    onSecondaryContainer = Color(0xFF2E114D),
    tertiary = GeminiCoral,
    onTertiary = Color.White,
    background = GeminiLightBackground,
    onBackground = GeminiLightOnBackground,
    surface = GeminiLightSurface,
    onSurface = GeminiLightOnSurface,
    surfaceVariant = GeminiLightSurfaceVariant,
    onSurfaceVariant = GeminiLightOnSurfaceVariant,
    outline = GeminiLightOutline,
    error = StatusDangerRed
)

val GeminiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun EngBuddyTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val colorScheme = if (isDark) GeminiDarkColorScheme else GeminiLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = GeminiShapes,
        content = content
    )
}
