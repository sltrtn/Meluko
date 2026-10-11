package com.alarm.meluko.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alarm.meluko.ui.components.MelukoCard
import com.alarm.meluko.ui.components.MelukoLoading
import com.alarm.meluko.ui.components.RankBadge
import com.alarm.meluko.ui.components.SectionHeader
import com.alarm.meluko.ui.components.StatusLabel
import com.alarm.meluko.ui.components.rememberCountUp
import com.alarm.meluko.ui.components.staggeredEnter
import com.alarm.meluko.ui.theme.BodyFontFamily
import com.alarm.meluko.ui.theme.DefaultFontFamily
import com.alarm.meluko.ui.theme.MelukoTheme

// ─────────────────────────────────────────────────────────────────────────────
//  Models
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

// ─────────────────────────────────────────────────────────────────────────────
//  SocialScreen
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SocialScreen(
    socialViewModel: SocialViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    onNavigateToTribe: (String) -> Unit = {},
    onJoinTribe: () -> Unit = {}
) {
    val uiState by socialViewModel.uiState.collectAsState()
    val members by socialViewModel.members.collectAsState()
    val tribes by socialViewModel.tribes.collectAsState()

    when (val state = uiState) {
        is SocialUiState.Loading -> MelukoLoading()
        is SocialUiState.Error -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                text = state.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        is SocialUiState.Success -> SocialContent(
            members = members,
            tribe = tribes.firstOrNull(),
            onNavigateToTribe = onNavigateToTribe,
            onJoinTribe = onJoinTribe
        )
    }
}

@Composable
private fun SocialContent(
    members: List<TribeMember>,
    tribe: TribeProgress?,
    onNavigateToTribe: (String) -> Unit,
    onJoinTribe: () -> Unit = {}
) {
    val awakeCount = members.count { it.status == WakeStatus.Awake }
    val missedCount = members.count { it.status == WakeStatus.Missed }
    val youRank = members.indexOfFirst { it.isYou } + 1
    val stillSleeping = members.filter { it.status == WakeStatus.Sleeping && !it.isYou }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
    ) {
        ScreenHeader(title = "Social")

        MorningBoard(
            awake = awakeCount,
            missed = missedCount,
            total = members.size,
            youRank = youRank,
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .staggeredEnter(0)
        )

        Spacer(Modifier.height(28.dp))
        SectionHeader("Your tribe", Modifier.padding(horizontal = 24.dp))
        Spacer(Modifier.height(10.dp))
        TribeSummaryCard(
            tribe = tribe,
            onClick = { tribe?.let { onNavigateToTribe(it.id) } },
            onJoinTribe = onJoinTribe,
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .staggeredEnter(1)
        )

        Spacer(Modifier.height(28.dp))
        SectionHeader("Leaderboard", Modifier.padding(horizontal = 24.dp))
        Spacer(Modifier.height(10.dp))
        if (members.isEmpty()) {
            EmptyHint("No one here yet. Invite your crew!", Modifier.padding(horizontal = 20.dp))
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                members.forEachIndexed { index, member ->
                    LeaderboardRow(
                        rank = index + 1,
                        member = member,
                        modifier = Modifier.staggeredEnter(2 + index)
                    )
                }
            }
        }

        if (stillSleeping.isNotEmpty()) {
            Spacer(Modifier.height(28.dp))
            SectionHeader("Still sleeping", Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.height(10.dp))
            MelukoCard(modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
                stillSleeping.forEachIndexed { i, member ->
                    if (i > 0) com.alarm.meluko.ui.components.Hairline(Modifier.padding(horizontal = 16.dp))
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(photoUrl = member.photoUrl, name = member.name, size = 36.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = member.name,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "zzz",
                            color = MelukoTheme.colors.sleeping,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(128.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Morning board — dawn gradient + animated awake ring
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MorningBoard(
    awake: Int,
    missed: Int,
    total: Int,
    youRank: Int,
    modifier: Modifier = Modifier
) {
    val c = MelukoTheme.colors
    val ratio = if (total == 0) 0f else awake.toFloat() / total
    val (mood, sub) = when {
        total == 0 -> "Quiet in here" to "Invite your tribe to get started"
        awake == total -> "Full house!" to "Everyone is up. Legends."
        missed > 0 -> "Rough morning" to "$missed slept through the alarm"
        awake == 0 -> "Still dark out" to "No one is up yet"
        else -> "Rise and shine" to "${total - awake} still snoozing"
    }

    val sweep = remember { Animatable(0f) }
    LaunchedEffect(ratio) { sweep.animateTo(ratio, tween(1_100, easing = FastOutSlowInEasing)) }
    val shownAwake = rememberCountUp(awake)

    // Dusk violet when nobody's up, warming toward sunrise as people wake.
    val top = lerpColor(c.auroraCool, c.sunrise[1], ratio)
    val bottom = lerpColor(Color(0xFF2A1F5C), c.sunrise[0], ratio)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.linearGradient(listOf(top, bottom)))
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = mood,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = sub,
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (youRank > 0) {
                    Spacer(Modifier.height(14.dp))
                    Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.22f)) {
                        Text(
                            text = "You're #$youRank in the tribe",
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
            Spacer(Modifier.width(16.dp))
            Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                    drawArc(Color.White.copy(alpha = 0.22f), -90f, 360f, false, style = stroke)
                    drawArc(Color.White, -90f, 360f * sweep.value, false, style = stroke)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$shownAwake/$total",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "UP",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

private fun lerpColor(a: Color, b: Color, t: Float): Color = androidx.compose.ui.graphics.lerp(a, b, t.coerceIn(0f, 1f))

// ─────────────────────────────────────────────────────────────────────────────
//  Leaderboard row
// ─────────────────────────────────────────────────────────────────────────────
@Composable
internal fun LeaderboardRow(
    rank: Int,
    member: TribeMember,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    MelukoCard(
        modifier = modifier.fillMaxWidth(),
        color = if (member.isYou) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
        else MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RankBadge(rank)
            Spacer(Modifier.width(12.dp))
            UserAvatar(photoUrl = member.photoUrl, name = member.name, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (member.isYou) "${member.name} (you)" else member.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(2.dp))
                StatusLabel(member.status, member.wakeTime)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.LocalFireDepartment, null,
                    tint = if (member.streakDays > 0) MelukoTheme.colors.awake
                    else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = "${member.streakDays}",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            trailing?.invoke()
        }
    }
}

@Composable
internal fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    MelukoCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(20.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SocialPreview() {
    MelukoTheme {
        SocialContent(
            members = listOf(
                TribeMember("a", "Ria", null, false, false, 9, WakeStatus.Awake, "6:58 AM"),
                TribeMember("b", "You", null, true, true, 4, WakeStatus.Sleeping),
                TribeMember("c", "Sam", null, false, false, 0, WakeStatus.Missed),
            ),
            tribe = TribeProgress("t", "Hostel 4B", null, 1, 3),
            onNavigateToTribe = {}
        )
    }
}
