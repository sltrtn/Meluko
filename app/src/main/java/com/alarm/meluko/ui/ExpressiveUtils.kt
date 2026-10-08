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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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

@Composable
fun ScreenHeader(title: String) {
    val today = remember {
        java.text.SimpleDateFormat("EEEE, MMMM d", java.util.Locale.getDefault())
            .format(java.util.Date())
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            fontFamily = DefaultFontFamily,
            letterSpacing = 3.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = today,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = DefaultFontFamily,
            letterSpacing = 0.3.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
