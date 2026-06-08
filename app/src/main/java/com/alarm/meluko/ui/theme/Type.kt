package com.alarm.meluko.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.alarm.meluko.R

/**
 * Display/Title Font: Clash Display
 * Clash Display is incredibly expressive with bold contrast. Perfect for sexy, premium titles on true black backgrounds.
 */
val DefaultFontFamily = FontFamily(
    Font(R.font.clash_display_extralight, FontWeight.ExtraLight),
    Font(R.font.clash_display_light,      FontWeight.Light),
    Font(R.font.clash_display_regular,    FontWeight.Normal),
    Font(R.font.clash_display_medium,     FontWeight.Medium),
    Font(R.font.clash_display_semibold,   FontWeight.SemiBold),
    Font(R.font.clash_display_bold,       FontWeight.Bold),
    Font(R.font.clash_display_bold,       FontWeight.ExtraBold),
    Font(R.font.clash_display_bold,       FontWeight.Black),
)

/**
 * Body/Text Font: Clash Display (All-in on Brand)
 * The user requested to shift completely to Clash Display. 
 * We map out the weights so that captions aren't overly bold and remain legible.
 */
val BodyFontFamily = FontFamily(
    Font(R.font.clash_display_extralight, FontWeight.ExtraLight),
    Font(R.font.clash_display_light,      FontWeight.Light),
    Font(R.font.clash_display_regular,    FontWeight.Normal),
    Font(R.font.clash_display_medium,     FontWeight.Medium),
    Font(R.font.clash_display_semibold,   FontWeight.SemiBold),
    Font(R.font.clash_display_bold,       FontWeight.Bold),
    Font(R.font.clash_display_bold,       FontWeight.ExtraBold),
    Font(R.font.clash_display_bold,       FontWeight.Black),
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily    = DefaultFontFamily,
        fontWeight    = FontWeight.Black,
        fontSize      = 57.sp,
        lineHeight    = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily    = DefaultFontFamily,
        fontWeight    = FontWeight.Black,
        fontSize      = 45.sp,
        lineHeight    = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily    = DefaultFontFamily,
        fontWeight    = FontWeight.ExtraBold,
        fontSize      = 36.sp,
        lineHeight    = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily    = DefaultFontFamily,
        fontWeight    = FontWeight.ExtraBold,
        fontSize      = 32.sp,
        lineHeight    = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily    = DefaultFontFamily,
        fontWeight    = FontWeight.ExtraBold,
        fontSize      = 28.sp,
        lineHeight    = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily    = DefaultFontFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 24.sp,
        lineHeight    = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily    = DefaultFontFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 22.sp,
        lineHeight    = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily    = DefaultFontFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 16.sp,
        lineHeight    = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily    = DefaultFontFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 14.sp,
        lineHeight    = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily    = BodyFontFamily,
        fontWeight    = FontWeight.ExtraBold,
        fontSize      = 16.sp,
        lineHeight    = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily    = BodyFontFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 14.sp,
        lineHeight    = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily    = BodyFontFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 12.sp,
        lineHeight    = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily    = BodyFontFamily,
        fontWeight    = FontWeight.ExtraBold,
        fontSize      = 14.sp,
        lineHeight    = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily    = BodyFontFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 12.sp,
        lineHeight    = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily    = BodyFontFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 11.sp,
        lineHeight    = 16.sp,
        letterSpacing = 0.5.sp
    )
)