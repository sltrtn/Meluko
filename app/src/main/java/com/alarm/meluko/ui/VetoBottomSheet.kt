package com.alarm.meluko.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alarm.meluko.ui.theme.DefaultFontFamily
import com.alarm.meluko.ui.theme.MelukoTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VetoBottomSheet(
    vetosLeft: Int,
    onDismiss: () -> Unit,
    onConfirmVeto: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                .padding(bottom = 48.dp, top = 32.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text       = "CAST VETO",
                fontSize   = 22.sp,
                fontWeight = FontWeight.Black,
                fontFamily = DefaultFontFamily,
                letterSpacing = 2.sp,
                color      = MaterialTheme.colorScheme.error
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text       = "Are you absolutely sure? A veto will instantly kill this proposal for the entire tribe.",
                fontSize   = 14.sp,
                fontFamily = DefaultFontFamily,
                color      = MaterialTheme.colorScheme.onSurface,
                textAlign  = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(24.dp))

            // Vetos Remaining visualization with animation
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VETOS REMAINING:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = DefaultFontFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(2) { index ->
                            val isActive = index < vetosLeft
                            AnimatedContent(
                                targetState = isActive,
                                transitionSpec = {
                                    (scaleIn(tween(200)) + fadeIn(tween(200)))
                                        .togetherWith(scaleOut(tween(150)) + fadeOut(tween(150)))
                                },
                                label = "vetoIcon$index"
                            ) { active ->
                                Text(
                                    text = if (active) "\uD83D\uDD25" else "\u25CB",
                                    fontSize = 16.sp,
                                    modifier = Modifier.alpha(if (active) 1f else 0.3f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(48.dp))

            // Big Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text       = "NO, CANCEL",
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = DefaultFontFamily
                    )
                }

                Button(
                    onClick = onConfirmVeto,
                    enabled = vetosLeft > 0,
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(
                        text       = "YES, VETO",
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = DefaultFontFamily
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun VetoBottomSheetPreview() {
    MelukoTheme {
        VetoBottomSheet(vetosLeft = 1, onDismiss = {}, onConfirmVeto = {})
    }
}
