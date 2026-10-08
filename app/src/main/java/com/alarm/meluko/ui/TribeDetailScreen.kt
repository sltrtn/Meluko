package com.alarm.meluko.ui

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alarm.meluko.data.FirestoreCollection
import com.alarm.meluko.data.FirestoreField
import com.alarm.meluko.ui.theme.DefaultFontFamily
import com.alarm.meluko.ui.theme.BodyFontFamily
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

    // Real-time members from SocialViewModel
    val members by socialViewModel.members.collectAsState()

    // Proposal state from ProposalViewModel
    val activeProposal by proposalViewModel.activeProposal.collectAsState()
    val vetoesUsed by proposalViewModel.vetoesUsed.collectAsState()

    val hasActiveProposal = activeProposal != null
    val canVeto = vetoesUsed < 2 // Max 2 vetos lifetime per tribe
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

    // Propose sheet
    if (showProposeSheet) {
        ProposeAlarmBottomSheet(
            tribeId = tribeId,
            proposalViewModel = proposalViewModel,
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
                onConfirmVeto = { onSuccess ->
                    proposalViewModel.castVeto(
                        tribeId = tribeId,
                        proposalId = proposal.id,
                        onSuccess = onSuccess
                    )
                }
            )
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.systemBars,
        floatingActionButton = {
            // Show propose FAB only when there is no active pending proposal
            if (!hasActiveProposal) {
                ExtendedFloatingActionButton(
                    onClick = { showProposeSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Filled.Alarm, "Propose") },
                    text = {
                        Text(
                            "PROPOSE ALARM",
                            fontWeight = FontWeight.Black,
                            fontFamily = DefaultFontFamily
                        )
                    }
                )
            }
        }
    ) { paddingValues ->
        if (!tribeLoaded) {
            if (tribeError != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = tribeError!!,
                            fontSize = 14.sp,
                            fontFamily = BodyFontFamily,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { /* retry would re-trigger listener */ }) {
                            Text("Retry", fontFamily = DefaultFontFamily)
                        }
                    }
                }
            } else {
                TribeDetailShimmer(modifier = Modifier.padding(paddingValues))
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Top Bar
            TopAppBar(
                title = {
                    Text(
                        text = tribeName.ifBlank { "Tribe" },
                        fontWeight = FontWeight.Black,
                        fontFamily = DefaultFontFamily,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )

            // Hero Alarm Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "$tribeMemberCount Members",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = DefaultFontFamily,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Alarm: $tribeAlarmTime",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = DefaultFontFamily,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Active Proposal Card (real data)
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
                        vetoesLeft = 2 - vetoesUsed,
                        vetoCount = proposal.vetoCount,
                        onVetoClick = { showVetoSheet = true },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Tribe Leaderboard
            Text(
                text = "${tribeName.ifBlank { "Tribe" }} Leaderboard",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = DefaultFontFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(10.dp))

            if (members.isEmpty()) {
                Text(
                    text = "No members yet. Invite your friends!",
                    fontSize = 13.sp,
                    fontFamily = BodyFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            } else {
                members.forEachIndexed { index, member ->
                    TribeDetailLeaderboardRow(
                        rank = index + 1,
                        name = member.name,
                        photoUrl = member.photoUrl,
                        streak = member.streakDays,
                        isYou = member.isYou,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
private fun TribeDetailLeaderboardRow(
    rank: Int,
    name: String,
    photoUrl: String?,
    streak: Int,
    isYou: Boolean,
    modifier: Modifier = Modifier
) {
    val medal = when (rank) { 1 -> "1st"; 2 -> "2nd"; 3 -> "3rd"; else -> null }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isYou) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.width(28.dp),
                contentAlignment = Alignment.Center
            ) {
                if (medal != null) Text(medal, fontSize = 20.sp)
                else Text(
                    "$rank",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = DefaultFontFamily
                )
            }
            UserAvatar(photoUrl = photoUrl, name = name, size = 38.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isYou) "$name (You)" else name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = DefaultFontFamily,
                    color = if (isYou) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "${streak}d",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                fontFamily = DefaultFontFamily,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Shimmer skeleton
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TribeDetailShimmer(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShimmerCircle(size = 24.dp)
            Spacer(Modifier.width(8.dp))
            ShimmerLine(width = 120.dp, height = 18.dp)
        }
        Spacer(Modifier.height(16.dp))
        ShimmerCard(
            modifier = Modifier.padding(horizontal = 20.dp),
            height = 120.dp
        )
        Spacer(Modifier.height(24.dp))
        ShimmerLine(
            width = 140.dp,
            height = 14.dp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(12.dp))
        repeat(5) {
            ShimmerLeaderboardRow(modifier = Modifier.padding(horizontal = 20.dp))
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun ProposalCard(
    proposerName: String,
    proposedTime: String,
    countdownText: String,
    canVeto: Boolean,
    alreadyVetoed: Boolean,
    vetoesLeft: Int,
    vetoCount: Int,
    onVetoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Proposal",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = DefaultFontFamily,
                    color = MaterialTheme.colorScheme.error
                )
                if (countdownText.isNotBlank()) {
                    Text(
                        text = countdownText,
                        fontSize = 10.sp,
                        fontFamily = BodyFontFamily,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "$proposerName wants to move the alarm to $proposedTime",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = DefaultFontFamily
            )
            if (vetoCount > 0) {
                Text(
                    text = "$vetoCount veto${if (vetoCount == 1) "" else "es"} cast so far.",
                    fontSize = 11.sp,
                    fontFamily = BodyFontFamily,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Text(
                text = "If nobody vetos within the window, this will be accepted automatically.",
                fontSize = 11.sp,
                fontFamily = BodyFontFamily,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.height(16.dp))

            when {
                alreadyVetoed -> {
                    Text(
                        text = "You already vetoed this proposal.",
                        fontSize = 12.sp,
                        fontFamily = BodyFontFamily,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                canVeto -> {
                    Button(
                        onClick = onVetoClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "VETO  ($vetoesLeft veto${if (vetoesLeft == 1) "" else "s"} remaining)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = DefaultFontFamily
                        )
                    }
                }
                else -> {
                    Text(
                        text = "You have no vetos remaining.",
                        fontSize = 12.sp,
                        fontFamily = BodyFontFamily,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
