package com.alarm.meluko.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alarm.meluko.ui.theme.DefaultFontFamily
import com.alarm.meluko.ui.theme.MelukoTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProposeAlarmBottomSheet(
    tribeId: String,
    proposalViewModel: ProposalViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isLoading by proposalViewModel.isLoading.collectAsState()

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
                text       = "The tribe will vote on this change.",
                fontSize   = 12.sp,
                fontFamily = DefaultFontFamily,
                color      = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(48.dp))

            var selectedHour by remember { mutableStateOf("07") }
            var selectedMinute by remember { mutableStateOf("30") }
            var selectedAmPm by remember { mutableStateOf("AM") }

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

            Spacer(Modifier.height(48.dp))

            Button(
                onClick = {
                    proposalViewModel.createProposal(
                        tribeId = tribeId,
                        proposedTime = "$selectedHour:$selectedMinute $selectedAmPm",
                        onSuccess = onDismiss
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
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
                        text       = "SEND PROPOSAL",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = DefaultFontFamily,
                        letterSpacing = 2.sp,
                        color      = MaterialTheme.colorScheme.onPrimary
                    )
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
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val density = LocalDensity.current
    val itemHeightPx = with(density) { 68.dp.toPx() }

    val paddedItems = listOf("", "") + items + listOf("", "")

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }.collect { index ->
            if (index in 0 until items.size) {
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

@Preview
@Composable
private fun ProposeAlarmBottomSheetPreview() {
    MelukoTheme {
        Scaffold { padding ->
            Box(Modifier.padding(padding).fillMaxSize().background(Color.Black)) {
            }
        }
    }
}
