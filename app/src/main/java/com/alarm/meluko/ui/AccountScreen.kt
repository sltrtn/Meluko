package com.alarm.meluko.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalUriHandler
import androidx.hilt.navigation.compose.hiltViewModel
import com.alarm.meluko.auth.AuthViewModel
import com.alarm.meluko.ui.theme.DefaultFontFamily
import com.alarm.meluko.ui.theme.BodyFontFamily
import com.alarm.meluko.ui.theme.MelukoTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    accountViewModel: AccountViewModel = hiltViewModel(),
    socialViewModel: SocialViewModel = hiltViewModel()
) {
    val displayName by accountViewModel.displayName.collectAsState()
    val username by accountViewModel.username.collectAsState()
    val bio by accountViewModel.bio.collectAsState()
    val photoUrl by accountViewModel.photoUrl.collectAsState()
    val streakDays by accountViewModel.streakDays.collectAsState()
    val bestStreak by accountViewModel.bestStreak.collectAsState()
    val totalWakes by accountViewModel.totalWakes.collectAsState()
    val isUpdating by accountViewModel.isUpdating.collectAsState()
    val notificationsEnabled by accountViewModel.notificationsEnabled.collectAsState()
    val tribesProgress by socialViewModel.tribes.collectAsState()

    val activeTribes = tribesProgress.map { TribeInfo(it.name, it.photoUrl, it.totalCount) }

    // Dialog / sheet state
    var showEditProfile by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }

    // Easter egg — tap MELUKO 7 times in the About dialog
    var easterEggTaps by remember { mutableStateOf(0) }
    var showSlater by remember { mutableStateOf(false) }
    LaunchedEffect(showAbout) {
        if (!showAbout) {
            easterEggTaps = 0
            showSlater = false
        }
    }

    // Edit profile form state
    var editName by remember(displayName) { mutableStateOf(displayName) }
    var editBio by remember(bio) { mutableStateOf(bio) }
    var pickedImageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> pickedImageUri = uri }

    // ── Edit Profile Dialog ───────────────────────────────────────────────────
    if (showEditProfile) {
        AlertDialog(
            onDismissRequest = { if (!isUpdating) showEditProfile = false },
            title = {
                Text(
                    text = "Edit Profile",
                    fontFamily = DefaultFontFamily,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Photo picker
                    Box(
                        contentAlignment = Alignment.BottomEnd,
                        modifier = Modifier.clickable { imagePicker.launch("image/*") }
                    ) {
                        UserAvatar(
                            photoUrl = pickedImageUri?.toString() ?: photoUrl,
                            name = displayName,
                            size = 80.dp
                        )
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                Icons.Outlined.CameraAlt,
                                contentDescription = "Change photo",
                                modifier = Modifier.padding(5.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Display Name", fontFamily = BodyFontFamily) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editBio,
                        onValueChange = { if (it.length <= 120) editBio = it },
                        label = { Text("Bio", fontFamily = BodyFontFamily) },
                        minLines = 2,
                        maxLines = 3,
                        supportingText = { Text("${editBio.length}/120", fontFamily = BodyFontFamily) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isUpdating) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        accountViewModel.updateProfile(
                            newDisplayName = editName,
                            newBio = editBio,
                            imageUri = pickedImageUri
                        ) {
                            showEditProfile = false
                            pickedImageUri = null
                        }
                    },
                    enabled = !isUpdating && editName.isNotBlank()
                ) {
                    Text("Save", fontFamily = DefaultFontFamily, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditProfile = false; pickedImageUri = null },
                    enabled = !isUpdating
                ) {
                    Text("Cancel", fontFamily = DefaultFontFamily)
                }
            }
        )
    }

    // ── Notifications Bottom Sheet ────────────────────────────────────────────
    if (showNotifications) {
        ModalBottomSheet(
            onDismissRequest = { showNotifications = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "Notifications",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = DefaultFontFamily
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Alarm nudges",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = DefaultFontFamily
                        )
                        Text(
                            text = "Get notified when tribe members wake up before you",
                            fontSize = 12.sp,
                            fontFamily = BodyFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { accountViewModel.updateNotificationPrefs(it) }
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    // ── About Dialog ──────────────────────────────────────────────────────────
    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = {
                // Tap 7 times to unlock the easter egg
                Text(
                    text = "MELUKO",
                    fontFamily = DefaultFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp,
                    letterSpacing = 4.sp,
                    modifier = Modifier.clickable {
                        easterEggTaps++
                        if (easterEggTaps >= 7) showSlater = true
                    }
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Version 1.0.0",
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Wake up together.\nStay accountable.",
                        fontFamily = DefaultFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Meluko is a social alarm app that helps you and your friends build consistent morning routines through accountability, healthy competition, and shared rituals.",
                        fontFamily = BodyFontFamily,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // ── 🥚 Easter egg ─────────────────────────────────────────
                    val uriHandler = LocalUriHandler.current
                    AnimatedVisibility(
                        visible = showSlater,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF1A0040),
                            modifier = Modifier.clickable {
                                uriHandler.openUri("https://github.com/sltrtn")
                            }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Slater",
                                    fontFamily = DefaultFontFamily,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 22.sp,
                                    letterSpacing = 3.sp,
                                    color = Color(0xFFD4AAFF)
                                )
                                Text(
                                    text = "The one who started it all.",
                                    fontFamily = BodyFontFamily,
                                    fontSize = 12.sp,
                                    color = Color(0xFF9B6FD6)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "github.com/sltrtn",
                                    fontFamily = BodyFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color(0xFFD4AAFF).copy(alpha = 0.7f),
                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                                )
                            }
                        }
                    }

                    // Hint text before unlocked
                    if (!showSlater && easterEggTaps in 1..6) {
                        Text(
                            text = "${7 - easterEggTaps} more...",
                            fontFamily = BodyFontFamily,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) {
                    Text("OK", fontFamily = DefaultFontFamily, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // ── Main Screen ───────────────────────────────────────────────────────────
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
        ) {
            // Header
            val today = remember {
                java.text.SimpleDateFormat("EEEE, d MMMM", java.util.Locale.getDefault())
                    .format(java.util.Date())
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ACCOUNT",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = DefaultFontFamily,
                    letterSpacing = 6.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = today,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = DefaultFontFamily,
                    letterSpacing = 0.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Hero profile card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        UserAvatar(photoUrl = photoUrl, name = displayName, size = 64.dp)
                        Column {
                            Text(
                                text = displayName.ifBlank { "Loading..." },
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = DefaultFontFamily,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = if (username.isNotBlank()) "@$username" else "",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = BodyFontFamily,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f)
                            )
                            if (bio.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = bio,
                                    fontSize = 12.sp,
                                    fontFamily = BodyFontFamily,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                                    maxLines = 2
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AccountPill(
                            text = "🔥 $streakDays-day streak",
                            color = MaterialTheme.colorScheme.tertiary,
                            on = MaterialTheme.colorScheme.onTertiary
                        )
                        AccountPill(
                            text = "☀️ $totalWakes wake-ups",
                            color = MaterialTheme.colorScheme.primary,
                            on = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Stats row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AccountStatCard("☀️", "$totalWakes", "Wake-ups", Modifier.weight(1f))
                AccountStatCard("🔥", "$streakDays d", "Streak", Modifier.weight(1f))
                AccountStatCard("🏅", "$bestStreak d", "Best", Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))

            // Your tribes
            if (activeTribes.isNotEmpty()) {
                Text(
                    text = "Your Tribes",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = DefaultFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(10.dp))
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    activeTribes.forEach { tribe ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                TribeAvatar(photoUrl = tribe.photoUrl, name = tribe.name, size = 32.dp)
                                Text(
                                    text = tribe.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = DefaultFontFamily,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${tribe.memberCount} members",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = BodyFontFamily,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            // Settings
            Text(
                text = "Settings",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = DefaultFontFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(10.dp))
            Surface(
                modifier = Modifier.padding(horizontal = 20.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsRow(
                        icon = Icons.Outlined.Edit,
                        label = "Edit Profile",
                        onClick = {
                            editName = displayName
                            editBio = bio
                            pickedImageUri = null
                            showEditProfile = true
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingsRow(
                        icon = Icons.Outlined.Notifications,
                        label = "Notifications",
                        onClick = { showNotifications = true }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingsRow(
                        icon = Icons.Outlined.Info,
                        label = "About Meluko",
                        onClick = { showAbout = true }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Sign out
            Surface(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                SettingsRow(
                    icon = Icons.AutoMirrored.Outlined.ExitToApp,
                    label = "Sign Out",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    textColor = MaterialTheme.colorScheme.onErrorContainer,
                    onClick = { authViewModel.signOut() }
                )
            }

            Spacer(Modifier.height(120.dp))
        }
    }
}

@Composable
private fun AccountPill(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    on: androidx.compose.ui.graphics.Color
) {
    Surface(shape = RoundedCornerShape(50), color = color) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = DefaultFontFamily,
            color = on,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun AccountStatCard(
    emoji: String,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(90.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(emoji, fontSize = 18.sp)
            Column {
                Text(
                    text = value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = DefaultFontFamily,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp
                )
                Text(
                    text = label,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = DefaultFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.3.sp
                )
            }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    textColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = androidx.compose.ui.graphics.Color.Transparent,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = DefaultFontFamily,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Preview
// ─────────────────────────────────────────────────────────────────────────────
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AccountPreview() {
    MelukoTheme { AccountScreen() }
}
