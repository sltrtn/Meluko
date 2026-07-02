package com.alarm.meluko.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.Build
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import com.alarm.meluko.ui.theme.DefaultFontFamily
import com.alarm.meluko.ui.theme.BodyFontFamily
import com.alarm.meluko.ui.theme.MelukoTheme
import kotlinx.coroutines.delay
import java.util.*

// ─────────────────────────────────────────────────────────────────────────────
//  Data model — will be replaced with Firestore later
// ─────────────────────────────────────────────────────────────────────────────
data class TribeInfo(
    val name: String,
    val photoUrl: String?,
    val memberCount: Int
)

// ─────────────────────────────────────────────────────────────────────────────
//  Entry — ViewModel hoist
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun HomeScreen(
    onTribeClick: (String) -> Unit,
    homeViewModel: HomeViewModel = hiltViewModel(),
    socialViewModel: SocialViewModel = hiltViewModel(),
    proposalViewModel: ProposalViewModel = hiltViewModel()
) {
    val nextAlarmTime by homeViewModel.nextAlarmTime.collectAsState()
    val isAlarmRinging by homeViewModel.isAlarmRinging.collectAsState()
    val primaryTribeId by homeViewModel.primaryTribeId.collectAsState()
    val streakDays by homeViewModel.streakDays.collectAsState()
    val totalWakes by homeViewModel.totalWakes.collectAsState()
    val rank by homeViewModel.rank.collectAsState()
    val tribesProgress by socialViewModel.tribes.collectAsState()

    val activeTribes = tribesProgress.map {
        TribeInfo(it.name, it.photoUrl, it.totalCount)
    }

    var showProposeSheet by remember { mutableStateOf(false) }

    if (showProposeSheet && primaryTribeId != null) {
        ProposeAlarmBottomSheet(
            tribeId = primaryTribeId!!,
            proposalViewModel = proposalViewModel,
            onDismiss = { showProposeSheet = false }
        )
    }

    HomeScreenContent(
        nextAlarmTime  = nextAlarmTime,
        activeTribes   = activeTribes,
        isAlarmRinging = isAlarmRinging,
        streakDays     = streakDays,
        totalWakes     = totalWakes,
        rank           = rank,
        onDismissAlarm = { homeViewModel.dismissAlarm() },
        onReset        = { homeViewModel.resetAlarm() },
        onTribeClick   = onTribeClick,
        onProposeClick = { showProposeSheet = true }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Stateless content
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun HomeScreenContent(
    nextAlarmTime:  String,
    activeTribes:   List<TribeInfo>,
    isAlarmRinging: Boolean,
    streakDays:     Int = 0,
    totalWakes:     Int = 0,
    rank:           Int = 1,
    onDismissAlarm: () -> Unit,
    onReset:        () -> Unit,
    onTribeClick:   (String) -> Unit,
    onProposeClick: () -> Unit
) {
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

            // ── Top Bar ───────────────────────────────────────────────────────
            ScreenHeader("MELUKO")

            // Request notification permission for Android 13+ (API 33+)
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = { isGranted ->
                    if (!isGranted) {
                        // Should probably show a rationale, but we'll keep it simple for now
                    }
                }
            )

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            // ── Hero alarm card ───────────────────────────────────────────────
            AlarmHeroCard(
                nextAlarmTime  = nextAlarmTime,
                onProposeClick = onProposeClick,
                modifier       = Modifier.padding(horizontal = 20.dp)
            )
            
            Spacer(Modifier.height(20.dp))

            // ── Active tribes ─────────────────────────────────────────────────
            ActiveTribesSection(
                tribes   = activeTribes,
                onTribeClick = onTribeClick,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(20.dp))

            // ── Stats ─────────────────────────────────────────────────────────
            StatsRow(
                streakDays = streakDays,
                totalWakes = totalWakes,
                rank       = rank,
                modifier   = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(20.dp))

            // ── Consistency ─────────────────────────────────────────────────────
            ConsistencyCard(
                streakDays = streakDays,
                totalWakes = totalWakes,
                modifier   = Modifier.padding(horizontal = 20.dp)
            )

            // Extra padding for glass nav bar
            Spacer(Modifier.height(120.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Hero Alarm Card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AlarmHeroCard(
    nextAlarmTime: String,
    onProposeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    // Live countdown — updates every minute
    val countdown by produceState(initialValue = computeCountdown(nextAlarmTime), key1 = nextAlarmTime) {
        while (true) {
            value = computeCountdown(nextAlarmTime)
            delay(60_000L)
        }
    }

    val timePart = nextAlarmTime.replace(" AM", "").replace(" PM", "").trim()
    val amPmPart = if (nextAlarmTime.contains("PM", ignoreCase = true)) "PM" else "AM"

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
            initialScale = 0.88f
        ) + fadeIn(
            animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium)
        ),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .pressScale(),
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Label
            Text(
                text          = "NEXT ALARM",
                fontSize      = 11.sp,
                fontWeight    = FontWeight.Bold,
                fontFamily    = DefaultFontFamily,
                letterSpacing = 3.sp,
                color         = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
            )

            Spacer(Modifier.height(8.dp))

            // Big time + AM on same row
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text       = timePart,
                    fontSize   = 72.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = DefaultFontFamily,
                    color      = MaterialTheme.colorScheme.onPrimaryContainer,
                    lineHeight = 76.sp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text       = amPmPart,
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = DefaultFontFamily,
                    color      = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier   = Modifier.padding(bottom = 10.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Countdown chip
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text       = countdown,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = DefaultFontFamily,
                        color      = MaterialTheme.colorScheme.onPrimary,
                        modifier   = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                    )
                }

                // Propose button
                Button(
                    onClick = onProposeClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onPrimaryContainer),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "PROPOSE ALARM",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = DefaultFontFamily,
                        color = MaterialTheme.colorScheme.primaryContainer
                    )
                }
            }
        }
    }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Active Tribes Section
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ActiveTribesSection(
    tribes:   List<TribeInfo>,
    onTribeClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Header
        Text(
            text       = "${tribes.size} tribes active",
            fontSize   = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = DefaultFontFamily,
            color      = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.5.sp,
            modifier   = Modifier.padding(bottom = 12.dp)
        )

        // Horizontally scrollable tribe chips
        Row(
            modifier              = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            tribes.forEach { tribe ->
                TribeChip(tribe = tribe, onClick = { onTribeClick(tribe.name) })
            }
        }
    }
}

