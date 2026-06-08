package com.alarm.meluko.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alarm.meluko.ui.theme.MelukoTheme
import com.alarm.meluko.ui.theme.DefaultFontFamily
import kotlinx.coroutines.launch

@Preview(showBackground = true)
@Composable
private fun SwipeToWakePreview() {
    MelukoTheme(darkTheme = true) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xFF1C0E00)) // Warm dark background like AlarmScreen
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            SwipeToWakeButton(onWake = {})
        }
    }
}

/**
 * A ultra-premium Swipe-to-Wake button designed to adapt to Meluko's warm amber aesthetic.
 */
@Composable
fun SwipeToWakeButton(
    onWake: () -> Unit,
    modifier: Modifier = Modifier,
    thumbSize: Dp = 68.dp,
    trackHeight: Dp = 80.dp
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    var trackWidth by remember { mutableIntStateOf(0) }

    val density = LocalDensity.current
    val thumbSizePx = with(density) { thumbSize.toPx() }
    val trackHeightPx = with(density) { trackHeight.toPx() }
    val thumbPaddingPx = (trackHeightPx - thumbSizePx) / 2f
    val thumbPaddingDp = with(density) { thumbPaddingPx.toDp() }

    val maxDrag = (trackWidth - thumbSizePx - (thumbPaddingPx * 2)).coerceAtLeast(0f)
    val progress = if (maxDrag > 0f) (offsetX.value / maxDrag).coerceIn(0f, 1f) else 0f
    val threshold = maxDrag * 0.85f
    val isNearEnd = progress > 0.85f

    val springSpec = spring<Float>(dampingRatio = 0.75f, stiffness = 300f)

    // Vibrant Sunrise Palette
    val sunriseOrange = Color(0xFFFF8C00)
    val glowingAmber = Color(0xFFFFD700)
    val deepBurntOrange = Color(0xFF6F3000)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(trackHeight)
            .onSizeChanged { trackWidth = it.width }
            .pointerInput(maxDrag) {
                if (maxDrag <= 0f) return@pointerInput
                detectHorizontalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (offsetX.value >= threshold) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                offsetX.animateTo(maxDrag, springSpec)
                                onWake()
                                delay(300)
                                offsetX.snapTo(0f)
                            } else {
                                offsetX.animateTo(0f, springSpec)
                            }
                        }
                    },
                    onHorizontalDrag = { _, delta ->
                        scope.launch {
                            val oldValue = offsetX.value
                            val newValue = (oldValue + delta).coerceIn(0f, maxDrag)
                            offsetX.snapTo(newValue)
                            if (newValue >= threshold && oldValue < threshold) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    }
                )
            }
    ) {
        // ── Track: Glassmorphism ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.05f))
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.15f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(50)
                )
        )

        // ── Progress Fill: Warm Glow ────────────────────────────────────────
        val fillWidth = with(density) { (offsetX.value + thumbSizePx + thumbPaddingPx).toDp() }
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(fillWidth)
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            sunriseOrange.copy(alpha = 0.1f),
                            sunriseOrange.copy(alpha = 0.4f * progress)
                        )
                    )
                )
        )

        // ── Label: Shimmering Text ───────────────────────────────────────────
        val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
        val shimmerTranslate by infiniteTransition.animateFloat(
            initialValue = -200f,
            targetValue = 600f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmerTranslate"
        )

        val labelAlpha = (1f - progress * 3f).coerceIn(0f, 1f)
        if (labelAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer { alpha = labelAlpha },
                contentAlignment = Alignment.Center
            ) {
                // Base text
                Text(
                    text = "Slide to Wake Up",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = DefaultFontFamily,
                    letterSpacing = 1.sp
                )
                // Shimmering overlay text
                Text(
                    text = "Slide to Wake Up",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = DefaultFontFamily,
                    letterSpacing = 1.sp,
                    style = TextStyle(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent, 
                                Color.White.copy(alpha = 0.6f), 
                                Color.Transparent
                            ),
                            start = androidx.compose.ui.geometry.Offset(shimmerTranslate, 0f),
                            end = androidx.compose.ui.geometry.Offset(shimmerTranslate + 100f, 0f)
                        )
                    )
                )
            }
        }

        // ── Thumb: The "Sun" ────────────────────────────────────────────────
        val thumbScale by animateFloatAsState(
            targetValue = if (isNearEnd) 1.15f else 1f,
            animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
            label = "thumbScale"
        )

        Box(
            modifier = Modifier
                .padding(start = thumbPaddingDp)
                .align(Alignment.CenterStart)
                .offset { IntOffset(offsetX.value.toInt(), 0) }
                .size(thumbSize)
                .graphicsLayer {
                    scaleX = thumbScale
                    scaleY = thumbScale
                }
                .drawBehind {
                    // Reactive Sun Glow
                    val glowColor = if (isNearEnd) Color.White else glowingAmber
                    val auraAlpha = (0.2f + (progress * 0.4f)).coerceIn(0f, 1f)
                    
                    drawCircle(
                        color = glowColor.copy(alpha = auraAlpha * 0.15f),
                        radius = size.minDimension / 2f + 35.dp.toPx()
                    )
                    drawCircle(
                        color = glowColor.copy(alpha = auraAlpha * 0.35f),
                        radius = size.minDimension / 2f + 15.dp.toPx()
                    )
                }
                .clip(CircleShape)
                .background(
                    if (isNearEnd) {
                        Brush.radialGradient(listOf(Color.White, Color.White))
                    } else {
                        Brush.radialGradient(listOf(glowingAmber, sunriseOrange))
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = isNearEnd,
                transitionSpec = {
                    (scaleIn(tween(200)) + fadeIn(tween(200))).togetherWith(
                        scaleOut(tween(200)) + fadeOut(tween(200))
                    )
                },
                label = "iconTransition"
            ) { reachedEnd ->
                Icon(
                    imageVector = if (reachedEnd) Icons.Rounded.Check else Icons.Rounded.WbSunny,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = if (reachedEnd) deepBurntOrange else Color.White
                )
            }
        }
    }
}

private suspend fun delay(timeMillis: Long) {
    kotlinx.coroutines.delay(timeMillis)
}
