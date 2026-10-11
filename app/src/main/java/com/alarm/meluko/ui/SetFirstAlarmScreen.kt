package com.alarm.meluko.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alarm.meluko.ui.theme.BodyFontFamily
import com.alarm.meluko.ui.theme.DefaultFontFamily
import com.alarm.meluko.ui.theme.MelukoTheme
import com.alarm.meluko.util.to24HourTime
import kotlinx.coroutines.delay

@Composable
fun SetFirstAlarmScreen(
    onBack: () -> Unit = {},
    homeViewModel: HomeViewModel = hiltViewModel(),
    proposalViewModel: ProposalViewModel = hiltViewModel(),
    onFinish: () -> Unit = {}
) {
    val primaryTribeId by homeViewModel.primaryTribeId.collectAsState()
    val isLoading by proposalViewModel.isLoading.collectAsState()
    val error by proposalViewModel.error.collectAsState()

    var selectedHour by rememberSaveable { mutableStateOf("07") }
    var selectedMinute by rememberSaveable { mutableStateOf("30") }
    var selectedAmPm by rememberSaveable { mutableStateOf("AM") }

    val tribeId = primaryTribeId

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(80); visible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        OnboardingGlowBackdrop()

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(400)) + slideInVertically(tween(400, easing = FastOutSlowInEasing)) { it / 8 }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OnboardingStepHeader(
                        step = 3,
                        totalSteps = 3,
                        title = "Set Your\nFirst Alarm",
                        subtitle = "Pick a wake-up time to propose to your tribe."
                    )

                    Spacer(Modifier.height(24.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WheelPicker(
                            items = (1..12).map { it.toString().padStart(2, '0') },
                            initialIndex = (selectedHour.toIntOrNull() ?: 7) - 1,
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
                            initialIndex = selectedMinute.toIntOrNull() ?: 30,
                            onItemSelected = { selectedMinute = it }
                        )

                        Spacer(Modifier.width(16.dp))

                        WheelPicker(
                            items = listOf("AM", "PM"),
                            initialIndex = if (selectedAmPm == "PM") 1 else 0,
                            onItemSelected = { selectedAmPm = it }
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    if (error != null) {
                        Text(
                            text = error!!,
                            fontSize = 12.sp,
                            fontFamily = BodyFontFamily,
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
                                    proposedTime = to24HourTime(selectedHour, selectedMinute, selectedAmPm),
                                    onSuccess = onFinish
                                )
                            } else {
                                onFinish()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .pressScale(),
                        shape = MaterialTheme.shapes.large,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            LoadingIndicator(
                            modifier = Modifier.size(36.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        } else {
                            Text(
                                text = "PROPOSE & CONTINUE",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = DefaultFontFamily,
                                letterSpacing = 2.sp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    TextButton(onClick = onFinish) {
                        Text(
                            text = "Skip, use default (07:30)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = BodyFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        OnboardingBackButton(
            onBack = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 20.dp, top = 16.dp)
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SetFirstAlarmPreview() {
    MelukoTheme {
        SetFirstAlarmScreen()
    }
}
