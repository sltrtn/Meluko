package com.alarm.meluko.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alarm.meluko.ui.theme.DefaultFontFamily
import com.alarm.meluko.ui.theme.BodyFontFamily
import com.alarm.meluko.ui.theme.MelukoTheme

// ─────────────────────────────────────────────────────────────────────────────
//  Models — replace with Firestore flows later
// ─────────────────────────────────────────────────────────────────────────────
enum class WakeStatus { Awake, Sleeping, Missed }

data class TribeMember(
    val uid:        String = "",
    val name:       String,
    val photoUrl:   String? = null,
    val isYou:      Boolean,
    val isAdmin:    Boolean = false,
    val streakDays: Int,
    val status:     WakeStatus,
    val wakeTime:   String? = null
)

data class TribeProgress(
    val id:          String,
    val name:        String,
    val photoUrl:    String?,
    val awakeCount:  Int,
    val totalCount:  Int
)

// Sample data has been moved to SocialViewModel for fallback

// ─────────────────────────────────────────────────────────────────────────────
//  SocialScreen
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SocialScreen(
    socialViewModel: SocialViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    onNavigateToTribe: (String) -> Unit = {}
) {
    val uiState by socialViewModel.uiState.collectAsState()
    val members by socialViewModel.members.collectAsState()
    val tribes by socialViewModel.tribes.collectAsState()

    val stillSleeping  = remember(members) { members.filter { it.status == WakeStatus.Sleeping } }
    val awakeCount     = remember(members) { members.count { it.status == WakeStatus.Awake } }
    val total          = members.size.coerceAtLeast(1) // avoid division by zero
    val youRank        = remember(members) { members.indexOfFirst { it.isYou } + 1 }
    val awakePercent   = awakeCount.toFloat() / total

    Surface(
        modifier = Modifier.fillMaxSize(),
        color    = MaterialTheme.colorScheme.background
    ) {
        when (val state = uiState) {
            is SocialUiState.Loading -> SocialShimmer()
            is SocialUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp,
                        fontFamily = BodyFontFamily
                    )
                }
            }
            is SocialUiState.Success -> {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
        ) {
            // ── Header ────────────────────────────────────────────────────────
            ScreenHeader("SOCIAL")

            // ── Morning Board ─────────────────────────────────────────────────
            MorningBoard(
                awakeCount   = awakeCount,
                total        = total,
                youRank      = youRank,
                awakePercent = awakePercent,
                modifier     = Modifier.padding(horizontal = 15.dp)
            )

            Spacer(Modifier.height(20.dp))

            // ── The Tribes ────────────────────────────────────────────────────
            SectionHeader(
                title    = "The Tribes",
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(10.dp))
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (tribes.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = "Find your tribe — wake up together.",
                            fontSize = 13.sp,
                            fontFamily = BodyFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                        )
                    }
                } else {
                    tribes.forEach { tribe ->
                            TribeStoryChip(
                                tribe = tribe,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pressScale()
                                    .clickable { onNavigateToTribe(tribe.id) }
                            )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Leaderboard ───────────────────────────────────────────────────
            SectionHeader(
                title    = "Leaderboard",
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(10.dp))

            if (members.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                    Text(
                        text = "No one here yet — invite your crew!",
                        fontSize = 13.sp,
                        fontFamily = BodyFontFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                    )
                }
            } else {
                members.forEachIndexed { index, member ->
                    LeaderboardRow(
                        rank     = index + 1,
                        member   = member,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }
            }

            // ── Still Sleeping ────────────────────────────────────────────────
            if (stillSleeping.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                SectionHeader(
                    title    = "Still Sleeping",
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(10.dp))
                Column(
                    modifier              = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement   = Arrangement.spacedBy(8.dp)
                ) {
                    stillSleeping.forEach { member ->
                        SleepingMemberRow(member = member)
                    }
                }
            }

            Spacer(Modifier.height(120.dp))
        }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Morning Board
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MorningBoard(
    awakeCount:   Int,
    total:        Int,
    youRank:      Int,
    awakePercent: Float,
    modifier:     Modifier = Modifier
) {
    val moodText = when {
        awakePercent >= 0.8f -> "Strong Morning!"
        awakePercent >= 0.5f -> "Mixed Start"
        else                 -> "Rough Morning"
    }
    val missedCount = total - awakeCount

    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Mood
                Text(
                    text       = moodText,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = DefaultFontFamily,
                    color      = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.height(4.dp))
                // Stats
                Text(
                    text       = "$awakeCount of $total awake  ·  $missedCount missed",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = BodyFontFamily,
                    color      = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                )
                Spacer(Modifier.height(12.dp))
                // Your rank pill
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f)
                ) {
                    Text(
                        text       = "You're #$youRank overall",
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = DefaultFontFamily,
                        color      = MaterialTheme.colorScheme.onPrimary,
                        modifier   = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }

            // Big percentage ring
            BigPercentage(
                percent = awakePercent,
                awake   = awakeCount,
                total   = total
            )
        }
    }
}

