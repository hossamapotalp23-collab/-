package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AccentGold,                  // Gold accents shine bright in dark mode
    secondary = PrimaryEmerald,            // Rich emerald green
    tertiary = GoldAmber,                  // Warm golden sparks
    background = DarkObsidian,             // Rich obsidian-green background
    surface = DarkEmeraldSurface,          // Rich glassy-green surface
    onPrimary = DarkObsidian,              // Dark text on gold button
    onSecondary = Color.White,
    onBackground = Color(0xFFE0EADF),      // Light soft minty text
    onSurface = Color(0xFFF0F5F1),         // Clean cream text on dark surface
    surfaceVariant = DarkEmeraldCard,      // Translucent cards
    onSurfaceVariant = Color(0xFFB0CBB0)   // Subdued green text
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryEmerald,              // Solid deep emerald for core components
    secondary = AccentGold,                // Golden trims and badges
    tertiary = GoldAmber,                  // Warn amber details
    background = PearlWhite,               // Soft warm pearl background
    surface = CardWhite,                   // Clean bright card surfaces
    onPrimary = Color.White,               // White text on green button
    onSecondary = TextDarkGreen,
    onBackground = TextDarkGreen,          // Deep dark forest green for readable text
    onSurface = TextDarkGreen,
    surfaceVariant = LightMint,            // Muted mint highlights
    onSurfaceVariant = PrimaryEmerald      // Green indicators on light background
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
