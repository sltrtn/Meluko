package com.alarm.meluko.ui

import android.content.Intent
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.HowToVote
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Share
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.alarm.meluko.data.FirestoreCollection
import com.alarm.meluko.data.FirestoreField
import com.alarm.meluko.ui.components.Hairline
import com.alarm.meluko.ui.components.MelukoCard
import com.alarm.meluko.ui.components.MelukoLoading
import com.alarm.meluko.ui.components.SectionHeader
import com.alarm.meluko.ui.components.aurora
import com.alarm.meluko.ui.components.glass
import com.alarm.meluko.ui.components.plural
import com.alarm.meluko.ui.components.staggeredEnter
import com.alarm.meluko.ui.theme.DefaultFontFamily
import com.alarm.meluko.ui.theme.BodyFontFamily
import com.alarm.meluko.ui.theme.MelukoTheme
import com.alarm.meluko.util.to12HourDisplay
import com.alarm.meluko.util.to12HourParts
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Date
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TribeDetailScreen(
    tribeId: String,
    onBack: () -> Unit,
    socialViewModel: SocialViewModel = hiltViewModel(),
    proposalViewModel: ProposalViewModel = hiltViewModel()
) {
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Real-time tribe metadata
    var tribeLoaded by remember { mutableStateOf(false) }
    var tribeError by remember { mutableStateOf<String?>(null) }
    var tribeName by remember { mutableStateOf("") }
    var tribeAlarmTime by remember { mutableStateOf("--:--") }
    var tribeMemberCount by remember { mutableStateOf(0) }
    var tribeInviteCode by remember { mutableStateOf("") }

    // Real-time members from SocialViewModel
    val members by socialViewModel.members.collectAsState()
    val isAdmin = members.any { it.isYou && it.isAdmin }
    val isTribeActionInProgress by socialViewModel.isTribeActionInProgress.collectAsState()

    val context = LocalContext.current
    var showLeaveConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var memberPendingRemoval by remember { mutableStateOf<TribeMember?>(null) }

    // Proposal state from ProposalViewModel
    val activeProposal by proposalViewModel.activeProposal.collectAsState()
    val vetoesUsed by proposalViewModel.vetoesUsed.collectAsState()

    val hasActiveProposal = activeProposal != null
    val isMyProposal = activeProposal?.proposedBy == uid
    val canVeto = vetoesUsed < ProposalViewModel.MAX_VETOES && !isMyProposal
    val alreadyVetoed = activeProposal?.vetoedBy?.contains(uid) == true

    var showProposeSheet by remember { mutableStateOf(false) }
    var showVetoSheet by remember { mutableStateOf(false) }

    // Countdown ticker for proposal expiry
    var countdownText by remember { mutableStateOf("") }
    LaunchedEffect(activeProposal?.expiresAt) {
        val expiresAt = activeProposal?.expiresAt ?: return@LaunchedEffect
        while (isActive) {
            val msLeft = expiresAt.time - Date().time
            if (msLeft <= 0) {
                countdownText = "Accepting..."
                break
            }
            val hours = TimeUnit.MILLISECONDS.toHours(msLeft)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(msLeft) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(msLeft) % 60
            countdownText = if (hours > 0) {
                "${hours}h ${minutes}m remaining"
            } else if (minutes > 0) {
                "${minutes}m ${seconds}s remaining"
            } else {
                "${seconds}s remaining"
            }
            delay(1000L)
        }
    }

    // Load tribe data and start listening to proposals with cleanup
    DisposableEffect(tribeId) {
        val tribeRegistration: ListenerRegistration = db.collection(FirestoreCollection.TRIBES).document(tribeId)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.w("TribeDetail", "Tribe listener error", error)
                    tribeError = "Failed to load tribe data"
                    return@addSnapshotListener
                }
                if (snap != null) {
                    tribeLoaded = true
                    if (snap.exists()) {
                        tribeName = snap.getString(FirestoreField.NAME) ?: tribeId
                        tribeAlarmTime = snap.getString(FirestoreField.ALARM_TIME) ?: "--:--"
                        tribeMemberCount = snap.getLong(FirestoreField.MEMBER_COUNT)?.toInt() ?: 0
                        tribeInviteCode = snap.getString(FirestoreField.INVITE_CODE) ?: ""
                    }
                }
            }
        socialViewModel.loadMembersForTribe(tribeId)
        proposalViewModel.listenToTribeProposals(tribeId)

        onDispose {
            tribeRegistration.remove()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val proposalError by proposalViewModel.error.collectAsState()

    LaunchedEffect(proposalError) {
        proposalError?.let {
            snackbarHostState.showSnackbar(it)
            proposalViewModel.clearError()
        }
    }

    val tribeActionError by socialViewModel.tribeActionError.collectAsState()
    LaunchedEffect(tribeActionError) {
        tribeActionError?.let {
            snackbarHostState.showSnackbar(it)
            socialViewModel.clearTribeActionError()
        }
    }

    // Leave tribe confirmation
    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = { Text("Leave Tribe?", fontFamily = DefaultFontFamily, fontWeight = FontWeight.Bold) },
            text = { Text("You'll stop following $tribeName's alarm and lose your streak here.", fontFamily = BodyFontFamily) },
            confirmButton = {
                TextButton(onClick = {
                    showLeaveConfirm = false
                    socialViewModel.leaveTribe(tribeId, onSuccess = onBack)
                }) {
                    Text("Leave", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirm = false }) { Text("Cancel", fontFamily = DefaultFontFamily) }
            }
        )
    }

    // Delete tribe confirmation
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Tribe?", fontFamily = DefaultFontFamily, fontWeight = FontWeight.Bold) },
            text = { Text("This permanently deletes $tribeName. This can't be undone.", fontFamily = BodyFontFamily) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    socialViewModel.deleteTribe(tribeId, onSuccess = onBack)
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel", fontFamily = DefaultFontFamily) }
            }
        )
    }

    // Remove member confirmation
    memberPendingRemoval?.let { target ->
        AlertDialog(
            onDismissRequest = { memberPendingRemoval = null },
            title = { Text("Remove ${target.name}?", fontFamily = DefaultFontFamily, fontWeight = FontWeight.Bold) },
            text = { Text("They'll be removed from $tribeName and stop following its alarm.", fontFamily = BodyFontFamily) },
            confirmButton = {
                TextButton(onClick = {
                    memberPendingRemoval = null
                    socialViewModel.removeMember(tribeId, target.uid, onSuccess = {})
                }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { memberPendingRemoval = null }) { Text("Cancel", fontFamily = DefaultFontFamily) }
            }
        )
    }

    // Propose sheet
    if (showProposeSheet) {
        ProposeAlarmBottomSheet(
            tribeId = tribeId,
            proposalViewModel = proposalViewModel,
            initialTime = tribeAlarmTime,
            onDismiss = { showProposeSheet = false }
        )
    }

    // Veto sheet
    if (showVetoSheet) {
        val proposal = activeProposal
        if (proposal != null) {
            VetoBottomSheet(
                vetosLeft = 2 - vetoesUsed,
                onDismiss = { showVetoSheet = false },
                onConfirmVeto = { onSuccess, onError ->
                    proposalViewModel.castVeto(
                        onError = onError,
                        tribeId = tribeId,
                        proposalId = proposal.id,
                        onSuccess = onSuccess
                    )
                }
            )
        }
    }

    LaunchedEffect(isAdmin, tribeInviteCode) {
        if (isAdmin && tribeInviteCode.isNotBlank()) socialViewModel.ensureInvite(tribeId, tribeInviteCode)
    }

    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    var codeCopied by remember { mutableStateOf(false) }
    LaunchedEffect(codeCopied) {
        if (codeCopied) {
            snackbarHostState.showSnackbar("Invite code copied")
            codeCopied = false
        }
    }
    val shareInvite = {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                "Wake up with me on Meluko! Join \"$tribeName\" with invite code $tribeInviteCode"
            )
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share invite code"))
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .aurora(),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.systemBars,
        floatingActionButton = {
            if (!hasActiveProposal && tribeLoaded) {
                ExtendedFloatingActionButton(
                    onClick = { showProposeSheet = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = MaterialTheme.shapes.large,
                    icon = { Icon(Icons.Rounded.Alarm, null) },
                    text = { Text("Propose alarm", fontWeight = FontWeight.ExtraBold, fontFamily = DefaultFontFamily) },
                    modifier = Modifier.fabPressScale()
                )
            }
        }
    ) { paddingValues ->
        if (!tribeLoaded) {
            if (tribeError != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = tribeError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onBack) { Text("Go back", fontFamily = DefaultFontFamily) }
                    }
                }
            } else {
                MelukoLoading(Modifier.padding(paddingValues))
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Top bar: glass round buttons over the aurora ───────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
                Spacer(Modifier.weight(1f))
                if (tribeInviteCode.isNotBlank()) {
                    GlassIconButton(Icons.Rounded.Share, "Share invite code") { shareInvite() }
                }
            }

            // ── Identity ──────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .staggeredEnter(0),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TribeAvatar(photoUrl = null, name = tribeName.ifBlank { "Tribe" }, size = 88.dp)
                Spacer(Modifier.height(14.dp))
                Text(
                    text = tribeName.ifBlank { "Tribe" },
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = plural(tribeMemberCount, "member"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(22.dp))

            // ── Alarm + invite ────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(IntrinsicSize.Min)
                    .staggeredEnter(1),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val (alarmClock, alarmAmPm) = to12HourParts(tribeAlarmTime)
                Box(
                    Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .clip(MaterialTheme.shapes.large)
                        .background(Brush.linearGradient(MelukoTheme.colors.sunrise.reversed()))
                        .padding(18.dp)
                ) {
                    Column {
                        Text(
                            "TRIBE ALARM",
                            color = Color(0xFF2A1100).copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                alarmClock,
                                color = Color(0xFF2A1100),
                                style = MaterialTheme.typography.headlineLarge
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                alarmAmPm,
                                color = Color(0xFF2A1100).copy(alpha = 0.8f),
                                modifier = Modifier.padding(bottom = 6.dp),
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                }
                if (tribeInviteCode.isNotBlank()) {
                    MelukoCard(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        onClick = {
                            clipboard.setText(androidx.compose.ui.text.AnnotatedString(tribeInviteCode))
                            codeCopied = true
                        }
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Text(
                                "INVITE CODE",
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelMedium
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                tribeInviteCode,
                                letterSpacing = 2.sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.ContentCopy, null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Tap to copy",
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }

            // ── Active proposal ───────────────────────────────────────────────
            AnimatedVisibility(
                visible = hasActiveProposal && activeProposal != null,
                enter = scaleIn(animationSpec = MotionSpec.cardEnter, initialScale = 0.9f) +
                        fadeIn(animationSpec = MotionSpec.touch)
            ) {
                activeProposal?.let { proposal ->
                    ProposalCard(
                        proposerName = proposal.proposedByName,
                        proposedTime = proposal.proposedTime,
                        countdownText = countdownText,
                        canVeto = canVeto && !alreadyVetoed,
                        alreadyVetoed = alreadyVetoed,
                        isMine = isMyProposal,
                        vetoesLeft = 2 - vetoesUsed,
                        onVetoClick = { showVetoSheet = true },
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp)
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Leaderboard ───────────────────────────────────────────────────
            SectionHeader("Leaderboard", Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.height(10.dp))
            if (members.isEmpty()) {
                EmptyHint("No members yet. Share the invite code!", Modifier.padding(horizontal = 20.dp))
            } else {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    members.forEachIndexed { index, member ->
                        LeaderboardRow(
                            rank = index + 1,
                            member = member,
                            modifier = Modifier.staggeredEnter(2 + index),
                            trailing = if (isAdmin && !member.isYou) {
                                {
                                    var menuOpen by remember { mutableStateOf(false) }
                                    Box {
                                        IconButton(onClick = { menuOpen = true }) {
                                            Icon(Icons.Rounded.MoreVert, contentDescription = "Manage ${member.name}")
                                        }
                                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                            if (!member.isAdmin) {
                                                DropdownMenuItem(
                                                    text = { Text("Make admin", fontFamily = DefaultFontFamily) },
                                                    leadingIcon = { Icon(Icons.Rounded.Shield, null) },
                                                    onClick = {
                                                        menuOpen = false
                                                        socialViewModel.makeAdmin(tribeId, member.uid)
                                                    }
                                                )
                                            }
                                            DropdownMenuItem(
                                                text = { Text("Remove from tribe", color = MaterialTheme.colorScheme.error, fontFamily = DefaultFontFamily) },
                                                leadingIcon = { Icon(Icons.Rounded.PersonRemove, null, tint = MaterialTheme.colorScheme.error) },
                                                onClick = {
                                                    menuOpen = false
                                                    memberPendingRemoval = member
                                                }
                                            )
                                        }
                                    }
                                }
                            } else null
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Manage ────────────────────────────────────────────────────────
            SectionHeader("Manage", Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.height(10.dp))
            MelukoCard(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
            ) {
                if (tribeInviteCode.isNotBlank()) {
                    TribeActionRow(
                        icon = Icons.Rounded.PersonAdd,
                        label = "Invite friends",
                        enabled = true,
                        onClick = { shareInvite() }
                    )
                    Hairline(Modifier.padding(horizontal = 16.dp))
                }
                if (isAdmin && tribeMemberCount <= 1) {
                    TribeActionRow(
                        icon = Icons.Rounded.DeleteForever,
                        label = "Delete tribe",
                        destructive = true,
                        enabled = !isTribeActionInProgress,
                        onClick = { showDeleteConfirm = true }
                    )
                } else {
                    TribeActionRow(
                        icon = Icons.AutoMirrored.Rounded.ExitToApp,
                        label = "Leave tribe",
                        destructive = true,
                        enabled = !isTribeActionInProgress,
                        onClick = { showLeaveConfirm = true }
                    )
                }
            }

            Spacer(Modifier.height(120.dp))
        }
    }
}

@Composable
private fun GlassIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .glass(CircleShape, hazeState = null)
            .clickable(onClick = onClick)
            .pressScale(),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun TribeActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    val tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
        onClick = onClick,
        enabled = enabled
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            Text(
                text = label,
                color = tint,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium
            )
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun ProposalCard(
    proposerName: String,
    proposedTime: String,
    countdownText: String,
    canVeto: Boolean,
    alreadyVetoed: Boolean,
    isMine: Boolean,
    vetoesLeft: Int,
    onVetoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val onCard = MaterialTheme.colorScheme.onTertiaryContainer
    MelukoCard(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.HowToVote, null, tint = onCard, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "PROPOSAL",
                    color = onCard.copy(alpha = 0.8f),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium
                )
                if (countdownText.isNotBlank()) {
                    Surface(shape = CircleShape, color = onCard.copy(alpha = 0.12f)) {
                        Text(
                            countdownText,
                            color = onCard,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "$proposerName wants to move the alarm to",
                color = onCard,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = to12HourDisplay(proposedTime),
                fontSize = 40.sp,
                lineHeight = 44.sp,
                fontWeight = FontWeight.Black,
                fontFamily = DefaultFontFamily,
                color = onCard
            )
            Text(
                text = "It goes through automatically when the timer ends. One veto cancels it.",
                color = onCard.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(16.dp))
            when {
                isMine -> Text(
                    "This is your proposal. Waiting on the tribe.",
                    color = onCard,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleSmall
                )
                alreadyVetoed -> Text(
                    "You vetoed this one.",
                    color = onCard,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleSmall
                )
                canVeto -> Button(
                    onClick = onVetoClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .pressScale(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(
                        "Veto · $vetoesLeft left",
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = DefaultFontFamily
                    )
                }
                else -> Text(
                    "You're out of vetoes.",
                    color = onCard.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
