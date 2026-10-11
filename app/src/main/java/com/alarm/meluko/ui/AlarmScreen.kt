package com.alarm.meluko.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alarm.meluko.ui.theme.BodyFontFamily
import com.alarm.meluko.ui.theme.DefaultFontFamily
import com.alarm.meluko.ui.theme.MelukoTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class AlarmTribeSnapshot(
    val tribeName: String,
    val awakeCount: Int,
    val otherCount: Int,
    val awakeMembers: List<Pair<String, String?>>
)

/** Non-null once the user has swiped. [streakDays] fills in when the wake is recorded. */
data class WakeCelebration(val streakDays: Int?)

// The alarm screen is a brand moment, so it uses the Sunrise palette directly
// rather than dynamic color (which would tint it with the wallpaper).
private val SunriseAmber = Color(0xFFFF8C00)
private val SunriseGold = Color(0xFFFFC94A)
private val SunriseEmber = Color(0xFF6F3000)

@Composable
fun AlarmScreen(
    tribe: AlarmTribeSnapshot? = null,
    celebration: WakeCelebration? = null,
    reduceMotion: Boolean = false,
    onWake: () -> Unit = {}
) {
    val locale = LocalConfiguration.current.locales[0]
    val now by produceState(initialValue = Date()) {
        while (true) {
            value = Date()
            delay(1_000L)
        }
    }

    // Sunrise creeps up from the bottom over the first minute of ringing.
    val sunrise = remember { Animatable(if (reduceMotion) 0.6f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduceMotion) sunrise.animateTo(1f, tween(60_000, easing = FastOutSlowInEasing))
    }

    val breathe = if (reduceMotion) 0.5f else {
        val t = rememberInfiniteTransition(label = "breathe")
        t.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(3_200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "breatheValue"
        ).value
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .drawBehind {
                val s = sunrise.value
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            SunriseAmber.copy(alpha = 0.10f + 0.28f * s),
                            SunriseEmber.copy(alpha = 0.10f * s),
                            Color.Transparent
                        ),
                        center = Offset(size.width / 2f, size.height * (1.15f - 0.25f * s)),
                        radius = size.height * (0.45f + 0.35f * s)
                    )
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = greetingFor(now).uppercase(),
                color = SunriseGold.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelMedium
            )

            Spacer(Modifier.height(20.dp))

            Box(contentAlignment = Alignment.Center) {
                // Breathing glow behind the clock
                Box(
                    Modifier
                        .size(260.dp)
                        .graphicsLayer { scaleX = 0.9f + 0.1f * breathe; scaleY = scaleX }
                        .background(
                            Brush.radialGradient(
                                listOf(SunriseAmber.copy(alpha = 0.10f + 0.12f * breathe), Color.Transparent)
                            ),
                            CircleShape
                        )
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = SimpleDateFormat("h:mm", locale).format(now),
                        fontSize = 104.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = DefaultFontFamily,
                        color = Color.White,
                        lineHeight = 108.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = SimpleDateFormat("a", Locale.US).format(now),
                        color = Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.padding(bottom = 18.dp),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            Text(
                text = SimpleDateFormat("EEEE, MMMM d", locale).format(now),
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(Modifier.height(36.dp))

            AnimatedVisibility(
                visible = tribe != null,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 3 }
            ) {
                tribe?.let { TribeStrip(it) }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 28.dp)
                .padding(bottom = 48.dp)
        ) {
            SwipeToWakeButton(
                onWake = onWake,
                trackHeight = 88.dp,
                thumbSize = 72.dp,
                reduceMotion = reduceMotion,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (celebration != null) {
            WakeCelebrationOverlay(celebration, reduceMotion)
        }
    }
}

private fun greetingFor(date: Date): String {
    val hour = Calendar.getInstance().apply { time = date }.get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 4..11 -> "Rise and shine"
        in 12..16 -> "Afternoon alarm"
        else -> "Time to get up"
    }
}

@Composable
private fun TribeStrip(tribe: AlarmTribeSnapshot) {
    val message = when {
        tribe.otherCount == 0 -> "Set the pace for ${tribe.tribeName}"
        tribe.awakeCount == 0 -> "Be the first one up in ${tribe.tribeName}"
        else -> "${tribe.awakeCount} of ${tribe.otherCount} already up in ${tribe.tribeName}"
    }
    Row(
        modifier = Modifier
            .padding(horizontal = 32.dp)
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(50))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(50))
            .padding(start = 8.dp, end = 18.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (tribe.awakeMembers.isNotEmpty()) {
            Box {
                tribe.awakeMembers.take(4).forEachIndexed { i, (name, photo) ->
                    UserAvatar(
                        photoUrl = photo,
                        name = name,
                        size = 30.dp,
                        modifier = Modifier
                            .padding(start = (i * 20).dp)
                            .border(2.dp, Color.Black, CircleShape)
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
        } else {
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = message,
            color = Color.White.copy(alpha = 0.85f),
            maxLines = 1,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun WakeCelebrationOverlay(celebration: WakeCelebration, reduceMotion: Boolean) {
    val burst = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduceMotion) burst.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }
    val spin = if (reduceMotion) 0f else {
        rememberInfiniteTransition(label = "rays").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(12_000, easing = LinearEasing)),
            label = "raySpin"
        ).value
    }
    // Odometer: show yesterday's count, then roll up to today's. A brand-new
    // streak just pops in as "1" (never flashes "0").
    var shownStreak by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(celebration.streakDays) {
        val streak = celebration.streakDays ?: return@LaunchedEffect
        if (streak > 1 && !reduceMotion) {
            shownStreak = streak - 1
            delay(350)
        }
        shownStreak = streak
    }
    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 4..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val b = burst.value
            val center = Offset(size.width / 2f, size.height * 0.45f)
            val maxR = size.height * 0.9f
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(SunriseGold.copy(alpha = 0.95f), SunriseAmber, SunriseEmber.copy(alpha = 0.96f)),
                    center = center,
                    radius = maxR * b + 1f
                ),
                radius = maxR * b,
                center = center
            )
            rotate(spin, center) {
                val rays = 16
                repeat(rays) { i ->
                    rotate(i * 360f / rays, center) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.12f * b),
                            start = Offset(center.x, center.y - 120.dp.toPx() * b),
                            end = Offset(center.x, center.y - maxR * b),
                            strokeWidth = 18.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                alpha = ((burst.value - 0.4f) / 0.6f).coerceIn(0f, 1f)
                translationY = (1f - burst.value) * 60f
            }
        ) {
            Text(
                text = greeting,
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge
            )
            val streak = shownStreak
            if (streak != null) {
                Spacer(Modifier.height(18.dp))
                AnimatedContent(
                    targetState = streak,
                    transitionSpec = {
                        (slideInVertically(spring(dampingRatio = 0.6f, stiffness = 300f)) { it } + fadeIn())
                            .togetherWith(slideOutVertically(tween(200)) { -it } + fadeOut(tween(150)))
                    },
                    label = "streakRoll"
                ) { value ->
                    Text(
                        text = "$value",
                        fontSize = 88.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = DefaultFontFamily,
                        color = Color.White,
                        lineHeight = 90.sp
                    )
                }
                Text(
                    text = if (celebration.streakDays == 1) "DAY STREAK STARTED" else "DAY STREAK",
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AlarmScreenPreview() {
    MelukoTheme(darkTheme = true) {
        AlarmScreen(
            tribe = AlarmTribeSnapshot("Hostel 4B", 2, 5, listOf("Sam" to null, "Ria" to null))
        )
    }
}
