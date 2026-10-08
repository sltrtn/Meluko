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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.WbSunny
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
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.alarm.meluko.BuildConfig
import com.alarm.meluko.alarm.AlarmScheduler
import com.alarm.meluko.ui.theme.BodyFontFamily
import com.alarm.meluko.ui.theme.DefaultFontFamily
import com.alarm.meluko.ui.theme.MelukoTheme
import kotlinx.coroutines.delay
import java.util.*

// ─────────────────────────────────────────────────────────────────────────────
//  Data model
// ─────────────────────────────────────────────────────────────────────────────
data class TribeInfo(
    val id: String,
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
    val isLoading by homeViewModel.isLoading.collectAsState()
    val nextAlarmTime by homeViewModel.nextAlarmTime.collectAsState()
    val primaryTribeId by homeViewModel.primaryTribeId.collectAsState()
    val streakDays by homeViewModel.streakDays.collectAsState()
    val totalWakes by homeViewModel.totalWakes.collectAsState()
    val rank by homeViewModel.rank.collectAsState()
    val tribesProgress by socialViewModel.tribes.collectAsState()

    val activeTribes = tribesProgress.map {
        TribeInfo(it.id, it.name, it.photoUrl, it.totalCount)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val proposalError by proposalViewModel.error.collectAsState()
    val homeError by homeViewModel.error.collectAsState()
    val isOnline by homeViewModel.isOnline.collectAsState()

    LaunchedEffect(proposalError) {
        proposalError?.let {
            snackbarHostState.showSnackbar(it)
            proposalViewModel.clearError()
        }
    }

    LaunchedEffect(homeError) {
        homeError?.let {
            snackbarHostState.showSnackbar(it)
            homeViewModel.clearError()
        }
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
        isLoading      = isLoading,
        isOnline       = isOnline,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        nextAlarmTime  = nextAlarmTime,
        hasTribe       = primaryTribeId != null,
        activeTribes   = activeTribes,
        streakDays     = streakDays,
        totalWakes     = totalWakes,
        rank           = rank,
        onTribeClick   = onTribeClick,
        onProposeClick = { showProposeSheet = true }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Stateless content
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun HomeScreenContent(
    isLoading:      Boolean = false,
    isOnline:       Boolean = true,
    nextAlarmTime:  String,
    hasTribe:       Boolean,
    activeTribes:   List<TribeInfo>,
    snackbarHost:   @Composable () -> Unit = {},
    streakDays:     Int = 0,
    totalWakes:     Int = 0,
    rank:           Int = 1,
    onTribeClick:   (String) -> Unit,
    onProposeClick: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = snackbarHost,
        contentWindowInsets = WindowInsets.systemBars
    ) { padding ->
        if (isLoading) {
            HomeShimmer(modifier = Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Top Bar ───────────────────────────────────────────────────────
            ScreenHeader("MELUKO")

            // ── Offline banner ────────────────────────────────────────────────
            AnimatedVisibility(visible = !isOnline) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "You're offline — showing cached data",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }

            val context = LocalContext.current

            // Request notification permission for Android 13+ (API 33+)
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = { }
            )

            var exactAlarmGranted by remember {
                mutableStateOf(AlarmScheduler(context).canScheduleExactAlarms())
            }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!granted) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            // ── Exact-alarm permission banner (Android 12+) ────────────────────
            if (!exactAlarmGranted) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Exact alarms are disabled — your alarm may be delayed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                AlarmScheduler(context).openExactAlarmSettings()
                            }
                        ) {
                            Text(
                                text = "OPEN SETTINGS",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Re-check permission when the user returns from settings
            LaunchedEffect(Unit) {
                while (true) {
                    delay(1_000L)
                    exactAlarmGranted = AlarmScheduler(context).canScheduleExactAlarms()
                }
            }

            // ── Battery optimization banner ─────────────────────────────────────
            // OEMs (Xiaomi, Samsung, OnePlus, etc.) kill background alarms unless
            // the app is whitelisted from battery optimization. Dismissible —
            // unlike exact-alarm permission, this is a recommendation, not a hard
            // requirement, so we respect "not now".
            val prefs = remember { context.getSharedPreferences("meluko_prefs", Context.MODE_PRIVATE) }
            var batteryBannerDismissed by remember {
                mutableStateOf(prefs.getBoolean("battery_banner_dismissed", false))
            }
            var batteryOptIgnored by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }

            LaunchedEffect(Unit) {
                while (true) {
                    delay(1_000L)
                    batteryOptIgnored = isIgnoringBatteryOptimizations(context)
                }
            }

            if (!batteryOptIgnored && !batteryBannerDismissed) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Battery optimization may delay your alarm. Whitelist Meluko for reliable wake-ups.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Row {
                            TextButton(onClick = {
                                batteryBannerDismissed = true
                                prefs.edit().putBoolean("battery_banner_dismissed", true).apply()
                            }) {
                                Text(
                                    text = "NOT NOW",
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = {
                                val intent = Intent(
                                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            }) {
                                Text(
                                    text = "ALLOW",
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // ── Hero alarm card ───────────────────────────────────────────────
            AlarmHeroCard(
                nextAlarmTime  = nextAlarmTime,
                hasTribe       = hasTribe,
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
    hasTribe: Boolean,
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

    val context = LocalContext.current
    val timePart = run {
        val parts = nextAlarmTime.split(":")
        val hour24 = parts.getOrElse(0) { "7" }.toIntOrNull() ?: 7
        val minute = parts.getOrElse(1) { "30" }.replace(Regex("[^0-9]"), "")
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        "%d:%s".format(hour12, minute)
    }
    val amPmPart = run {
        val hour24 = nextAlarmTime.split(":").getOrElse(0) { "7" }.toIntOrNull() ?: 7
        if (hour24 >= 12) "PM" else "AM"
    }

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            animationSpec = MotionSpec.cardEnter,
            initialScale = 0.88f
        ) + fadeIn(
            animationSpec = MotionSpec.touch
        ),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .pressScale(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
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

            // Big time + AM centered
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
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
                horizontalArrangement = Arrangement.Center,
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
                    fontFamily = BodyFontFamily,
                    color      = MaterialTheme.colorScheme.onPrimary,
                        modifier   = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                // Propose button — only visible when user has a tribe
                if (hasTribe) {
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

                if (BuildConfig.DEBUG) {
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { AlarmScheduler(context).scheduleIn(5) },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(
                            text = "TEST",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = DefaultFontFamily
                        )
                    }
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
                TribeChip(tribe = tribe, onClick = { onTribeClick(tribe.id) })
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
        StatCard(Icons.Filled.LocalFireDepartment, "$streakDays", "Streak",    MaterialTheme.colorScheme.primaryContainer,    MaterialTheme.colorScheme.onPrimaryContainer,   Modifier.weight(1f).celebrateBurst(rememberIncreaseTrigger(streakDays)))
        StatCard(Icons.Filled.WbSunny,             "$totalWakes", "Wake-ups",  MaterialTheme.colorScheme.secondaryContainer,  MaterialTheme.colorScheme.onSecondaryContainer,  Modifier.weight(1f))
        StatCard(Icons.Filled.EmojiEvents,         "#$rank",      "Rank",      MaterialTheme.colorScheme.tertiaryContainer,   MaterialTheme.colorScheme.onTertiaryContainer,   Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(
    icon:           androidx.compose.ui.graphics.vector.ImageVector,
    value:          String,
    label:          String,
    containerColor: Color,
    contentColor:   Color,
    modifier:       Modifier = Modifier
) {
    Card(
        modifier  = modifier.height(100.dp).pressScale(pressedScale = 0.92f),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(15.dp))
            }
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

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
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
        shape = RoundedCornerShape(12.dp),
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
                ConsistencyMetric(Icons.Filled.LocalFireDepartment, "$streakDays", "Current streak", MaterialTheme.colorScheme.primary)
                ConsistencyMetric(Icons.Filled.WbSunny, "$totalWakes", "Total wake-ups", MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

@Composable
private fun ConsistencyMetric(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    tint: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
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
//  Shimmer skeleton
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HomeShimmer(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(20.dp))
        ScreenHeader("MELUKO")
        Spacer(Modifier.height(12.dp))
        ShimmerCard(
            modifier = Modifier.padding(horizontal = 20.dp),
            height = 240.dp
        )
        Spacer(Modifier.height(20.dp))
        ShimmerLine(
            width = 100.dp,
            height = 14.dp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ShimmerTribeChip()
            ShimmerTribeChip()
            ShimmerTribeChip()
        }
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ShimmerStatCard(Modifier.weight(1f))
            ShimmerStatCard(Modifier.weight(1f))
            ShimmerStatCard(Modifier.weight(1f))
        }
        Spacer(Modifier.height(20.dp))
        ShimmerCard(
            modifier = Modifier.padding(horizontal = 20.dp),
            height = 160.dp
        )
        Spacer(Modifier.height(120.dp))
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
            hasTribe = true,
            activeTribes = emptyList(),
            streakDays = 5,
            totalWakes = 23,
            rank = 2,
            onTribeClick = {},
            onProposeClick = {}
        )
    }
}
