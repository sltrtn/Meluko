package com.alarm.meluko.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.RoundedPolygon
import com.alarm.meluko.ui.WakeStatus
import com.alarm.meluko.ui.pressScale
import com.alarm.meluko.ui.theme.BodyFontFamily
import com.alarm.meluko.ui.theme.DefaultFontFamily
import com.alarm.meluko.ui.theme.MelukoTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
//  Glass
//  One HazeState per screen shell. Content marks itself `hazeSource`, and the
//  few floating surfaces (nav bar, sticky headers) frost what scrolls beneath.
// ─────────────────────────────────────────────────────────────────────────────

val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

/**
 * Frosted-glass surface. Uses real backdrop blur when a [HazeState] is
 * available (Android 12+), otherwise falls back to a translucent tint, so it
 * always reads as glass without hard-requiring blur support.
 */
@Composable
fun Modifier.glass(
    shape: Shape,
    hazeState: HazeState? = LocalHazeState.current,
    tint: Color = MelukoTheme.colors.glassTint,
    border: Color = MelukoTheme.colors.glassBorder,
    blurRadius: Dp = 28.dp,
): Modifier {
    val base = this
        .clip(shape)
        .border(1.dp, border, shape)
    return if (hazeState != null) {
        base.hazeEffect(
            state = hazeState,
            style = HazeStyle(
                backgroundColor = MaterialTheme.colorScheme.surface,
                tints = listOf(HazeTint(tint)),
                blurRadius = blurRadius,
                noiseFactor = 0.08f,
                fallbackTint = HazeTint(tint.copy(alpha = 0.92f)),
            )
        )
    } else {
        base.background(tint.copy(alpha = (tint.alpha + 0.25f).coerceAtMost(0.96f)))
    }
}

/**
 * Soft sunrise/dusk light behind screen content. It's what makes the glass
 * read as glass — a frosted pane over a flat colour just looks grey.
 */
fun Modifier.aurora(): Modifier = composed {
    val c = MelukoTheme.colors
    val strength = if (c.isDark) 0.22f else 0.30f
    this.drawBehind {
        drawCircle(
            brush = Brush.radialGradient(
                listOf(c.auroraWarm.copy(alpha = strength), Color.Transparent),
                center = Offset(size.width * 0.15f, size.height * 0.02f),
                radius = size.width * 0.85f
            ),
            radius = size.width * 0.85f,
            center = Offset(size.width * 0.15f, size.height * 0.02f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(c.auroraCool.copy(alpha = strength * 0.7f), Color.Transparent),
                center = Offset(size.width * 1.0f, size.height * 0.18f),
                radius = size.width * 0.7f
            ),
            radius = size.width * 0.7f,
            center = Offset(size.width * 1.0f, size.height * 0.18f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Section header — uppercase, tracked out (design system rule)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelMedium
        )
        trailing?.invoke()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Expressive shapes
// ─────────────────────────────────────────────────────────────────────────────

/** An icon sitting inside one of Material's expressive shapes (cookie, sunny, clover…). */
@Composable
fun ShapeIconBadge(
    icon: ImageVector,
    polygon: RoundedPolygon,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = size * 0.5f,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(container, polygon.toShape()),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(iconSize))
    }
}

/** Rank as a shaped badge: 1–3 get medal colours on burst/cookie shapes. */
@Composable
fun RankBadge(rank: Int, modifier: Modifier = Modifier, size: Dp = 34.dp) {
    val (polygon, container, content) = when (rank) {
        1 -> Triple(MaterialShapes.SoftBurst, Color(0xFFFFC94A), Color(0xFF3D2A00))
        2 -> Triple(MaterialShapes.Cookie9Sided, Color(0xFFD9DEE6), Color(0xFF2A2F36))
        3 -> Triple(MaterialShapes.Cookie7Sided, Color(0xFFE8A27A), Color(0xFF3D1E0B))
        else -> Triple(
            MaterialShapes.Circle,
            MaterialTheme.colorScheme.surfaceContainerHighest,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Box(
        modifier = modifier
            .size(size)
            .background(container, polygon.toShape()),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$rank",
            fontSize = (size.value * 0.42f).sp,
            fontWeight = FontWeight.Black,
            fontFamily = DefaultFontFamily,
            color = content
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Numbers that count up
// ─────────────────────────────────────────────────────────────────────────────

/** Rolls from 0 to [target] the first time it's shown, then tracks changes smoothly. */
@Composable
fun rememberCountUp(target: Int, durationMs: Int = 900): Int {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(target) {
        anim.animateTo(target.toFloat(), tween(durationMs, easing = FastOutSlowInEasing))
    }
    return anim.value.toInt()
}

// ─────────────────────────────────────────────────────────────────────────────
//  Stat tile
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StatTile(
    icon: ImageVector,
    polygon: RoundedPolygon,
    value: Int,
    label: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    prefix: String = "",
    suffix: String = "",
) {
    val shown = rememberCountUp(value)
    Surface(
        modifier = modifier
            .heightIn(min = 112.dp)
            .pressScale(pressedScale = 0.94f),
        shape = MaterialTheme.shapes.large,
        color = container
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ShapeIconBadge(
                icon = icon,
                polygon = polygon,
                container = content.copy(alpha = 0.14f),
                content = content,
                size = 32.dp
            )
            Column {
                Text(
                    text = "$prefix$shown$suffix",
                    color = content,
                    maxLines = 1,
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = label,
                    color = content.copy(alpha = 0.72f),
                    maxLines = 2,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Cards
// ─────────────────────────────────────────────────────────────────────────────

/** The standard Meluko card: large expressive corners, no elevation, springy press. */
@Composable
fun MelukoCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
    shape: Shape = MaterialTheme.shapes.large,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier.pressScale(pressedScale = 0.97f),
            shape = shape,
            color = color
        ) { Column(content = content) }
    } else {
        Surface(modifier = modifier, shape = shape, color = color) { Column(content = content) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Status
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun statusColor(status: WakeStatus): Color {
    val c = MelukoTheme.colors
    return when (status) {
        WakeStatus.Awake -> c.awake
        WakeStatus.Sleeping -> c.sleeping
        WakeStatus.Missed -> c.missed
    }
}

/** Coloured dot + label, e.g. "● Awake · 7:02 AM". */
@Composable
fun StatusLabel(status: WakeStatus, wakeTime: String?, modifier: Modifier = Modifier) {
    val color by animateColorAsState(statusColor(status), label = "statusColor")
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(7.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = when (status) {
                WakeStatus.Awake -> if (wakeTime.isNullOrBlank()) "Awake" else "Awake · $wakeTime"
                WakeStatus.Sleeping -> "Sleeping"
                WakeStatus.Missed -> "Missed"
            },
            color = color,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Loading — Material 3 Expressive morphing-shape indicator
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MelukoLoading(modifier: Modifier = Modifier, label: String? = null) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LoadingIndicator(
                modifier = Modifier.size(72.dp),
                color = MaterialTheme.colorScheme.primary
            )
            if (label != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = label,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Entrance choreography
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Fade + rise in, delayed by [index] so a screen's cards cascade in rather
 * than popping all at once.
 */
fun Modifier.staggeredEnter(index: Int, stepMs: Long = 55L): Modifier = composed {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * stepMs)
        progress.animateTo(1f, tween(480, easing = FastOutSlowInEasing))
    }
    this.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 36.dp.toPx()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Small text helpers
// ─────────────────────────────────────────────────────────────────────────────

fun plural(count: Int, one: String, many: String = one + "s"): String =
    "$count ${if (count == 1) one else many}"

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    )
}
