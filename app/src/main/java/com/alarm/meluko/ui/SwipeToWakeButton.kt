package com.alarm.meluko.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import com.alarm.meluko.ui.theme.DefaultFontFamily
import com.alarm.meluko.ui.theme.MelukoTheme
import kotlinx.coroutines.launch

@Preview(showBackground = true)
@Composable
private fun SwipeToWakePreview() {
    MelukoTheme(darkTheme = true) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xFF1C0E00))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            SwipeToWakeButton(onWake = {})
        }
    }
}

@Composable
fun SwipeToWakeButton(
    onWake: () -> Unit,
    modifier: Modifier = Modifier,
    thumbSize: Dp = 68.dp,
    trackHeight: Dp = 80.dp
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var trackWidth by remember { mutableIntStateOf(0) }

    val density = LocalDensity.current
    val thumbSizePx = with(density) { thumbSize.toPx() }
    val trackHeightPx = with(density) { trackHeight.toPx() }
    val thumbPaddingPx = (trackHeightPx - thumbSizePx) / 2f
    val thumbPaddingDp = with(density) { thumbPaddingPx.toDp() }

    val maxDrag = (trackWidth - thumbSizePx - (thumbPaddingPx * 2)).coerceAtLeast(0f)
    val threshold = maxDrag * 0.85f

    // Raw drag offset — no coroutine-per-event overhead
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val springOffset = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val displayOffset = if (isDragging) dragOffset else springOffset.value
    val progress = if (maxDrag > 0f) (displayOffset / maxDrag).coerceIn(0f, 1f) else 0f
    val isNearEnd = progress > 0.85f

    val springSpec = spring<Float>(dampingRatio = 0.75f, stiffness = 300f)

    val sunriseOrange = Color(0xFFFF8C00)
    val glowingAmber = Color(0xFFFFD700)
    val deepBurntOrange = Color(0xFF6F3000)

    // ── Shimmer: only runs when label is visible ──────────────────────────
    val labelAlpha = (1f - progress * 3f).coerceIn(0f, 1f)
    val shimmerVisible = labelAlpha > 0.01f
    val shimmerTranslate = remember { Animatable(-200f) }
    LaunchedEffect(shimmerVisible) {
        if (shimmerVisible) {
            while (true) {
                shimmerTranslate.animateTo(600f, tween(2000, easing = LinearEasing))
                shimmerTranslate.snapTo(-200f)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(trackHeight)
            .onSizeChanged { trackWidth = it.width }
            .pointerInput(maxDrag) {
                if (maxDrag <= 0f) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = {
                        isDragging = true
                        scope.launch { springOffset.snapTo(springOffset.value) }
                    },
                    onDragEnd = {
                        isDragging = false
                        scope.launch {
                            if (springOffset.value >= threshold) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                springOffset.animateTo(maxDrag, springSpec)
                                onWake()
                                delay(300)
                                springOffset.snapTo(0f)
                                dragOffset = 0f
                            } else {
                                springOffset.animateTo(0f, springSpec)
                                dragOffset = 0f
                            }
                        }
                    },
                    onHorizontalDrag = { _, delta ->
                        dragOffset = (dragOffset + delta).coerceIn(0f, maxDrag)
                        scope.launch { springOffset.snapTo(dragOffset) }
                        if (dragOffset >= threshold && dragOffset - delta < threshold) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }
                )
            }
    ) {
        // ── Track + Progress Fill ─────────────────────────────────────────
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
                .drawBehind {
                    val fillWidth = (displayOffset + thumbSizePx + thumbPaddingPx)
                        .coerceAtMost(size.width)
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                sunriseOrange.copy(alpha = 0.1f),
                                sunriseOrange.copy(alpha = 0.4f * progress)
                            )
                        ),
                        size = Size(fillWidth, size.height)
                    )
                }
        )

        // ── Label: Shimmering Text ─────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer { alpha = labelAlpha },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Slide to Wake Up",
                color = Color.White.copy(alpha = 0.3f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = DefaultFontFamily,
                letterSpacing = 1.sp
            )
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
                        start = Offset(shimmerTranslate.value, 0f),
                        end = Offset(shimmerTranslate.value + 100f, 0f)
                    )
                )
            )
        }

        // ── Thumb: The "Sun" ──────────────────────────────────────────────
        val thumbScale by animateFloatAsState(
            targetValue = if (isNearEnd) 1.15f else 1f,
            animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
            label = "thumbScale"
        )

        Box(
            modifier = Modifier
                .padding(start = thumbPaddingDp)
                .align(Alignment.CenterStart)
                .offset { IntOffset(displayOffset.toInt(), 0) }
                .size(thumbSize)
                .graphicsLayer {
                    scaleX = thumbScale
                    scaleY = thumbScale
                }
                .drawBehind {
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
