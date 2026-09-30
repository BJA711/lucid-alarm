package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = VoidBackground,
    primaryContainer = Color(0x3338BDF8),
    onPrimaryContainer = TextPrimaryDark,
    secondary = NeonIndigo,
    onSecondary = VoidBackground,
    secondaryContainer = Color(0x33818CF8),
    onSecondaryContainer = TextPrimaryDark,
    tertiary = NeonEmerald,
    onTertiary = VoidBackground,
    background = VoidBackground,
    onBackground = TextPrimaryDark,
    surface = MidnightBackground,
    onSurface = TextPrimaryDark,
    surfaceVariant = DeepCharcoal,
    onSurfaceVariant = TextSecondaryDark,
    outline = GlassBorderDark,
    outlineVariant = GlassBorderSubtleDark
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = Color(0xFF4F46E5),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEEF2FF),
    onSecondaryContainer = Color(0xFF3730A3),
    tertiary = Color(0xFF059669),
    onTertiary = Color.White,
    background = Color(0xFFF1F5F9),
    onBackground = TextPrimaryLight,
    surface = Color.White,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0)
)

@Composable
fun AlarmClockTheme(
    darkTheme: Boolean = true, // Default to stunning dark glass aesthetic
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GlassTypography,
        content = content
    )
}
