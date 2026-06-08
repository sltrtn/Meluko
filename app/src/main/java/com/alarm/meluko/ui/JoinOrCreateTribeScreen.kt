package com.alarm.meluko.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alarm.meluko.ui.theme.BodyFontFamily
import com.alarm.meluko.ui.theme.DefaultFontFamily
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinOrCreateTribeScreen(
    onFinish: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    var isJoining by remember { mutableStateOf(true) }
    var inviteCode by remember { mutableStateOf("") }
    var tribeName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val focusManager = LocalFocusManager.current

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(80); visible = true }

    val bgColor = MaterialTheme.colorScheme.background
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // Glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.12f), Color.Transparent),
                        radius = 600f
                    )
                )
        )

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(400)) + slideInVertically(tween(400, easing = FastOutSlowInEasing)) { it / 8 }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(56.dp))

                Text(
                    text = "STEP 2 OF 2",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = DefaultFontFamily,
                    letterSpacing = 3.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Find Your\nTribe",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = DefaultFontFamily,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    lineHeight = 44.sp
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Join an existing tribe with an invite code\nor start your own.",
                    fontSize = 15.sp,
                    fontFamily = BodyFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(Modifier.height(36.dp))

                // Toggle chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(true to "Join a Tribe", false to "Create a Tribe").forEach { (joining, label) ->
                        val isSelected = isJoining == joining
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            onClick = { isJoining = joining }
                        ) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = DefaultFontFamily,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))

                AnimatedContent(
                    targetState = isJoining,
                    transitionSpec = {
                        (fadeIn(tween(200)) + slideInHorizontally(tween(220)) { if (targetState) -40 else 40 })
                            .togetherWith(fadeOut(tween(150)) + slideOutHorizontally(tween(150)) { if (targetState) 40 else -40 })
                    },
                    label = "form"
                ) { joining ->
                    if (joining) {
                        Column {
                            OutlinedTextField(
                                value = inviteCode,
                                onValueChange = { if (it.length <= 6) inviteCode = it.uppercase() },
                                label = { Text("Invite Code") },
                                placeholder = { Text("e.g. ABC123") },
                                leadingIcon = {
                                    Icon(Icons.Rounded.ContentPaste, null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Ask your tribe admin for their 6-character invite code.",
                                fontSize = 12.sp,
                                fontFamily = BodyFontFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        Column {
                            OutlinedTextField(
                                value = tribeName,
                                onValueChange = { tribeName = it },
                                label = { Text("Tribe Name") },
                                leadingIcon = {
                                    Icon(Icons.Rounded.People, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Next
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                            Spacer(Modifier.height(16.dp))
                            OutlinedTextField(
                                value = description,
                                onValueChange = { if (it.length <= 100) description = it },
                                label = { Text("Description (optional)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                minLines = 2,
                                maxLines = 3,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                            Spacer(Modifier.height(12.dp))
                            // Invite code preview for new tribes
                            if (tribeName.isNotBlank()) {
                                val previewCode = tribeName.take(3).uppercase() + "XXX"
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Rounded.Add, null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(16.dp))
                                        Text(
                                            text = "Your tribe will get a 6-digit invite code for others to join.",
                                            fontSize = 12.sp,
                                            fontFamily = BodyFontFamily,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (error != null) {
                    Spacer(Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 13.sp,
                            fontFamily = BodyFontFamily
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        if (isJoining && inviteCode.length == 6) {
                            viewModel.joinTribe(inviteCode, onSuccess = onFinish)
                        } else if (!isJoining && tribeName.isNotBlank()) {
                            viewModel.createTribe(tribeName, description, onSuccess = onFinish)
                        }
                    },
                    enabled = !isLoading && if (isJoining) inviteCode.length == 6 else tribeName.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = if (isJoining) "JOIN TRIBE" else "CREATE TRIBE",
                                fontWeight = FontWeight.Black,
                                fontFamily = DefaultFontFamily,
                                fontSize = 15.sp,
                                letterSpacing = 2.sp
                            )
                            Icon(Icons.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
