package com.alarm.meluko.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.alarm.meluko.auth.AuthViewModel
import com.alarm.meluko.ui.theme.DefaultFontFamily
import com.alarm.meluko.ui.theme.BodyFontFamily
import com.alarm.meluko.ui.theme.MelukoTheme
import com.alarm.meluko.ui.components.Hairline
import com.alarm.meluko.ui.components.MelukoCard
import com.alarm.meluko.ui.components.MelukoLoading
import com.alarm.meluko.ui.components.SectionHeader
import com.alarm.meluko.ui.components.ShapeIconBadge
import com.alarm.meluko.ui.components.StatTile
import com.alarm.meluko.ui.components.staggeredEnter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    accountViewModel: AccountViewModel = hiltViewModel(),
    socialViewModel: SocialViewModel = hiltViewModel()
) {
    val isLoaded by accountViewModel.isLoaded.collectAsState()
    val displayName by accountViewModel.displayName.collectAsState()
    val username by accountViewModel.username.collectAsState()
    val bio by accountViewModel.bio.collectAsState()
    val photoUrl by accountViewModel.photoUrl.collectAsState()
    val streakDays by accountViewModel.streakDays.collectAsState()
    val bestStreak by accountViewModel.bestStreak.collectAsState()
    val totalWakes by accountViewModel.totalWakes.collectAsState()
    val isUpdating by accountViewModel.isUpdating.collectAsState()
    val tribesProgress by socialViewModel.tribes.collectAsState()
    val accountError by accountViewModel.error.collectAsState()

    val activeTribes = tribesProgress.map { TribeInfo(it.id, it.name, it.photoUrl, it.totalCount) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(accountError) {
        accountError?.let {
            snackbarHostState.showSnackbar(it)
            accountViewModel.clearError()
        }
    }

    // Dialog / sheet state
    var showEditProfile by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showDeleteAccountConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current

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

    val alarmToneUri by accountViewModel.alarmToneUri.collectAsState()
    val alarmToneTitle by accountViewModel.alarmToneTitle.collectAsState()
    var showToneSheet by remember { mutableStateOf(false) }

    if (showToneSheet) {
        AlarmToneSheet(
            currentUri = alarmToneUri,
            onDismiss = { showToneSheet = false },
            onSave = { tone ->
                accountViewModel.updateAlarmTone(tone.uri, tone.title)
                showToneSheet = false
            }
        )
    }

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
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )

                    OutlinedTextField(
                        value = editBio,
                        onValueChange = { if (it.length <= 120) editBio = it },
                        label = { Text("Bio", fontFamily = BodyFontFamily) },
                        minLines = 2,
                        maxLines = 3,
                        supportingText = { Text("${editBio.length}/120", fontFamily = BodyFontFamily) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )

                    if (isUpdating) LoadingIndicator(modifier = Modifier.size(40.dp))
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
                    Text("Cancel", fontWeight = FontWeight.Bold, fontFamily = DefaultFontFamily)
                }
            }
        )
    }

    // ── About Dialog ──────────────────────────────────────────────────────────
    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = {
                // Tap 7 times to unlock the easter egg. No ripple, so the title
                // doesn't look tappable; haptics start once you're 3 away.
                val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                Text(
                    text = "MELUKO",
                    letterSpacing = 4.sp,
                    modifier = Modifier.clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {
                        if (showSlater) return@clickable
                        easterEggTaps++
                        when {
                            easterEggTaps >= 7 -> {
                                showSlater = true
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            }
                            easterEggTaps >= 4 -> haptic.performHapticFeedback(
                                androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove
                            )
                        }
                    },
                    style = MaterialTheme.typography.headlineMedium
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Version ${com.alarm.meluko.BuildConfig.VERSION_NAME}",
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Wake up together.\nStay accountable.",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Meluko is a social alarm app that helps you and your friends build consistent morning routines through accountability, healthy competition, and shared rituals.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )

                    // ── Easter egg ─────────────────────────────────────────
                    val uriHandler = LocalUriHandler.current
                    AnimatedVisibility(
                        visible = showSlater,
                        enter = fadeIn() + scaleIn(
                            animationSpec = androidx.compose.animation.core.spring(
                                dampingRatio = 0.45f,
                                stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                            ),
                            initialScale = 0.6f
                        ),
                        exit = fadeOut() + scaleOut()
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            onClick = { uriHandler.openUri("https://github.com/sltrtn") }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            androidx.compose.ui.graphics.Brush.linearGradient(MelukoTheme.colors.sunrise.reversed()),
                                            MaterialShapes.SoftBurst.toShape()
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "S",
                                        color = Color(0xFF2A1100),
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }
                                Spacer(Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Slater",
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Text(
                                        text = "The one who started it all.",
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "github.com/sltrtn",
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }

                    // Stay silent until 3 taps away, the way Android's developer options do.
                    if (!showSlater && easterEggTaps in 4..6) {
                        val left = 7 - easterEggTaps
                        Text(
                            text = "You're $left ${if (left == 1) "tap" else "taps"} away…",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.labelSmall
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

    // ── Delete Account Dialog ─────────────────────────────────────────────────
    if (showDeleteAccountConfirm) {
        AlertDialog(
            onDismissRequest = { if (!isUpdating) showDeleteAccountConfirm = false },
            title = {
                Text(
                    text = "Delete Account?",
                    fontFamily = DefaultFontFamily,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "This permanently deletes your profile, streaks, and tribe memberships. This can't be undone.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (isUpdating) {
                        Spacer(Modifier.height(4.dp))
                        LoadingIndicator(modifier = Modifier.size(40.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        accountViewModel.deleteAccount(context) {
                            showDeleteAccountConfirm = false
                            authViewModel.signOut()
                        }
                    },
                    enabled = !isUpdating
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteAccountConfirm = false },
                    enabled = !isUpdating
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold, fontFamily = DefaultFontFamily)
                }
            }
        )
    }

    // ── Main Screen ───────────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {
        if (!isLoaded) {
            MelukoLoading()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
            ) {
                ScreenHeader(title = "Account")

                ProfileHero(
                    displayName = displayName,
                    username = username,
                    bio = bio,
                    photoUrl = photoUrl,
                    tribeName = activeTribes.firstOrNull()?.name,
                    onEdit = {
                        editName = displayName
                        editBio = bio
                        pickedImageUri = null
                        showEditProfile = true
                    },
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .staggeredEnter(0)
                )

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .staggeredEnter(1),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatTile(
                        icon = Icons.Rounded.LocalFireDepartment,
                        polygon = MaterialShapes.Sunny,
                        value = streakDays,
                        label = "Day streak",
                        container = MaterialTheme.colorScheme.primaryContainer,
                        content = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .weight(1f)
                            .celebrateBurst(rememberIncreaseTrigger(streakDays))
                    )
                    StatTile(
                        icon = Icons.Rounded.EmojiEvents,
                        polygon = MaterialShapes.SoftBurst,
                        value = bestStreak,
                        label = "Best streak",
                        container = MaterialTheme.colorScheme.tertiaryContainer,
                        content = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        icon = Icons.Rounded.WbSunny,
                        polygon = MaterialShapes.Cookie9Sided,
                        value = totalWakes,
                        label = "Wake-ups",
                        container = MaterialTheme.colorScheme.secondaryContainer,
                        content = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(28.dp))
                SectionHeader("Settings", Modifier.padding(horizontal = 24.dp))
                Spacer(Modifier.height(10.dp))
                MelukoCard(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                        .staggeredEnter(2)
                ) {
                    SettingsRow(
                        icon = Icons.Rounded.Edit,
                        polygon = MaterialShapes.Cookie6Sided,
                        badgeColor = MaterialTheme.colorScheme.primaryContainer,
                        badgeContent = MaterialTheme.colorScheme.onPrimaryContainer,
                        label = "Edit profile",
                        onClick = {
                            editName = displayName
                            editBio = bio
                            pickedImageUri = null
                            showEditProfile = true
                        }
                    )
                    Hairline(Modifier.padding(start = 72.dp))
                    SettingsRow(
                        icon = Icons.Rounded.Alarm,
                        polygon = MaterialShapes.Clover4Leaf,
                        badgeColor = MaterialTheme.colorScheme.secondaryContainer,
                        badgeContent = MaterialTheme.colorScheme.onSecondaryContainer,
                        label = "Alarm tone",
                        subtitle = alarmToneTitle
                            ?: if (alarmToneUri != null) "Custom tone" else "Default alarm",
                        onClick = { showToneSheet = true }
                    )
                    Hairline(Modifier.padding(start = 72.dp))
                    SettingsRow(
                        icon = Icons.Rounded.Info,
                        polygon = MaterialShapes.Pill,
                        badgeColor = MaterialTheme.colorScheme.tertiaryContainer,
                        badgeContent = MaterialTheme.colorScheme.onTertiaryContainer,
                        label = "About Meluko",
                        onClick = { showAbout = true }
                    )
                }

                Spacer(Modifier.height(28.dp))
                SectionHeader("Account", Modifier.padding(horizontal = 24.dp))
                Spacer(Modifier.height(10.dp))
                MelukoCard(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                        .staggeredEnter(3)
                ) {
                    SettingsRow(
                        icon = Icons.AutoMirrored.Rounded.Logout,
                        polygon = MaterialShapes.Circle,
                        badgeColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        badgeContent = MaterialTheme.colorScheme.onSurface,
                        label = "Sign out",
                        onClick = { authViewModel.signOut() }
                    )
                    Hairline(Modifier.padding(start = 72.dp))
                    SettingsRow(
                        icon = Icons.Rounded.DeleteForever,
                        polygon = MaterialShapes.Circle,
                        badgeColor = MaterialTheme.colorScheme.errorContainer,
                        badgeContent = MaterialTheme.colorScheme.onErrorContainer,
                        label = "Delete account",
                        labelColor = MaterialTheme.colorScheme.error,
                        onClick = { showDeleteAccountConfirm = true }
                    )
                }

                Spacer(Modifier.height(20.dp))
                Text(
                    text = "Meluko ${com.alarm.meluko.BuildConfig.VERSION_NAME} · wake up together",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    style = MaterialTheme.typography.labelSmall
                )

                Spacer(Modifier.height(128.dp))
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun ProfileHero(
    displayName: String,
    username: String,
    bio: String,
    photoUrl: String?,
    tribeName: String?,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = MelukoTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(
                Brush.linearGradient(
                    listOf(
                        c.auroraCool.copy(alpha = if (c.isDark) 0.45f else 0.35f),
                        c.auroraWarm.copy(alpha = if (c.isDark) 0.35f else 0.40f)
                    )
                )
            )
            .border(1.dp, c.glassBorder, MaterialTheme.shapes.extraLarge)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            UserAvatar(
                photoUrl = photoUrl,
                name = displayName,
                size = 96.dp,
                shape = MaterialShapes.Cookie12Sided.toShape()
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = displayName.ifBlank { "You" },
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.headlineSmall
            )
            if (username.isNotBlank()) {
                Text(
                    text = "@$username",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (bio.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = bio,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (tribeName != null) {
                    Surface(shape = CircleShape, color = c.glassTint) {
                        Text(
                            text = tribeName,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
                FilledTonalButton(onClick = onEdit, modifier = Modifier.pressScale()) {
                    Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Edit", fontWeight = FontWeight.ExtraBold, fontFamily = DefaultFontFamily)
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    polygon: androidx.graphics.shapes.RoundedPolygon,
    badgeColor: Color,
    badgeContent: Color,
    label: String,
    subtitle: String? = null,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShapeIconBadge(
                icon = icon,
                polygon = polygon,
                container = badgeColor,
                content = badgeContent,
                size = 40.dp,
                iconSize = 20.dp
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = labelColor,
                    style = MaterialTheme.typography.titleMedium
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AccountPreview() {
    MelukoTheme { AccountScreen() }
}
