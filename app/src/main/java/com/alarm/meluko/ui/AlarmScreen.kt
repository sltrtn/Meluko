package com.alarm.meluko.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alarm.meluko.ui.theme.DefaultFontFamily
import com.alarm.meluko.ui.theme.MelukoTheme
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────────────────────────────────────
//  AlarmScreen
//
//  Design intent:
//   • Dark warm gradient background — the moment before sunrise
//   • Two massive time segment cards (amber fill, white Sora text)
//   • Gentle pulse animation on the cards — alive, urgent, not aggressive
//   • SwipeToWake pinned at the bottom — the ONLY action available
//   • No navigation, no back stack — full focus on waking up
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AlarmScreen(
    alarmTime: String = "07:30",
    onDismiss: () -> Unit = {}
) {
    val parts = alarmTime.split(":").let {
        Pair(
            it.getOrElse(0) { "07" },
            it.getOrElse(1) { "30" }
        )
    }

    // Live wall-clock for the day/date label
    val now by produceState(initialValue = Date()) {
        while (true) {
            value = Date()
            kotlinx.coroutines.delay(60_000L)
        }
    }
    val dayLabel = remember(now) {
        SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now)
    }

    // Slow pulse on the time cards — breathing rhythm
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue   = 1f,
        targetValue    = 1.03f,
        animationSpec  = infiniteRepeatable(
            animation  = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // ── Root: dark warm gradient ──────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0.0f to Color(0xFF0D0705),
                    0.4f to Color(0xFF1C0E00),
                    0.8f to Color(0xFF2E1600),
                    1.0f to Color(0xFF3D1E00),
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.height(24.dp))

            // ── Day + date ────────────────────────────────────────────────────
            Text(
                text       = dayLabel,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = DefaultFontFamily,
                color      = Color.White.copy(alpha = 0.55f),
                letterSpacing = 1.sp
            )

            Spacer(Modifier.height(8.dp))

            // ── Headline ──────────────────────────────────────────────────────
            Text(
                text       = "Time to Rise 🌅",
                fontSize   = 22.sp,
                fontWeight = FontWeight.Black,
                fontFamily = DefaultFontFamily,
                color      = Color(0xFFFFB68C) // warm amber glow
            )

            Spacer(Modifier.weight(1f))

            // ── Two giant time cards ──────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(pulse),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AlarmTimeCard(number = parts.first,  modifier = Modifier.weight(1f))
                AlarmTimeCard(number = parts.second, modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))

            // AM label
            Text(
                text       = "AM",
                fontSize   = 28.sp,
                fontWeight = FontWeight.Black,
                fontFamily = DefaultFontFamily,
                color      = Color.White
            )

            Spacer(Modifier.weight(1f))

            // ── Tribe context ─────────────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.White.copy(alpha = 0.10f)
            ) {
                Text(
                    text       = "👀  Your tribe is watching",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = DefaultFontFamily,
                    color      = Color.White.copy(alpha = 0.80f),
                    modifier   = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }

            Spacer(Modifier.height(32.dp))

            // ── SwipeToWake ───────────────────────────────────────────────────
            // Larger than the HomeScreen version — this is the ONLY CTA
            SwipeToWakeButton(
                onWake      = onDismiss,
                trackHeight = 88.dp,
                thumbSize   = 72.dp,
                modifier    = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Time segment card — alarm version
//  Warm amber card, white text, larger than the HomeScreen variant
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AlarmTimeCard(number: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(0.85f)
            .background(
                brush = Brush.linearGradient(
                    listOf(Color(0xFFCC6600), Color(0xFF8C3A00))
                ),
                shape = RoundedCornerShape(32.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = number,
            fontSize   = 96.sp,
            fontWeight = FontWeight.Black,
            fontFamily = DefaultFontFamily,
            color      = Color.White,
            lineHeight = 100.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Preview — force dark so we see the real alarm experience
// ─────────────────────────────────────────────────────────────────────────────
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AlarmScreenPreview() {
    MelukoTheme(darkTheme = true) {
        AlarmScreen(alarmTime = "07:30", onDismiss = {})
    }
}
