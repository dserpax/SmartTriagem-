package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Color(0xFF003642),
    primaryContainer = Color(0xFF004E5F),
    onPrimaryContainer = Color(0xFFB5EAFF),

    secondary = TechIndigoLight,
    onSecondary = Color(0xFF1E1E60),
    secondaryContainer = Color(0xFF37378E),
    onSecondaryContainer = Color(0xFFE0E0FF),

    tertiary = EmeraldSuccess,
    onTertiary = Color(0xFF003822),
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = Color(0xFF6DF7B8),

    background = TechNavyDark,
    onBackground = Color(0xFFE2E8F0),
    surface = TechSurfaceDark,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = TechSurfaceContainerDark,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155),
    error = RoseCritical,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = TechPrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCBE6FF),
    onPrimaryContainer = Color(0xFF001E30),

    secondary = TechIndigo,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E0FF),
    onSecondaryContainer = Color(0xFF190065),

    tertiary = EmeraldSuccess,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFA7F3D0),
    onTertiaryContainer = Color(0xFF064E3B),

    background = TechWhite,
    onBackground = TechNavyLight,
    surface = TechSurfaceLight,
    onSurface = TechNavyLight,
    surfaceVariant = TechSurfaceContainerLight,
    onSurfaceVariant = TechSecondaryTextLight,
    outline = TechBorderLight,
    error = RoseCritical,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
