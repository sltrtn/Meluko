package com.alarm.meluko.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ── Seed: #FF8C00 Sunrise Amber ───────────────────────────────────────────────
// One seed color drives every role in both schemes.
// Switch to a new seed → regenerate Color.kt → everything updates harmoniously.

private val MelukoLightColors = lightColorScheme(
    primary                = md_light_primary,
    onPrimary              = md_light_onPrimary,
    primaryContainer       = md_light_primaryContainer,
    onPrimaryContainer     = md_light_onPrimaryContainer,
    secondary              = md_light_secondary,
    onSecondary            = md_light_onSecondary,
    secondaryContainer     = md_light_secondaryContainer,
    onSecondaryContainer   = md_light_onSecondaryContainer,
    tertiary               = md_light_tertiary,
    onTertiary             = md_light_onTertiary,
    tertiaryContainer      = md_light_tertiaryContainer,
    onTertiaryContainer    = md_light_onTertiaryContainer,
    error                  = md_light_error,
    onError                = md_light_onError,
    errorContainer         = md_light_errorContainer,
    onErrorContainer       = md_light_onErrorContainer,
    background             = md_light_background,
    onBackground           = md_light_onBackground,
    surface                = md_light_surface,
    onSurface              = md_light_onSurface,
    surfaceVariant         = md_light_surfaceVariant,
    onSurfaceVariant       = md_light_onSurfaceVariant,
    surfaceContainerHigh   = md_light_surfaceContainerHigh,
    outline                = md_light_outline,
    outlineVariant         = md_light_outlineVariant,
    inverseSurface         = md_light_inverseSurface,
    inverseOnSurface       = md_light_inverseOnSurface,
    inversePrimary         = md_light_inversePrimary,
)

private val MelukoDarkColors = darkColorScheme(
    primary                = md_dark_primary,
    onPrimary              = md_dark_onPrimary,
    primaryContainer       = md_dark_primaryContainer,
    onPrimaryContainer     = md_dark_onPrimaryContainer,
    secondary              = md_dark_secondary,
    onSecondary            = md_dark_onSecondary,
    secondaryContainer     = md_dark_secondaryContainer,
    onSecondaryContainer   = md_dark_onSecondaryContainer,
    tertiary               = md_dark_tertiary,
    onTertiary             = md_dark_onTertiary,
    tertiaryContainer      = md_dark_tertiaryContainer,
    onTertiaryContainer    = md_dark_onTertiaryContainer,
    error                  = md_dark_error,
    onError                = md_dark_onError,
    errorContainer         = md_dark_errorContainer,
    onErrorContainer       = md_dark_onErrorContainer,
    background             = Color.Black, // TRUE BLACK
    onBackground           = md_dark_onBackground,
    surface                = Color.Black, // TRUE BLACK
    onSurface              = md_dark_onSurface,
    surfaceVariant         = Color(0xFF121212), // Very dark for cards
    onSurfaceVariant       = md_dark_onSurfaceVariant,
    surfaceContainerHigh   = Color(0xFF1E1E1E), // Slightly lighter for contrast
    outline                = md_dark_outline,
    outlineVariant         = md_dark_outlineVariant,
    inverseSurface         = md_dark_inverseSurface,
    inverseOnSurface       = md_dark_inverseOnSurface,
    inversePrimary         = md_dark_inversePrimary,
)

@Composable
fun MelukoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // Follow system theme setting
    dynamicColor: Boolean = true, // Use wallpaper colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            val dynamicScheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            
            // If we are in dark mode, inject true black backgrounds but keep wallpaper accent colors
            if (darkTheme) {
                dynamicScheme.copy(
                    background = Color.Black,
                    surface = Color.Black,
                    surfaceContainer = Color.Black,
                    surfaceContainerLow = Color.Black,
                    surfaceContainerLowest = Color.Black,
                    surfaceVariant = Color(0xFF121212),
                    surfaceContainerHigh = Color(0xFF1E1E1E)
                )
            } else {
                dynamicScheme
            }
        }
        darkTheme -> MelukoDarkColors
        else -> MelukoLightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}