package com.alarm.meluko.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ── Meluko brand palette ─────────────────────────────────────────────────────
// Fixed brand colors (no wallpaper dynamic color): every user sees Meluko.
// Accents: Sunrise Amber (primary), Coral (secondary), Dusk Violet (tertiary).
// Dark = "Dawn" (warm near-black), Light = "Morning" (warm cream).

val SunriseAmber = Color(0xFFFF8C00)

internal val MelukoDarkScheme = darkColorScheme(
    primary = Color(0xFFFFB060),
    onPrimary = Color(0xFF3D1D00),
    primaryContainer = SunriseAmber,
    onPrimaryContainer = Color(0xFF2A1100),
    inversePrimary = Color(0xFF9A4A00),

    secondary = Color(0xFFFF9E8A),
    onSecondary = Color(0xFF4A140A),
    secondaryContainer = Color(0xFF6B2A1D),
    onSecondaryContainer = Color(0xFFFFDAD2),

    tertiary = Color(0xFFC7B8FF),
    onTertiary = Color(0xFF2A1C66),
    tertiaryContainer = Color(0xFF3E3290),
    onTertiaryContainer = Color(0xFFE6DEFF),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF7A1A12),
    onErrorContainer = Color(0xFFFFDAD6),

    background = Color(0xFF0C0A09),
    onBackground = Color(0xFFF6EEE8),
    surface = Color(0xFF0C0A09),
    onSurface = Color(0xFFF6EEE8),
    surfaceVariant = Color(0xFF29241F),
    onSurfaceVariant = Color(0xFFBFB2A8),
    surfaceTint = Color(0xFFFFB060),
    surfaceBright = Color(0xFF2F2924),
    surfaceDim = Color(0xFF0C0A09),
    surfaceContainerLowest = Color(0xFF080706),
    surfaceContainerLow = Color(0xFF131110),
    surfaceContainer = Color(0xFF181513),
    surfaceContainerHigh = Color(0xFF201C19),
    surfaceContainerHighest = Color(0xFF29241F),
    inverseSurface = Color(0xFFF6EEE8),
    inverseOnSurface = Color(0xFF221A14),
    outline = Color(0xFF8C7F76),
    outlineVariant = Color(0xFF3A332D),
    scrim = Color(0xFF000000),
)

internal val MelukoLightScheme = lightColorScheme(
    primary = Color(0xFF9A4A00),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = SunriseAmber,
    onPrimaryContainer = Color(0xFF2A1100),
    inversePrimary = Color(0xFFFFB060),

    secondary = Color(0xFFA33A28),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDAD2),
    onSecondaryContainer = Color(0xFF3E0A02),

    tertiary = Color(0xFF5A4BC2),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE6DEFF),
    onTertiaryContainer = Color(0xFF160A5E),

    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    background = Color(0xFFFFF8F3),
    onBackground = Color(0xFF221A14),
    surface = Color(0xFFFFF8F3),
    onSurface = Color(0xFF221A14),
    surfaceVariant = Color(0xFFF3DED0),
    onSurfaceVariant = Color(0xFF5A4A3F),
    surfaceTint = Color(0xFF9A4A00),
    surfaceBright = Color(0xFFFFF8F3),
    surfaceDim = Color(0xFFEAD7C9),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF1E8),
    surfaceContainer = Color(0xFFFCEADE),
    surfaceContainerHigh = Color(0xFFF6E2D4),
    surfaceContainerHighest = Color(0xFFF0D9C9),
    inverseSurface = Color(0xFF382F28),
    inverseOnSurface = Color(0xFFFFEDE2),
    outline = Color(0xFF8E7A6D),
    outlineVariant = Color(0xFFE0CBBD),
    scrim = Color(0xFF000000),
)

/** Meluko-specific tokens that Material's ColorScheme has no slot for. */
@Immutable
data class MelukoColors(
    val awake: Color,
    val sleeping: Color,
    val missed: Color,
    /** Sunrise gradient for hero surfaces, darkest → brightest. */
    val sunrise: List<Color>,
    /** Soft ambient blobs behind content so glass has something to frost. */
    val auroraWarm: Color,
    val auroraCool: Color,
    val glassTint: Color,
    val glassBorder: Color,
    val isDark: Boolean,
)

internal val DarkMelukoColors = MelukoColors(
    awake = Color(0xFFFFC46B),
    sleeping = Color(0xFFB9A7FF),
    missed = Color(0xFFFF8A7A),
    sunrise = listOf(Color(0xFFB34700), Color(0xFFFF7A1A), Color(0xFFFFB347)),
    auroraWarm = Color(0xFFFF8C00),
    auroraCool = Color(0xFF6B4FD8),
    glassTint = Color(0xFF1C1815).copy(alpha = 0.62f),
    glassBorder = Color.White.copy(alpha = 0.10f),
    isDark = true,
)

internal val LightMelukoColors = MelukoColors(
    awake = Color(0xFFB85C00),
    sleeping = Color(0xFF5A4BC2),
    missed = Color(0xFFC0392B),
    sunrise = listOf(Color(0xFFE85D00), Color(0xFFFF8C00), Color(0xFFFFB347)),
    auroraWarm = Color(0xFFFFB066),
    auroraCool = Color(0xFFB9A7FF),
    glassTint = Color(0xFFFFF8F3).copy(alpha = 0.70f),
    glassBorder = Color.White.copy(alpha = 0.65f),
    isDark = false,
)

val LocalMelukoColors = staticCompositionLocalOf { DarkMelukoColors }
