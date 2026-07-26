package com.alarm.meluko.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun shimmerGradient(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "translate"
    )
    return Brush.linearGradient(
        colors = listOf(
            Color(0xFF222226),
            Color(0xFF36363A),
            Color(0xFF222226),
        ),
        start = Offset(translateAnim - 500f, 0f),
        end = Offset(translateAnim, Float.POSITIVE_INFINITY)
    )
}

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp)
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(shimmerGradient())
    )
}

@Composable
fun ShimmerCircle(
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(shimmerGradient())
    )
}

@Composable
fun ShimmerLine(
    width: Dp = 120.dp,
    height: Dp = 14.dp,
    shape: RoundedCornerShape = RoundedCornerShape(4.dp),
    modifier: Modifier = Modifier
) {
    ShimmerBox(
        modifier = modifier
            .width(width)
            .height(height),
        shape = shape
    )
}

@Composable
fun ShimmerCard(
    modifier: Modifier = Modifier,
    height: Dp = 180.dp
) {
    ShimmerBox(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    )
}

@Composable
fun ShimmerLeaderboardRow(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShimmerLine(width = 20.dp, height = 16.dp, shape = RoundedCornerShape(2.dp))
        Spacer(Modifier.width(12.dp))
        ShimmerCircle(size = 38.dp)
        Spacer(Modifier.width(12.dp))
        ShimmerLine(width = 100.dp, height = 14.dp)
        Spacer(Modifier.weight(1f))
        ShimmerLine(width = 48.dp, height = 14.dp)
    }
}

@Composable
fun ShimmerStatCard(modifier: Modifier = Modifier) {
    ShimmerBox(
        modifier = modifier.fillMaxWidth().height(90.dp)
    )
}

@Composable
fun ShimmerTribeChip(modifier: Modifier = Modifier) {
    ShimmerBox(
        modifier = modifier.width(140.dp).height(60.dp),
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun ShimmerSettingsRow(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShimmerCircle(size = 20.dp)
        Spacer(Modifier.width(14.dp))
        ShimmerLine(width = 120.dp, height = 14.dp)
        Spacer(Modifier.weight(1f))
        ShimmerLine(width = 16.dp, height = 16.dp, shape = RoundedCornerShape(2.dp))
    }
}
