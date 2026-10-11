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
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
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
import kotlinx.coroutines.flow.first
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
    trackHeight: Dp = 80.dp,
    reduceMotion: Boolean = false
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
    val glowingAmber = Color(0xFFFFC94A)
    val paleGold = Color(0xFFFFF1C9)
    val deepBurntOrange = Color(0xFF6F3000)

    // ── Shimmer: only runs when label is visible ──────────────────────────
    val labelAlpha = (1f - progress * 3f).coerceIn(0f, 1f)
    val shimmerVisible = labelAlpha > 0.01f && !reduceMotion
    val shimmerTranslate = remember { Animatable(-200f) }
    LaunchedEffect(shimmerVisible) {
        if (shimmerVisible) {
            while (true) {
                shimmerTranslate.animateTo(700f, tween(2200, easing = LinearEasing))
                shimmerTranslate.snapTo(-200f)
            }
        }
    }

    // ── Idle nudge: teaches the gesture if nobody touches it ───────────────
    // Visual-only offset on the thumb; never feeds into progress/threshold.
    val nudge = remember { Animatable(0f) }
    val nudgeDistance = with(density) { 22.dp.toPx() }
    LaunchedEffect(isDragging, reduceMotion) {
        nudge.snapTo(0f)
        if (isDragging || reduceMotion) return@LaunchedEffect
        while (true) {
            delay(2600)
            if (springOffset.value == 0f) {
                nudge.animateTo(nudgeDistance, tween(260, easing = FastOutSlowInEasing))
                nudge.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = 500f))
            }
        }
    }

    // ── Sun rays rotate slowly ──────────────────────────────────────────────
    val rayRotation = if (reduceMotion) 0f else {
        rememberInfiniteTransition(label = "rays").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(14_000, easing = LinearEasing)),
            label = "rayRotation"
        ).value
    }
    val liftPx = with(density) { 4.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(trackHeight)
            .onSizeChanged { trackWidth = it.width }
            .pointerInput(Unit) {
                if (maxDrag <= 0f) {
                    snapshotFlow { maxDrag }.first { it > 0f }
                }
                detectHorizontalDragGestures(
                    onDragStart = {
                        isDragging = true
                        scope.launch { springOffset.snapTo(springOffset.value) }
                    },
                    onDragEnd = {
                        isDragging = false
                        val finalDrag = dragOffset
                        scope.launch {
                            if (finalDrag >= threshold) {
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
        // ── Track: a horizon that brightens as the sun is dragged across ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.07f), Color.White.copy(alpha = 0.03f))
                    )
                )
                .drawBehind {
                    val fillWidth = (displayOffset + thumbSizePx + thumbPaddingPx * 2)
                        .coerceAtMost(size.width)
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                deepBurntOrange.copy(alpha = 0.35f + 0.3f * progress),
                                sunriseOrange.copy(alpha = 0.25f + 0.5f * progress),
                                glowingAmber.copy(alpha = 0.15f + 0.6f * progress)
                            ),
                            endX = fillWidth
                        ),
                        size = Size(fillWidth, size.height),
                        cornerRadius = CornerRadius(size.height / 2f)
                    )
                    // Soft top-edge highlight, like light catching the horizon
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.10f + 0.10f * progress), Color.Transparent),
                            endY = size.height * 0.5f
                        ),
                        size = Size(size.width, size.height * 0.5f)
                    )
                }
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.02f))
                    ),
                    shape = RoundedCornerShape(50)
                )
        )

        // ── Label + chevron hints ──────────────────────────────────────────
        val chevronPhase = if (reduceMotion) 0f else shimmerTranslate.value
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(start = thumbSize * 0.6f)
                .graphicsLayer { alpha = labelAlpha },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Slide to rise",
                    color = Color.White.copy(alpha = 0.35f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = DefaultFontFamily,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Slide to rise",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = DefaultFontFamily,
                    letterSpacing = 1.5.sp,
                    style = TextStyle(
                        brush = Brush.linearGradient(
                            colors = listOf(Color.Transparent, paleGold.copy(alpha = 0.9f), Color.Transparent),
                            start = Offset(shimmerTranslate.value - 200f, 0f),
                            end = Offset(shimmerTranslate.value - 80f, 0f)
                        )
                    )
                )
            }
            Spacer(Modifier.width(6.dp))
            repeat(3) { i ->
                // Each chevron brightens in sequence, a wave moving right.
                val wave = ((chevronPhase / 900f) * 3f - i).let { 1f - kotlin.math.abs(it - 1f) }.coerceIn(0f, 1f)
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.18f + 0.5f * wave),
                    modifier = Modifier
                        .size(18.dp)
                        .offset(x = (-8 * i).dp)
                )
            }
        }

        // ── Thumb: the sun ─────────────────────────────────────────────────
        val thumbScale by animateFloatAsState(
            targetValue = if (isNearEnd) 1.12f else 1f,
            animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
            label = "thumbScale"
        )

        Box(
            modifier = Modifier
                .padding(start = thumbPaddingDp)
                .align(Alignment.CenterStart)
                .offset { IntOffset((displayOffset + nudge.value).toInt(), 0) }
                .size(thumbSize)
                .graphicsLayer {
                    scaleX = thumbScale
                    scaleY = thumbScale
                    // The sun lifts slightly as it "rises" across the track.
                    translationY = -liftPx * progress
                }
                .drawBehind {
                    val r = size.minDimension / 2f
                    val auraAlpha = 0.25f + progress * 0.45f
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(glowingAmber.copy(alpha = auraAlpha * 0.45f), Color.Transparent),
                            radius = r + 34.dp.toPx()
                        ),
                        radius = r + 34.dp.toPx()
                    )
                    // Rays: grow longer and brighter as progress increases
                    val rays = 12
                    val inner = r + 4.dp.toPx()
                    val outer = r + (8.dp.toPx() + 10.dp.toPx() * progress)
                    rotate(rayRotation) {
                        repeat(rays) { i ->
                            rotate(i * 360f / rays) {
                                drawLine(
                                    color = (if (isNearEnd) paleGold else glowingAmber)
                                        .copy(alpha = 0.35f + 0.5f * progress),
                                    start = Offset(center.x, center.y - inner),
                                    end = Offset(center.x, center.y - outer),
                                    strokeWidth = 3.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }
                }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        if (isNearEnd) listOf(Color.White, paleGold)
                        else listOf(paleGold, glowingAmber, sunriseOrange)
                    )
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
                if (reachedEnd) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = deepBurntOrange
                    )
                } else {
                    Box(Modifier.size(32.dp))
                }
            }
        }
    }
}

private suspend fun delay(timeMillis: Long) {
    kotlinx.coroutines.delay(timeMillis)
}
