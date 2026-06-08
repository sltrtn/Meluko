package com.alarm.meluko.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alarm.meluko.ui.theme.DefaultFontFamily
import com.alarm.meluko.ui.theme.BodyFontFamily
import com.alarm.meluko.ui.theme.MelukoTheme
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.Star
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TribeDetailScreen(
    tribeId: String,
    onBack: () -> Unit,
    socialViewModel: SocialViewModel = hiltViewModel()
) {
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val coroutineScope = rememberCoroutineScope()

    // Real-time tribe metadata
    var tribeName by remember { mutableStateOf(tribeId) }
    var tribeAlarmTime by remember { mutableStateOf("--:--") }
    var tribeMemberCount by remember { mutableStateOf(0) }

    // Real-time members
    val members by socialViewModel.members.collectAsState()

    // Current user's streak for veto eligibility
    val userStreak = members.find { it.isYou }?.streakDays ?: 0
    val hasVetoPower = userStreak >= 7

    var isFollowedTone by remember { mutableStateOf(false) }
    var showProposeSheet by remember { mutableStateOf(false) }
    var showVetoSheet by remember { mutableStateOf(false) }
    val hasActiveProposal = false // Will be wired in next phase

    // Load tribe metadata
    LaunchedEffect(tribeId) {
        db.collection("tribes").document(tribeId)
            .addSnapshotListener { snap, _ ->
                if (snap != null && snap.exists()) {
                    tribeName = snap.getString("name") ?: tribeId
                    tribeAlarmTime = snap.getString("alarmTime") ?: "--:--"
                    tribeMemberCount = snap.getLong("memberCount")?.toInt() ?: 0
                }
            }
    }

    if (showProposeSheet) {
        ProposeAlarmBottomSheet(
            onDismiss = { showProposeSheet = false },
            onPropose = { proposedTime -> 
                // TODO: Wire to Firestore
            }
        )
    }

    if (showVetoSheet) {
        VetoBottomSheet(
            vetosLeft = 2,
            onDismiss = { showVetoSheet = false },
            onConfirmVeto = { 
                showVetoSheet = false
                // TODO: Wire to Firestore to cast VETO
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (!hasActiveProposal) {
                ExtendedFloatingActionButton(
                    onClick = { showProposeSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Filled.Star, "Propose") },
                    text = { 
                        Text("PROPOSE ALARM", fontWeight = FontWeight.Black, fontFamily = DefaultFontFamily) 
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
        ) {
            // ── Top Bar ───────────────────────────────────────────────────────
            TopAppBar(
                title = {
                    Text(
                        text = tribeName,
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

            // ── Hero Card: Tribe Stats ───────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "$tribeMemberCount Members",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = DefaultFontFamily,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Next Alarm: $tribeAlarmTime",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = DefaultFontFamily,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        
                    }
                }
            }

            // ── Active Proposal Card ──────────────────────────────────────────

            if (hasActiveProposal) {
                ProposalCard(
                    proposerName = "A tribe member",
                    proposedTime = "05:30 AM",
                    agreeCount = 0,
                    disagreeCount = 0,
                    canVeto = hasVetoPower,
                    onVetoClick = { showVetoSheet = true },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            // ── Alarm Tone Selection ──────────────────────────────────────────
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tribe Alarm Tone",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = DefaultFontFamily,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Spotify: Morning Motivation",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = DefaultFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isFollowedTone,
                        onCheckedChange = { isFollowedTone = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
            
            Text(
                text = "Turn this on to use this tribe's selected playlist for your daily alarm.",
                fontSize = 11.sp,
                fontFamily = DefaultFontFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 36.dp)
            )

            Spacer(Modifier.height(24.dp))

            // ── Tribe Leaderboard ─────────────────────────────────────────────
            Text(
                text = "Tribe Leaderboard",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = DefaultFontFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(10.dp))

            // Mock Leaderboard uses real Firestore members from SocialViewModel
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
            
            Spacer(Modifier.height(40.dp))
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
    val medal = when (rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> null }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (isYou) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
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
                else Text("$rank", fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = DefaultFontFamily)
            }
            UserAvatar(
                photoUrl = photoUrl,
                name     = name,
                size     = 38.dp
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = DefaultFontFamily,
                    color = if (isYou) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "🔥 ${streak}d",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                fontFamily = DefaultFontFamily,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ProposalCard(
    proposerName: String,
    proposedTime: String,
    agreeCount: Int,
    disagreeCount: Int,
    canVeto: Boolean,
    onVetoClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🔔 Active Proposal",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = DefaultFontFamily,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "$proposerName wants to move the alarm to $proposedTime.",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = DefaultFontFamily
            )
            
            Spacer(Modifier.height(16.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Agree ($agreeCount)", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = DefaultFontFamily)
                }
                
                if (canVeto) {
                    Button(
                        onClick = onVetoClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Veto 🔥", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = DefaultFontFamily)
                    }
                } else {
                    OutlinedButton(
                        onClick = { },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onErrorContainer)
                    ) {
                        Text("Disagree ($disagreeCount)", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = DefaultFontFamily)
                    }
                }
            }
            if (canVeto) {
                Text(
                    text = "Your 7+ day streak grants you a Veto. Use it wisely.",
                    fontSize = 10.sp,
                    fontFamily = DefaultFontFamily,
                    modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TribeDetailPreview() {
    MelukoTheme {
        TribeDetailScreen(tribeId = "Hostel D4", onBack = {})
    }
}
