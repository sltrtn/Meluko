package com.alarm.meluko.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alarm.meluko.ui.theme.DefaultFontFamily

/**
 * Springy press-scale effect. Element shrinks on press and bounces back.
 * Uses Animatable + graphicsLayer to avoid recomposition during animation.
 */
fun Modifier.pressScale(
    pressedScale: Float = 0.94f,
    releasedScale: Float = 1.0f,
    stiffness: Float = Spring.StiffnessMedium,
    dampingRatio: Float = Spring.DampingRatioNoBouncy
): Modifier = composed {
    val scale = remember { Animatable(releasedScale) }
    val scope = rememberCoroutineScope()
    this
        .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
        .pointerInput(Unit) {
            awaitEachGesture {
                // requireUnconsumed = false lets parent clickable elements still receive the tap
                awaitFirstDown(requireUnconsumed = false)
                val pressJob = scope.launch {
                    scale.animateTo(pressedScale, spring(dampingRatio, stiffness))
                }
                waitForUpOrCancellation()
                pressJob.cancel()
                scope.launch {
                    scale.animateTo(releasedScale, spring(dampingRatio, stiffness))
                }
            }
        }
}

fun Modifier.chipPressScale(): Modifier = pressScale(
    pressedScale = 0.92f,
    stiffness = Spring.StiffnessMedium,
    dampingRatio = Spring.DampingRatioLowBouncy
)

fun Modifier.fabPressScale(): Modifier = pressScale(
    pressedScale = 0.90f,
    stiffness = Spring.StiffnessMediumLow,
    dampingRatio = Spring.DampingRatioMediumBouncy
)

/**
 * True for exactly one composition when [value] goes up from its previous
 * value — used to fire a one-shot celebration animation (e.g. streak ticking
 * up) without any extra state hoisting at the call site.
 */
@Composable
fun rememberIncreaseTrigger(value: Int): Boolean {
    var previous by remember { mutableStateOf(value) }
    val increased = value > previous
    SideEffect { previous = value }
    return increased
}

/**
 * Springy pop: scales up then settles back to normal. Fire it by flipping
 * [trigger] to true for one composition (see [rememberIncreaseTrigger]).
 */
fun Modifier.celebrateBurst(trigger: Boolean): Modifier = composed {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(trigger) {
        if (trigger) {
            scope.launch {
                scale.animateTo(1.28f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
                scale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
            }
        }
    }
    this.graphicsLayer { scaleX = scale.value; scaleY = scale.value }
}

@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val today = remember(locale) {
        java.text.SimpleDateFormat("EEEE, MMMM d", locale).format(java.util.Date())
    }
    androidx.compose.foundation.layout.Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 20.dp, top = 20.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = (eyebrow ?: today).uppercase(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = DefaultFontFamily,
                letterSpacing = 1.4.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = title,
                fontSize = 32.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Black,
                fontFamily = DefaultFontFamily,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        trailing?.invoke()
    }
}
