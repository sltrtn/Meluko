package com.alarm.meluko.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alarm.meluko.ui.theme.DefaultFontFamily
import kotlinx.coroutines.delay

/**
 * WelcomeBackScreen — shown for ~1.5 s between login and HOME.
 *
 * Animation timeline:
 *  0 ms  — screen is invisible (alpha 0)
 *  0 ms  — [contentVisible = true] triggers: "Welcome back," fades in (600 ms)
 * 120 ms — name slides in from bottom (offset 0→24 dp reversed) + fades in (500 ms, delayed 120 ms)
 * 1200 ms — onFinish() called → NavGraph fade-out starts (400 ms)
 * ≈1600 ms — HOME fades in, user lands
 *
 * The 1.2 s call + 400 ms nav transition ≈ 1.6 s total visible time, indistinguishable
 * from the old 1.5 s hard-cut but dramatically smoother.
 */
@Composable
fun WelcomeBackScreen(
    displayName: String?,
    onFinish: () -> Unit
) {
    val name = displayName?.split(" ")?.firstOrNull() ?: "there"

    // ── Animation state ──────────────────────────────────────────────────────
    // Phase 1: overall content fade-in (triggered immediately on first frame)
    var contentVisible by remember { mutableStateOf(false) }
    val screenAlpha by animateFloatAsState(
        targetValue  = if (contentVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label        = "screenAlpha"
    )

    // Phase 2: name slides up from below + staggered fade
    var nameVisible by remember { mutableStateOf(false) }
    val nameAlpha by animateFloatAsState(
        targetValue  = if (nameVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label        = "nameAlpha"
    )
    val nameOffsetDp by animateFloatAsState(
        targetValue  = if (nameVisible) 0f else 24f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label        = "nameOffset"
    )

    // ── Side effects ─────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        // Kick off screen fade immediately
        contentVisible = true
        // Stagger the name entrance by 120 ms so it feels layered
        delay(120)
        nameVisible = true
        // Wait until 1.2 s total, then hand off — NavGraph fade covers the rest
        delay(1_200 - 120)
        onFinish()
    }

    // ── UI ───────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .alpha(screenAlpha),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // "Welcome back," — fades in with the screen
            Text(
                text       = "Welcome back,",
                fontSize   = 22.sp,
                fontFamily = DefaultFontFamily,
                fontWeight = FontWeight.Medium,
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign  = TextAlign.Center
            )

            Spacer(Modifier.height(10.dp))

            // Name — staggered slide-up + fade-in
            Box(
                modifier = Modifier
                    .offset(y = nameOffsetDp.dp)
                    .alpha(nameAlpha)
            ) {
                Text(
                    text       = name,
                    fontSize   = 52.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = DefaultFontFamily,
                    color      = MaterialTheme.colorScheme.primary,
                    textAlign  = TextAlign.Center
                )
            }
        }
    }
}
