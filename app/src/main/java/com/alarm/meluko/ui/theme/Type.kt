package com.alarm.meluko.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.alarm.meluko.R

// Outfit, the weights Meluko actually uses.
val DefaultFontFamily = FontFamily(
    Font(R.font.outfit_regular,   FontWeight.Normal),
    Font(R.font.outfit_medium,    FontWeight.Medium),
    Font(R.font.outfit_semibold,  FontWeight.SemiBold),
    Font(R.font.outfit_bold,      FontWeight.Bold),
    Font(R.font.outfit_extrabold, FontWeight.ExtraBold),
    Font(R.font.outfit_black,     FontWeight.Black),
)

/** Same family as [DefaultFontFamily]; kept as a name for body copy call sites. */
val BodyFontFamily = DefaultFontFamily

// Tabular figures: every digit is the same width, so clocks, countdowns and
// counters don't jiggle sideways as they tick. Outfit ships the "tnum" feature.
private const val TABULAR = "tnum"

private fun style(
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
    tracking: TextUnit = 0.sp,
) = TextStyle(
    fontFamily = DefaultFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking,
    fontFeatureSettings = TABULAR,
)

/**
 * Meluko type scale. Use these roles instead of hand-picked sizes:
 * display/headline for heroes and screen titles, title for card titles and
 * names, body for copy (Regular), label for buttons/chips/eyebrows.
 * Uppercase eyebrows use labelMedium (tracked out).
 */
val Typography = Typography(
    displayLarge   = style(57, 62, FontWeight.Black, (-0.25).sp),
    displayMedium  = style(45, 50, FontWeight.Black),
    displaySmall   = style(36, 42, FontWeight.Black),

    headlineLarge  = style(32, 36, FontWeight.Black),
    headlineMedium = style(28, 32, FontWeight.Black),
    headlineSmall  = style(24, 30, FontWeight.ExtraBold),

    titleLarge     = style(20, 26, FontWeight.ExtraBold),
    titleMedium    = style(16, 22, FontWeight.Bold),
    titleSmall     = style(14, 20, FontWeight.Bold),

    bodyLarge      = style(16, 24, FontWeight.Normal),
    bodyMedium     = style(14, 20, FontWeight.Normal),
    bodySmall      = style(13, 18, FontWeight.Normal),

    labelLarge     = style(14, 20, FontWeight.ExtraBold, 0.2.sp),
    labelMedium    = style(12, 16, FontWeight.ExtraBold, 1.6.sp),
    labelSmall     = style(12, 16, FontWeight.SemiBold),
)
