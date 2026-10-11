package com.alarm.meluko.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.dp

// Expressive shape scale: rounder and bolder than baseline M3.
private val MelukoShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun MelukoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalMelukoColors provides if (darkTheme) DarkMelukoColors else LightMelukoColors
    ) {
        MaterialExpressiveTheme(
            colorScheme = if (darkTheme) MelukoDarkScheme else MelukoLightScheme,
            motionScheme = MotionScheme.expressive(),
            shapes = MelukoShapes,
            typography = Typography,
            content = content
        )
    }
}

/** Access Meluko's extra semantic colors: `MelukoTheme.colors.awake`. */
object MelukoTheme {
    val colors: MelukoColors
        @Composable @ReadOnlyComposable get() = LocalMelukoColors.current
}
