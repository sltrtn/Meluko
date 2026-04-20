package com.alarm.meluko.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Note: Google Sans Flex Rounded is a proprietary Google font usually available 
// through Google Fonts or system resources. For this implementation, we will use
// the "sans-serif-rounded" system font which maps to the rounded aesthetic 
// required for Material 3 Expressive when Google Sans Flex isn't bundled.

val ExpressiveFontFamily = FontFamily.SansSerif // We can use Rounded if available or a variable font

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = ExpressiveFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = ExpressiveFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = ExpressiveFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = ExpressiveFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = ExpressiveFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    labelLarge = TextStyle(
        fontFamily = ExpressiveFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )
)