package com.alarm.meluko.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alarm.meluko.ui.theme.DefaultFontFamily
import com.alarm.meluko.ui.theme.BodyFontFamily
import com.alarm.meluko.util.to24HourTime
import kotlinx.coroutines.delay

private enum class ProposeState { Idle, Sending, Success }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProposeAlarmBottomSheet(
    tribeId: String,
    proposalViewModel: ProposalViewModel,
    onDismiss: () -> Unit,
    initialTime: String = "07:30",
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isLoading by proposalViewModel.isLoading.collectAsState()
    var proposeState by remember { mutableStateOf(ProposeState.Idle) }

    LaunchedEffect(proposeState) {
        if (proposeState == ProposeState.Success) {
            delay(1500L)
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = MaterialTheme.colorScheme.surface,
        dragHandle       = null,
        shape            = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 48.dp, top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (proposeState == ProposeState.Success) {
                val pulseTransition = rememberInfiniteTransition(label = "pulse")
                val pulseScale by pulseTransition.animateFloat(
                    initialValue = 0.92f,
                    targetValue = 1.08f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseScale"
                )
                Spacer(Modifier.height(40.dp))
                AnimatedVisibility(
                    visible = true,
                    enter = scaleIn(
                        initialScale = 0f,
                        animationSpec = MotionSpec.touch
                    ) + fadeIn()
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier
                            .size(80.dp)
                            .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }
                    )
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "PROPOSAL SENT!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = DefaultFontFamily,
                    letterSpacing = 3.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "The tribe has 2 hours to veto.\nCheck back to see the result.",
                    fontSize = 13.sp,
                    fontFamily = BodyFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                Spacer(Modifier.height(40.dp))
            } else {
                Text(
                    text       = "PROPOSE TIME",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = DefaultFontFamily,
                    letterSpacing = 4.sp,
                    color      = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text       = "Everyone gets 2 hours to veto it.",
                    fontSize   = 12.sp,
                    fontFamily = BodyFontFamily,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(48.dp))

                // Start the wheels on the tribe's current alarm, not an arbitrary time.
                val (initClock, initAmPm) = remember(initialTime) {
                    com.alarm.meluko.util.to12HourParts(initialTime)
                }
                val initHour = initClock.substringBefore(":").toIntOrNull()?.coerceIn(1, 12) ?: 7
                val initMinute = initClock.substringAfter(":", "30").toIntOrNull()?.coerceIn(0, 59) ?: 30
                var selectedHour by remember { mutableStateOf(initHour.toString().padStart(2, '0')) }
                var selectedMinute by remember { mutableStateOf(initMinute.toString().padStart(2, '0')) }
                var selectedAmPm by remember { mutableStateOf(if (initAmPm == "PM") "PM" else "AM") }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WheelPicker(
                        items = (1..12).map { it.toString().padStart(2, '0') },
                        initialIndex = initHour - 1,
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
                        initialIndex = initMinute,
                        onItemSelected = { selectedMinute = it }
                    )

                    Spacer(Modifier.width(16.dp))

                    WheelPicker(
                        items = listOf("AM", "PM"),
                        initialIndex = if (initAmPm == "PM") 1 else 0,
                        onItemSelected = { selectedAmPm = it }
                    )
                }

                Spacer(Modifier.height(48.dp))

                Button(
                    onClick = {
                        proposeState = ProposeState.Sending
                        proposalViewModel.createProposal(
                            tribeId = tribeId,
                            proposedTime = to24HourTime(selectedHour, selectedMinute, selectedAmPm),
                            onSuccess = { proposeState = ProposeState.Success },
                            onError = { proposeState = ProposeState.Idle }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .height(64.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    enabled = proposeState == ProposeState.Idle
                ) {
                    if (proposeState == ProposeState.Sending) {
                        LoadingIndicator(
                            modifier = Modifier.size(36.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        Text(
                            text       = "SEND PROPOSAL",
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = DefaultFontFamily,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    items: List<String>,
    initialIndex: Int,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // 5 rows visible with 2 blank padding rows at each end, so when snapped the
    // centred row is items[firstVisibleItemIndex].
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val density = LocalDensity.current
    val itemHeightPx = with(density) { 68.dp.toPx() }

    val paddedItems = listOf("", "") + items + listOf("", "")

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }.collect { index ->
            if (index in items.indices) {
                onItemSelected(items[index])
            }
        }
    }

    Box(
        modifier = modifier
            .width(90.dp)
            .height(340.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                )
        )

        LazyColumn(
            state = listState,
            flingBehavior = snapBehavior,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(paddedItems.size) { i ->
                val text = paddedItems[i]

                Box(
                    modifier = Modifier
                        .height(68.dp)
                        .graphicsLayer {
                            val firstVisible = listState.firstVisibleItemIndex
                            val scrollOffset = listState.firstVisibleItemScrollOffset
                            val itemCenter = (i - firstVisible) * itemHeightPx - scrollOffset + itemHeightPx / 2f
                            val viewportCenter = (listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset) / 2f
                            val distance = kotlin.math.abs(viewportCenter - itemCenter) / itemHeightPx

                            val calculatedScale = (1f - (distance * 0.3f)).coerceIn(0.4f, 1f)
                            val calculatedAlpha = (1f - (distance * 0.5f)).coerceIn(0.1f, 1f)

                            scaleX = calculatedScale
                            scaleY = calculatedScale
                            alpha = calculatedAlpha
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (text.isNotEmpty()) {
                        Text(
                            text = text,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = DefaultFontFamily,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Box(modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .align(Alignment.TopCenter)
            .background(
                Brush.verticalGradient(
                    colors = listOf(MaterialTheme.colorScheme.surface, Color.Transparent)
                )
            )
        )
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .align(Alignment.BottomCenter)
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, MaterialTheme.colorScheme.surface)
                )
            )
        )
    }
}
