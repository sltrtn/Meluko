package com.alarm.meluko.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alarm.meluko.ui.theme.DefaultFontFamily
import kotlinx.coroutines.delay
import kotlin.random.Random

data class ScatterLetter(
    val char: Char,
    val size: Float,
    val rotation: Float,
    val xPercent: Float,
    val yPercent: Float,
    val delayMs: Long
)

@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }

    // Generate random letters to fill the screen
    val letters = remember {
        val chars = "MELUKO".toCharArray()
        List(45) {
            ScatterLetter(
                char = chars.random(),
                size = Random.nextInt(40, 160).toFloat(),
                rotation = if (Random.nextBoolean()) 0f else listOf(90f, -90f).random(),
                xPercent = Random.nextFloat(),
                yPercent = Random.nextFloat(),
                delayMs = Random.nextLong(0, 600)
            )
        }
    }

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2200) // Wait for all bounces to finish + some reading time
        onSplashComplete()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        val maxWidth = maxWidth
        val maxHeight = maxHeight

        // Draw the scattered background letters
        letters.forEach { letter ->
            BouncingLetter(
                letter = letter,
                maxWidth = maxWidth.value,
                maxHeight = maxHeight.value,
                startTrigger = startAnimation
            )
        }

        // Central main logo fading and scaling in
        val mainScale by animateFloatAsState(
            targetValue = if (startAnimation) 1.2f else 0.5f,
            animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            label = "MainScale"
        )
        val mainAlpha by animateFloatAsState(
            targetValue = if (startAnimation) 1f else 0f,
            animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            label = "MainAlpha"
        )

        Text(
            text          = "MELUKO",
            fontSize      = 48.sp,
            fontWeight    = FontWeight.Black,
            fontFamily    = DefaultFontFamily,
            color         = MaterialTheme.colorScheme.primary.copy(alpha = mainAlpha),
            letterSpacing = 12.sp,
            modifier      = Modifier.scale(mainScale)
        )
    }
}

@Composable
private fun BouncingLetter(
    letter: ScatterLetter,
    maxWidth: Float,
    maxHeight: Float,
    startTrigger: Boolean
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(startTrigger) {
        if (startTrigger) {
            delay(letter.delayMs)
            isVisible = true
        }
    }

    // Fun bouncy spring animation
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "BounceScale"
    )

    Text(
        text = letter.char.toString(),
        fontSize = letter.size.sp,
        fontWeight = FontWeight.Black,
        fontFamily = DefaultFontFamily,
        // Keep alpha low so it acts as a background texture and doesn't overpower the main logo
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
        modifier = Modifier
            .offset(
                // Center the offset calculation so xPercent=0.5 is the exact center of the screen
                x = ((letter.xPercent - 0.5f) * maxWidth).dp,
                y = ((letter.yPercent - 0.5f) * maxHeight).dp
            )
            .scale(scale)
            .rotate(letter.rotation)
    )
}