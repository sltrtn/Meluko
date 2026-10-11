package com.alarm.meluko.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.graphics.graphicsLayer
import com.alarm.meluko.BuildConfig
import com.alarm.meluko.ui.components.aurora
import com.alarm.meluko.ui.theme.DefaultFontFamily
import com.alarm.meluko.ui.theme.MelukoTheme
import com.alarm.meluko.auth.AuthState
import com.alarm.meluko.auth.AuthViewModel

@Composable
fun LoginScreen(authViewModel: AuthViewModel) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    val isLoading = authState is AuthState.Loading
    val errorMsg = (authState as? AuthState.Error)?.message

    // Animated entry — slides up and fades in on first render
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .aurora(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(600)) + slideInVertically(
                animationSpec = tween(600),
                initialOffsetY = { it / 4 }
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // ── Logo: an expressive sun, slowly turning ─────────────────
                val spin by rememberInfiniteTransition(label = "logo").animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(tween(24_000, easing = LinearEasing)),
                    label = "logoSpin"
                )
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .graphicsLayer { rotationZ = spin }
                            .background(
                                brush = Brush.linearGradient(MelukoTheme.colors.sunrise.reversed()),
                                shape = MaterialShapes.Sunny.toShape()
                            )
                    )
                    Text(
                        text = "M",
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = DefaultFontFamily,
                        color = Color(0xFF2A1100)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // ── Brand ────────────────────────────────────────────────────
                Text(
                    text = "Meluko",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Wake up. Together.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(56.dp))

                // ── Login Card ───────────────────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Join the Tribe",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Sign in to sync your alarm\nwith your hostel group.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // ── Google Sign-In Button ────────────────────────────
                        Button(
                            onClick = { authViewModel.signInWithGoogle(context) },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .pressScale(),
                            shape = MaterialTheme.shapes.large,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                disabledContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            )
                        ) {
                            if (isLoading) {
                                LoadingIndicator(
                            modifier = Modifier.size(36.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                            } else {
                                Text(
                                    text = "Continue with Google",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // ── Debug-only preview bypass ────────────────────────
                        if (BuildConfig.DEBUG) {
                            TextButton(
                                onClick = { authViewModel.debugPreviewSignIn() },
                                enabled = !isLoading
                            ) {
                                Text(
                                    text = "Skip Login (Debug Preview)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // ── Error banner with retry ──────────────────────────
                        if (errorMsg != null && !isLoading) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = errorMsg,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(
                                        onClick = { authViewModel.signInWithGoogle(context) }
                                    ) {
                                        Text(
                                            text = "RETRY",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "By continuing, you agree to Meluko's Social Sleep Terms.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}
