package com.alarm.meluko.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Springy press-scale effect. Element shrinks on press and bounces back.
 */
fun Modifier.pressScale(
    pressedScale: Float = 0.94f,
    releasedScale: Float = 1.0f,
    stiffness: Float = Spring.StiffnessMediumLow,
    dampingRatio: Float = Spring.DampingRatioMediumBouncy
): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else releasedScale,
        animationSpec = spring(dampingRatio = dampingRatio, stiffness = stiffness),
        label = "pressScale"
    )
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    tryAwaitRelease()
                    isPressed = false
                }
            )
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