@Composable
private fun BigPercentage(percent: Float, awake: Int, total: Int) {
    Box(
        modifier         = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text       = "$awake/$total",
                fontSize   = 18.sp,
                fontWeight = FontWeight.Black,
                fontFamily = DefaultFontFamily,
                color      = MaterialTheme.colorScheme.onPrimary,
                lineHeight = 20.sp
            )
            Text(
                text       = "awake",
                fontSize   = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = DefaultFontFamily,
                color      = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                letterSpacing = 0.5.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Tribe Story Chip
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TribeStoryChip(
    tribe: TribeProgress,
    modifier: Modifier = Modifier
) {
    val percent = tribe.awakeCount.toFloat() / tribe.totalCount
    val chipColor = when {
        percent >= 0.8f -> MaterialTheme.colorScheme.tertiaryContainer
        percent >= 0.5f -> MaterialTheme.colorScheme.secondaryContainer
        else            -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val textColor = when {
        percent >= 0.8f -> MaterialTheme.colorScheme.onTertiaryContainer
        percent >= 0.5f -> MaterialTheme.colorScheme.onSecondaryContainer
        else            -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = chipColor
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TribeAvatar(
                photoUrl = tribe.photoUrl,
                name     = tribe.name,
                size     = 32.dp
            )
            Column {
                Text(
                    text       = tribe.name,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = DefaultFontFamily,
                    color      = textColor
                )
                Text(
                    text       = "${tribe.awakeCount}/${tribe.totalCount} awake",
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = BodyFontFamily,
                    color      = textColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Leaderboard Row
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun LeaderboardRow(
    rank:     Int,
    member:   TribeMember,
    modifier: Modifier = Modifier
) {
    val medal = when (rank) { 1 -> "1st"; 2 -> "2nd"; 3 -> "3rd"; else -> null }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        color    = if (member.isYou)
                       MaterialTheme.colorScheme.primaryContainer
                   else
                       MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Rank / medal
            Box(
                modifier         = Modifier.width(28.dp),
                contentAlignment = Alignment.Center
            ) {
                if (medal != null) {
                    Text(medal, fontSize = 20.sp)
                } else {
                    Text(
                        text       = "$rank",
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = DefaultFontFamily,
                        color      = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Avatar
            UserAvatar(
                photoUrl = member.photoUrl,
                name     = member.name,
                size     = 38.dp
            )

            // Name + status
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = if (member.isYou) "You" else member.name,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = DefaultFontFamily,
                    color      = if (member.isYou) MaterialTheme.colorScheme.onPrimaryContainer
                                 else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text       = when (member.status) {
                        WakeStatus.Awake    -> "Awake · ${member.wakeTime ?: ""}"
                        WakeStatus.Sleeping -> "Sleeping"
                        WakeStatus.Missed   -> "Missed"
                    },
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = BodyFontFamily,
                    color      = when (member.status) {
                        WakeStatus.Awake    -> MaterialTheme.colorScheme.tertiary
                        WakeStatus.Sleeping -> MaterialTheme.colorScheme.onSurfaceVariant
                        WakeStatus.Missed   -> MaterialTheme.colorScheme.error
                    }
                )
            }

            // Streak
            Text(
                text       = "${member.streakDays}d",
                fontSize   = 13.sp,
                fontWeight = FontWeight.Black,
                fontFamily = DefaultFontFamily,
                color      = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Nudge Row
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SleepingMemberRow(member: TribeMember) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            UserAvatar(
                photoUrl = member.photoUrl,
                name     = member.name,
                size     = 36.dp
            )
            Column {
                Text(
                    text       = member.name,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = DefaultFontFamily,
                    color      = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text       = "Still sleeping",
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = BodyFontFamily,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Section header
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text          = title,
        fontSize      = 13.sp,
        fontWeight    = FontWeight.Bold,
        fontFamily    = DefaultFontFamily,
        color         = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.5.sp,
        modifier      = modifier
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Shimmer skeleton
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SocialShimmer(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
    ) {
        Spacer(Modifier.height(20.dp))
        ScreenHeader("SOCIAL")
        Spacer(Modifier.height(12.dp))
        ShimmerCard(
            modifier = Modifier.padding(horizontal = 15.dp),
            height = 160.dp
        )
        Spacer(Modifier.height(20.dp))
        ShimmerLine(
            width = 100.dp,
            height = 14.dp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(12.dp))
        ShimmerCard(
            modifier = Modifier.padding(horizontal = 20.dp),
            height = 52.dp
        )
        Spacer(Modifier.height(8.dp))
        ShimmerCard(
            modifier = Modifier.padding(horizontal = 20.dp),
            height = 52.dp
        )
        Spacer(Modifier.height(24.dp))
        ShimmerLine(
            width = 100.dp,
            height = 14.dp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(12.dp))
        repeat(5) {
            ShimmerLeaderboardRow(modifier = Modifier.padding(horizontal = 20.dp))
        }
        Spacer(Modifier.height(120.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Preview
// ─────────────────────────────────────────────────────────────────────────────
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SocialPreview() {
    MelukoTheme { SocialScreen() }
}
