package com.alarm.meluko.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alarm.meluko.ui.theme.DefaultFontFamily
import com.alarm.meluko.ui.theme.MelukoTheme

@Composable
fun SetFirstAlarmScreen(
    homeViewModel: HomeViewModel = hiltViewModel(),
    proposalViewModel: ProposalViewModel = hiltViewModel(),
    onFinish: () -> Unit = {}
) {
    val primaryTribeId by homeViewModel.primaryTribeId.collectAsState()
    val isLoading by proposalViewModel.isLoading.collectAsState()
    val error by proposalViewModel.error.collectAsState()

    var selectedHour by remember { mutableStateOf("07") }
    var selectedMinute by remember { mutableStateOf("30") }
    var selectedAmPm by remember { mutableStateOf("AM") }

    val tribeId = primaryTribeId

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "YOUR FIRST ALARM",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = DefaultFontFamily,
                letterSpacing = 4.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Pick a wake-up time to propose to your tribe.",
                fontSize = 12.sp,
                fontFamily = DefaultFontFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WheelPicker(
                    items = (1..12).map { it.toString().padStart(2, '0') },
                    initialIndex = 6,
                    onItemSelected = { selectedHour = it }
                )

                Text(
                    text = ":",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = DefaultFontFamily,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp).offset(y = (-4).dp)
                )

                WheelPicker(
                    items = (0..59).map { it.toString().padStart(2, '0') },
                    initialIndex = 30,
                    onItemSelected = { selectedMinute = it }
                )

                Spacer(Modifier.width(16.dp))

                WheelPicker(
                    items = listOf("AM", "PM"),
                    initialIndex = 0,
                    onItemSelected = { selectedAmPm = it }
                )
            }

            Spacer(Modifier.height(24.dp))

            if (error != null) {
                Text(
                    text = error!!,
                    fontSize = 12.sp,
                    fontFamily = DefaultFontFamily,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Button(
                onClick = {
                    if (tribeId != null) {
                        proposalViewModel.createProposal(
                            tribeId = tribeId,
                            proposedTime = "$selectedHour:$selectedMinute $selectedAmPm",
                            onSuccess = onFinish
                        )
                    } else {
                        onFinish()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "PROPOSE & CONTINUE",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = DefaultFontFamily,
                        letterSpacing = 2.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            TextButton(
                onClick = onFinish
            ) {
                Text(
                    text = "Skip, use default (07:30)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = DefaultFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SetFirstAlarmPreview() {
    MelukoTheme {
        SetFirstAlarmScreen()
    }
}
