package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EmeraldColorScheme = darkColorScheme(
    primary = Color(0xFF0F8F6B),           // Premium bright Emerald Green
    secondary = Color(0xFF063B35),         // Deep dark green
    tertiary = Color(0xFFD4AF37),          // Luxurious soft gold
    background = Color(0xFF040A07),        // Deep black with soft green undertone
    surface = Color(0xFF0A1813),           // Semi-transparent glassy dark surface
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFFF0F5F1),      // Light soft text
    onSurface = Color(0xFFFFFFFF),         // Pure white text
    surfaceVariant = Color(0xFF102820),    // Glass effect card background
    onSurfaceVariant = Color(0xFFB0CBB0)   // Subdued green text
)

private val IndigoColorScheme = darkColorScheme(
    primary = Color(0xFF3B82F6),           // Royal Celestial Blue
    secondary = Color(0xFF1E3A8A),         // Deep Navy Blue
    tertiary = Color(0xFFD4AF37),          // Luxurious gold
    background = Color(0xFF030712),        // Deep space blue/black
    surface = Color(0xFF0F172A),           // Dark slate glassy surface
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFFE2E8F0),      // Subdued white text
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1E293B),    // Slate card background
    onSurfaceVariant = Color(0xFF94A3B8)   // Subdued blue gray
)

private val SandColorScheme = lightColorScheme(
    primary = Color(0xFF8D6E63),           // Warm Bronze/Brown
    secondary = Color(0xFFD7CCC8),         // Light sandy beige
    tertiary = Color(0xFFBF360C),          // Deep rich terracotta/gold
    background = Color(0xFFFAF6EE),        // Warm desert sand white
    surface = Color(0xFFF4EBE1),           // Warm ivory cream surface
    onPrimary = Color.White,
    onSecondary = Color(0xFF3E2723),
    onBackground = Color(0xFF3E2723),      // Dark brown body text
    onSurface = Color(0xFF3E2723),         // Dark brown surface text
    surfaceVariant = Color(0xFFEDE0D4),    // Warm sand card
    onSurfaceVariant = Color(0xFF704F37)   // Subdued medium brown
)

@Composable
fun MyApplicationTheme(
    themeName: String = "Emerald",
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeName) {
        "Indigo" -> IndigoColorScheme
        "Sand" -> SandColorScheme
        else -> EmeraldColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
