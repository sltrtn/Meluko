package com.alarm.meluko.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alarm.meluko.ui.theme.DefaultFontFamily
import com.alarm.meluko.ui.theme.BodyFontFamily
import kotlinx.coroutines.delay

private enum class VetoState { Idle, Confirming, Success }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VetoBottomSheet(
    vetosLeft: Int,
    onDismiss: () -> Unit,
    onConfirmVeto: (onSuccess: () -> Unit, onError: () -> Unit) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var vetoState by remember { mutableStateOf(VetoState.Idle) }

    LaunchedEffect(vetoState) {
        if (vetoState == VetoState.Success) {
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
                .padding(bottom = 48.dp, top = 32.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (vetoState == VetoState.Success) {
                Spacer(Modifier.height(24.dp))
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
                        modifier = Modifier.size(80.dp)
                    )
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "VETO CAST!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = DefaultFontFamily,
                    letterSpacing = 3.sp,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "The proposal has been killed.\nYour tribe will keep the current time.",
                    fontSize = 13.sp,
                    fontFamily = BodyFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                )
                Spacer(Modifier.height(24.dp))
            } else {
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
                    fontFamily = BodyFontFamily,
                    color      = MaterialTheme.colorScheme.onSurface,
                    textAlign  = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(Modifier.height(24.dp))

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

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
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
                        onClick = {
                            vetoState = VetoState.Confirming
                            onConfirmVeto({
                                vetoState = VetoState.Success
                            }) {
                                vetoState = VetoState.Idle
                            }
                        },
                        enabled = vetoState == VetoState.Idle && vetosLeft > 0,
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        if (vetoState == VetoState.Confirming) {
                            LoadingIndicator(
                            modifier = Modifier.size(36.dp),
                            color = MaterialTheme.colorScheme.onError
                        )
                        } else {
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
    }
}
