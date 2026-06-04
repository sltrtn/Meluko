package com.alarm.meluko.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alarm.meluko.ui.theme.DefaultFontFamily
import com.alarm.meluko.ui.theme.MelukoTheme
import kotlinx.coroutines.delay
import java.util.*

// ─────────────────────────────────────────────────────────────────────────────
//  Data model — will be replaced with Firestore later
// ─────────────────────────────────────────────────────────────────────────────
data class TribeInfo(
    val name: String,
    val emoji: String,
    val memberCount: Int
)

// ─────────────────────────────────────────────────────────────────────────────
//  Entry — ViewModel hoist
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun HomeScreen(homeViewModel: HomeViewModel = hiltViewModel()) {
    val isAlarmRinging by homeViewModel.isAlarmRinging.collectAsState()
    val nextAlarmTime  by homeViewModel.nextAlarmTime.collectAsState()

    // Sample tribes — replace with Firestore flow in the future
    val activeTribes = listOf(
        TribeInfo("Hostel D4",   "🏠", 8),
        TribeInfo("MIT CS 4th",  "📚", 12),
        TribeInfo("Early Birds", "🌅", 5),
    )

    HomeScreenContent(
        nextAlarmTime = nextAlarmTime,
        activeTribes  = activeTribes,
        onReset       = { homeViewModel.resetAlarm() }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Stateless content
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun HomeScreenContent(
    nextAlarmTime: String,
    activeTribes:  List<TribeInfo>,
    onReset:       () -> Unit = {}
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
                    text          = "MELUKO",
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


            // ── Hero alarm card ───────────────────────────────────────────────
            AlarmHeroCard(
                nextAlarmTime = nextAlarmTime,
                modifier      = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(20.dp))

            // ── Active tribes ─────────────────────────────────────────────────
            ActiveTribesSection(
                tribes   = activeTribes,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(20.dp))

            // ── Stats ─────────────────────────────────────────────────────────
            StatsRow(modifier = Modifier.padding(horizontal = 20.dp))

            // Debug
            Spacer(Modifier.height(12.dp))
            TextButton(
                onClick  = onReset,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    "Debug: Trigger Alarm",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }

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
    modifier: Modifier = Modifier
) {
    // Live countdown — updates every minute
    val countdown by produceState(initialValue = computeCountdown(nextAlarmTime), key1 = nextAlarmTime) {
        while (true) {
            value = computeCountdown(nextAlarmTime)
            delay(60_000L)
        }
    }

    val parts = nextAlarmTime.split(":").let {
        Pair(it.getOrElse(0) { "07" }, it.getOrElse(1) { "30" })
    }

    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(36.dp),
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
                    text       = nextAlarmTime,
                    fontSize   = 72.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = DefaultFontFamily,
                    color      = MaterialTheme.colorScheme.onPrimaryContainer,
                    lineHeight = 76.sp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text       = "AM",
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = DefaultFontFamily,
                    color      = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier   = Modifier.padding(bottom = 10.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

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
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Active Tribes Section
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ActiveTribesSection(
    tribes:   List<TribeInfo>,
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
                TribeChip(tribe = tribe)
            }
        }
    }
}

@Composable
private fun TribeChip(tribe: TribeInfo) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(tribe.emoji, fontSize = 20.sp)
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
                    fontFamily = DefaultFontFamily,
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
private fun StatsRow(modifier: Modifier = Modifier) {
    Row(
        modifier              = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard("🔥", "12",  "Streak",    MaterialTheme.colorScheme.primaryContainer,    MaterialTheme.colorScheme.onPrimaryContainer,   Modifier.weight(1f))
        StatCard("❄️", "2",   "Freezes",   MaterialTheme.colorScheme.secondaryContainer,  MaterialTheme.colorScheme.onSecondaryContainer,  Modifier.weight(1f))
        StatCard("🏆", "#2",  "Rank",      MaterialTheme.colorScheme.tertiaryContainer,   MaterialTheme.colorScheme.onTertiaryContainer,   Modifier.weight(1f))
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
        modifier  = modifier.height(100.dp),
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
                    fontFamily = DefaultFontFamily,
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
//  Previews
// ─────────────────────────────────────────────────────────────────────────────
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomePreview() {
    MelukoTheme {
        HomeScreenContent(
            nextAlarmTime = "07:30",
            activeTribes  = listOf(
                TribeInfo("Hostel D4",   "🏠", 8),
                TribeInfo("MIT CS 4th",  "📚", 12),
                TribeInfo("Early Birds", "🌅", 5),
            )
        )
    }
}