@Composable
private fun TribeChip(tribe: TribeInfo, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.pressScale(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
        onClick = onClick
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TribeAvatar(
                photoUrl = tribe.photoUrl,
                name     = tribe.name,
                size     = 40.dp
            )
            Column {
                Text(
                    text       = tribe.name,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = DefaultFontFamily,
                    color      = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text       = "${tribe.memberCount} members",
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = BodyFontFamily,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Stats Row
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StatsRow(
    streakDays: Int,
    totalWakes: Int,
    rank: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier              = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard("🔥", "$streakDays", "Streak",    MaterialTheme.colorScheme.primaryContainer,    MaterialTheme.colorScheme.onPrimaryContainer,   Modifier.weight(1f))
        StatCard("☀️", "$totalWakes", "Wake-ups",  MaterialTheme.colorScheme.secondaryContainer,  MaterialTheme.colorScheme.onSecondaryContainer,  Modifier.weight(1f))
        StatCard("🏆", "#$rank",      "Rank",      MaterialTheme.colorScheme.tertiaryContainer,   MaterialTheme.colorScheme.onTertiaryContainer,   Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(
    emoji:          String,
    value:          String,
    label:          String,
    containerColor: Color,
    contentColor:   Color,
    modifier:       Modifier = Modifier
) {
    Card(
        modifier  = modifier.height(100.dp).pressScale(pressedScale = 0.92f),
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(emoji, fontSize = 20.sp)
            Column {
                Text(
                    text       = value,
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = DefaultFontFamily,
                    color      = contentColor,
                    lineHeight = 24.sp
                )
                Text(
                    text       = label,
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = BodyFontFamily,
                    color      = contentColor.copy(alpha = 0.65f),
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Countdown helper
// ─────────────────────────────────────────────────────────────────────────────
fun computeCountdown(timeString: String): String {
    val parts       = timeString.split(":")
    val alarmHour   = parts.getOrElse(0) { "7" }.toIntOrNull()  ?: 7
    val alarmMinute = parts.getOrElse(1) { "30" }.toIntOrNull() ?: 30

    val now   = Calendar.getInstance()
    val alarm = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, alarmHour)
        set(Calendar.MINUTE, alarmMinute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    if (!alarm.after(now)) alarm.add(Calendar.DAY_OF_MONTH, 1)

    val diffMillis = alarm.timeInMillis - now.timeInMillis
    val hours      = (diffMillis / (1000 * 60 * 60)).toInt()
    val minutes    = ((diffMillis % (1000 * 60 * 60)) / (1000 * 60)).toInt()

    return when {
        hours > 0   -> "in ${hours}h ${minutes}m"
        minutes > 0 -> "in ${minutes}m"
        else        -> "Starting soon"
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Consistency Card — real data from Firestore
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ConsistencyCard(
    streakDays: Int,
    totalWakes: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Consistency",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = DefaultFontFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ConsistencyMetric("🔥", "$streakDays", "Current streak")
                ConsistencyMetric("☀️", "$totalWakes", "Total wake-ups")
            }
        }
    }
}

@Composable
private fun ConsistencyMetric(
    emoji: String,
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 24.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            fontFamily = DefaultFontFamily,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 30.sp
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = BodyFontFamily,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.3.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Previews
// ─────────────────────────────────────────────────────────────────────────────
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomePreview() {
    MelukoTheme {
        HomeScreenContent(
            nextAlarmTime = "07:30",
            activeTribes = emptyList(),
            isAlarmRinging = false,
            streakDays = 5,
            totalWakes = 23,
            rank = 2,
            onDismissAlarm = {},
            onReset = {},
            onTribeClick = {},
            onProposeClick = {}
        )
    }
}
