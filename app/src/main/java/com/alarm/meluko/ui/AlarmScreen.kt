package com.alarm.meluko.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alarm.meluko.ui.theme.DefaultFontFamily
import com.alarm.meluko.ui.theme.MelukoTheme

@Composable
fun AlarmScreen(
    alarmTime: String = "07:30",
    onDismiss: () -> Unit = {}
) {
    val parts = alarmTime.split(":").let {
        Pair(
            it.getOrElse(0) { "07" },
            it.getOrElse(1) { "30" }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = parts.first,
                    fontSize = 96.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = DefaultFontFamily,
                    color = Color.White,
                    lineHeight = 100.sp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = ":",
                    fontSize = 96.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = DefaultFontFamily,
                    color = Color.White.copy(alpha = 0.3f),
                    lineHeight = 100.sp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = parts.second,
                    fontSize = 96.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = DefaultFontFamily,
                    color = Color.White,
                    lineHeight = 100.sp
                )
            }

            Spacer(Modifier.height(8.dp))

            val amPm = if (alarmTime.contains("PM", ignoreCase = true)) "PM" else "AM"
            Text(
                text = amPm,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = DefaultFontFamily,
                color = Color.White.copy(alpha = 0.5f),
                letterSpacing = 2.sp
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 32.dp)
                .padding(bottom = 48.dp)
        ) {
            SwipeToWakeButton(
                onWake = onDismiss,
                trackHeight = 88.dp,
                thumbSize = 72.dp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AlarmScreenPreview() {
    MelukoTheme(darkTheme = true) {
        AlarmScreen(alarmTime = "07:30", onDismiss = {})
    }
}
