package com.alarm.meluko.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alarm.meluko.ui.theme.MelukoTheme
import com.alarm.meluko.ui.theme.DefaultFontFamily
import com.alarm.meluko.ui.theme.BodyFontFamily
import kotlinx.coroutines.launch

@Preview(showBackground = true)
@Composable
private fun SwipeToWakePreview() {
    MelukoTheme {
        Box(Modifier.padding(20.dp)) {
            SwipeToWakeButton(onWake = {})
        }
    }
}

@Composable
fun SwipeToWakeButton(
    onWake: () -> Unit,
    modifier: Modifier = Modifier,
    thumbSize: Dp  = 68.dp,
    trackHeight: Dp = 80.dp
) {
    val offsetX    = remember { Animatable(0f) }
    var trackWidth by remember { mutableIntStateOf(0) }

    val density     = LocalDensity.current
    val thumbSizePx = with(density) { thumbSize.toPx() }
    val maxDrag     = (trackWidth - thumbSizePx).coerceAtLeast(0f)
    val threshold   = maxDrag * 0.85f

    // Spring physics per Meluko engineering spec
    val springSpec = spring<Float>(dampingRatio = 0.6f, stiffness = 400f)
    val scope      = rememberCoroutineScope()

    val progress = if (maxDrag > 0f) (offsetX.value / maxDrag).coerceIn(0f, 1f) else 0f

    // Thumb vertical centering offset (so it sits in the middle of the track)
    val trackHeightPx = with(density) { trackHeight.toPx() }
    val thumbPaddingPx = (trackHeightPx - thumbSizePx) / 2f
    val thumbPaddingDp = with(density) { thumbPaddingPx.toDp() }

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
                                offsetX.animateTo(maxDrag, springSpec)
                                onWake()
                                offsetX.snapTo(0f)
                            } else {
                                offsetX.animateTo(0f, springSpec)
                            }
                        }
                    },
                    onHorizontalDrag = { _, delta ->
                        scope.launch {
                            offsetX.snapTo((offsetX.value + delta).coerceIn(0f, maxDrag))
                        }
                    }
                )
            }
    ) {

        // ── Track — M3 tonal surface with outline ──────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                .border(
                    width  = 2.dp,
                    color  = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                    shape  = RoundedCornerShape(50)
                )
        )

        // ── Animated fill — grows left to right ───────────────────────────────
        val fillWidth = with(density) { (offsetX.value + thumbSizePx + thumbPaddingPx).toDp() }
        Box(
            modifier = Modifier
                .height(trackHeight)
                .width(fillWidth)
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f + progress * 0.2f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f + progress * 0.2f)
                        )
                    )
                )
        )

        // ── Label — fades as progress increases ───────────────────────────────
        val labelAlpha = (1f - progress * 2.2f).coerceIn(0f, 1f)
        if (labelAlpha > 0.01f) {
            Box(
                modifier         = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = "Slide to Wake Up  ›",
                    color      = MaterialTheme.colorScheme.primary.copy(alpha = labelAlpha),
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = DefaultFontFamily,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // ── Thumb — large M3 circle with glow ─────────────────────────────────
        val thumbOffsetX = with(density) { offsetX.value.toDp() }
        val glowAlpha    = progress * 0.65f

        Box(
            modifier = Modifier
                .padding(start = thumbPaddingDp + thumbOffsetX, top = thumbPaddingDp)
                .size(thumbSize)
                .drawBehind {
                    // Outer glow (grows as thumb moves right)
                    drawCircle(
                        color  = Color(0xFF6B00F5).copy(alpha = glowAlpha * 0.2f),
                        radius = size.minDimension / 2f + 18.dp.toPx()
                    )
                    // Inner glow (sharper)
                    drawCircle(
                        color  = Color(0xFF6B00F5).copy(alpha = glowAlpha * 0.4f),
                        radius = size.minDimension / 2f + 8.dp.toPx()
                    )
                }
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = if (progress > 0.85f) "✓" else "☀",
                fontSize   = 28.sp,
                color      = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Black
            )
        }
    }
}
