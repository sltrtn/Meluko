package com.alarm.meluko.ui

import androidx.compose.foundation.background
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
import com.alarm.meluko.ui.theme.MelukoTheme

// ─────────────────────────────────────────────────────────────────────────────
//  Models — replace with Firestore flows later
// ─────────────────────────────────────────────────────────────────────────────
enum class WakeStatus { Awake, Sleeping, Missed }

data class TribeMember(
    val name:       String,
    val isYou:      Boolean,
    val streakDays: Int,
    val status:     WakeStatus,
    val wakeTime:   String? = null
)

data class TribeProgress(
    val name:        String,
    val emoji:       String,
    val awakeCount:  Int,
    val totalCount:  Int
)

// ─────────────────────────────────────────────────────────────────────────────
//  Sample data — will be wired to Firestore in a later session
// ─────────────────────────────────────────────────────────────────────────────
private val sampleMembers = listOf(
    TribeMember("Aisha",  false, 14, WakeStatus.Awake,    "6:47 AM"),
    TribeMember("You",    true,  12, WakeStatus.Awake,    "7:28 AM"),
    TribeMember("Sachin", false,  8, WakeStatus.Awake,    "7:02 AM"),
    TribeMember("Priya",  false,  5, WakeStatus.Sleeping, null),
    TribeMember("Joe",    false,  3, WakeStatus.Missed,   null),
    TribeMember("Riya",   false,  2, WakeStatus.Awake,    "7:15 AM"),
)

private val sampleTribes = listOf(
    TribeProgress("Hostel D4",   "🏠",  6, 8),
    TribeProgress("MIT CS 4th",  "📚",  5, 12),
    TribeProgress("Early Birds", "🌅",  3, 5),
)

// ─────────────────────────────────────────────────────────────────────────────
//  SocialScreen
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SocialScreen() {
    val leaderboard    = remember { sampleMembers.sortedByDescending { it.streakDays } }
    val stillSleeping  = remember { sampleMembers.filter { it.status == WakeStatus.Sleeping } }
    val awakeCount     = remember { sampleMembers.count { it.status == WakeStatus.Awake } }
    val total          = sampleMembers.size
    val youRank        = remember { leaderboard.indexOfFirst { it.isYou } + 1 }
    val awakePercent   = awakeCount.toFloat() / total

    Surface(
        modifier = Modifier.fillMaxSize(),
        color    = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
        ) {
            // ── Header ────────────────────────────────────────────────────────
            val today = remember {
                java.text.SimpleDateFormat("EEEE, d MMMM", java.util.Locale.getDefault())
                    .format(java.util.Date())
            }
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text          = "SOCIAL",
                    fontSize      = 18.sp,
                    fontWeight    = FontWeight.Black,
                    fontFamily    = DefaultFontFamily,
                    letterSpacing = 6.sp,
                    color         = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text          = today,
                    fontSize      = 12.sp,
                    fontWeight    = FontWeight.Bold,
                    fontFamily    = DefaultFontFamily,
                    letterSpacing = 0.5.sp,
                    color         = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── Morning Board ─────────────────────────────────────────────────
            MorningBoard(
                awakeCount   = awakeCount,
                total        = total,
                youRank      = youRank,
                awakePercent = awakePercent,
                modifier     = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(20.dp))

            // ── Tribe Stories row ─────────────────────────────────────────────
            Row(
                modifier              = Modifier
                    .padding(horizontal = 20.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                sampleTribes.forEach { tribe ->
                    TribeStoryChip(tribe = tribe)
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Leaderboard ───────────────────────────────────────────────────
            SectionHeader(
                title    = "Leaderboard",
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(10.dp))

            leaderboard.forEachIndexed { index, member ->
                LeaderboardRow(
                    rank     = index + 1,
                    member   = member,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            // ── Still Sleeping ────────────────────────────────────────────────
            if (stillSleeping.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                SectionHeader(
                    title    = "😴  Still Sleeping",
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(10.dp))
                Column(
                    modifier              = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement   = Arrangement.spacedBy(8.dp)
                ) {
                    stillSleeping.forEach { member ->
                        NudgeRow(member = member)
                    }
                }
            }

            Spacer(Modifier.height(120.dp))
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
    val (moodEmoji, moodText) = when {
        awakePercent >= 0.8f -> "☀️" to "Strong Morning!"
        awakePercent >= 0.5f -> "⚡" to "Mixed Start"
        else                 -> "💤" to "Rough Morning"
    }
    val missedCount = total - awakeCount

    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(36.dp),
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
                    text       = "$moodEmoji  $moodText",
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
                    fontFamily = DefaultFontFamily,
                    color      = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                )
                Spacer(Modifier.height(12.dp))
                // Your rank pill
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f)
                ) {
                    Text(
                        text       = "You're #$youRank overall 🔥",
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
private fun TribeStoryChip(tribe: TribeProgress) {
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
        shape = RoundedCornerShape(20.dp),
        color = chipColor
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(tribe.emoji, fontSize = 18.sp)
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
                    fontFamily = DefaultFontFamily,
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
    val medal = when (rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> null }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(20.dp),
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
            Box(
                modifier         = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        if (member.isYou) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = member.name.first().uppercaseChar().toString(),
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = DefaultFontFamily,
                    color      = if (member.isYou) MaterialTheme.colorScheme.onPrimary
                                 else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

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
                        WakeStatus.Sleeping -> "Sleeping 😴"
                        WakeStatus.Missed   -> "Missed ❌"
                    },
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = DefaultFontFamily,
                    color      = when (member.status) {
                        WakeStatus.Awake    -> MaterialTheme.colorScheme.tertiary
                        WakeStatus.Sleeping -> MaterialTheme.colorScheme.onSurfaceVariant
                        WakeStatus.Missed   -> MaterialTheme.colorScheme.error
                    }
                )
            }

            // Streak
            Text(
                text       = "🔥 ${member.streakDays}d",
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
private fun NudgeRow(member: TribeMember) {
    var nudged by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Avatar
                Box(
                    modifier         = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = member.name.first().uppercaseChar().toString(),
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = DefaultFontFamily,
                        color      = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
                        fontWeight = FontWeight.Medium,
                        fontFamily = DefaultFontFamily,
                        color      = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Nudge button — toggles to "Sent!" after tap
            FilledTonalButton(
                onClick = { nudged = true },
                enabled = !nudged,
                shape   = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text       = if (nudged) "Sent ✓" else "Nudge 👋",
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = DefaultFontFamily
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
//  Preview
// ─────────────────────────────────────────────────────────────────────────────
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SocialPreview() {
    MelukoTheme { SocialScreen() }
}
