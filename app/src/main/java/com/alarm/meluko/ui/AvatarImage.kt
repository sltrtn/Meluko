package com.alarm.meluko.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.alarm.meluko.ui.theme.DefaultFontFamily

// ─────────────────────────────────────────────────────────────────────────────
//  Placeholder colour palette
//  Deterministic — same name always maps to the same colour across all devices.
//  Palette is warm and harmonises with the sunrise amber theme.
// ─────────────────────────────────────────────────────────────────────────────
private val tribePalette = listOf(
    Color(0xFFE8A87C), // warm peach
    Color(0xFF7EC8A8), // morning teal
    Color(0xFFA87CE8), // soft lavender
    Color(0xFFE8C87C), // golden
    Color(0xFF7CA8E8), // sky blue
    Color(0xFFE87CA8), // rose
)

fun tribePlaceholderColor(name: String): Color =
    tribePalette[Math.abs(name.hashCode()) % tribePalette.size]

// ─────────────────────────────────────────────────────────────────────────────
//  UserAvatar — circular, for people
//
//  Shows the real photo (via Coil AsyncImage) when photoUrl is set.
//  Falls back to an initial-letter circle using M3 surfaceVariant tokens.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun UserAvatar(
    photoUrl:  String?,
    name:      String,
    size:      Dp,
    modifier:  Modifier = Modifier
) {
    if (!photoUrl.isNullOrEmpty()) {
        AsyncImage(
            model              = photoUrl,
            contentDescription = name,
            contentScale       = ContentScale.Crop,
            modifier           = modifier
                .size(size)
                .clip(CircleShape)
        )
    } else {
        Box(
            modifier         = modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                fontSize   = (size.value * 0.38f).sp,
                fontWeight = FontWeight.Black,
                fontFamily = DefaultFontFamily,
                color      = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  TribeAvatar — rounded-square, for groups / circles
//
//  Shape (30% corner radius ≈ iOS app icon squircle) differentiates group
//  identity from personal identity at a glance.
//  Placeholder uses a deterministic colour from the warm palette so the same
//  tribe always shows the same colour on every device.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun TribeAvatar(
    photoUrl:  String?,
    name:      String,
    size:      Dp,
    modifier:  Modifier = Modifier
) {
    val shape            = RoundedCornerShape(30)           // squircle-style
    val placeholderColor = tribePlaceholderColor(name)

    if (!photoUrl.isNullOrEmpty()) {
        AsyncImage(
            model              = photoUrl,
            contentDescription = name,
            contentScale       = ContentScale.Crop,
            modifier           = modifier
                .size(size)
                .clip(shape)
        )
    } else {
        Box(
            modifier         = modifier
                .size(size)
                .clip(shape)
                .background(placeholderColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                fontSize   = (size.value * 0.38f).sp,
                fontWeight = FontWeight.Black,
                fontFamily = DefaultFontFamily,
                color      = Color.White
            )
        }
    }
}
