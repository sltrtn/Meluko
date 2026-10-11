package com.alarm.meluko.ui

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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.HowToVote
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.alarm.meluko.BuildConfig
import com.alarm.meluko.alarm.AlarmScheduler
import com.alarm.meluko.ui.components.MelukoCard
import com.alarm.meluko.ui.components.MelukoLoading
import com.alarm.meluko.ui.components.SectionHeader
import com.alarm.meluko.ui.components.StatTile
import com.alarm.meluko.ui.components.plural
import com.alarm.meluko.ui.components.staggeredEnter
import com.alarm.meluko.ui.theme.BodyFontFamily
import com.alarm.meluko.ui.theme.DefaultFontFamily
import com.alarm.meluko.ui.theme.MelukoTheme
import com.alarm.meluko.util.to12HourParts
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
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
    onJoinTribe: () -> Unit = {},
    homeViewModel: HomeViewModel = hiltViewModel(),
    socialViewModel: SocialViewModel = hiltViewModel(),
    proposalViewModel: ProposalViewModel = hiltViewModel()
) {
    val isLoading by homeViewModel.isLoading.collectAsState()
    val nextAlarmTime by homeViewModel.nextAlarmTime.collectAsState()
    val primaryTribeId by homeViewModel.primaryTribeId.collectAsState()
    val streakDays by homeViewModel.streakDays.collectAsState()
    val bestStreak by homeViewModel.bestStreak.collectAsState()
    val totalWakes by homeViewModel.totalWakes.collectAsState()
    val rank by homeViewModel.rank.collectAsState()
    val lastWakeDate by homeViewModel.lastWakeDate.collectAsState()
    val todayStatus by homeViewModel.todayStatus.collectAsState()
    val tribes by socialViewModel.tribes.collectAsState()
    val activeProposal by proposalViewModel.activeProposal.collectAsState()

    // Home also drives auto-accept, so proposals resolve without anyone opening Tribe Detail.
    LaunchedEffect(primaryTribeId) {
        primaryTribeId?.let { proposalViewModel.listenToTribeProposals(it) }
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
            initialTime = nextAlarmTime,
            onDismiss = { showProposeSheet = false }
        )
    }

    HomeScreenContent(
        isLoading = isLoading,
        isOnline = isOnline,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        nextAlarmTime = nextAlarmTime,
        hasTribe = primaryTribeId != null,
        hasOpenProposal = activeProposal != null,
        tribe = tribes.firstOrNull(),
        streakDays = streakDays,
        bestStreak = bestStreak,
        totalWakes = totalWakes,
        rank = rank,
        lastWakeDate = lastWakeDate,
        todayStatus = todayStatus,
        onTribeClick = onTribeClick,
        onJoinTribe = onJoinTribe,
        onProposeClick = { showProposeSheet = true }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Stateless content
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun HomeScreenContent(
    isLoading: Boolean = false,
    isOnline: Boolean = true,
    nextAlarmTime: String,
    hasTribe: Boolean,
    tribe: TribeProgress?,
    hasOpenProposal: Boolean = false,
    snackbarHost: @Composable () -> Unit = {},
    streakDays: Int = 0,
    bestStreak: Int = 0,
    totalWakes: Int = 0,
    rank: Int = 1,
    lastWakeDate: String? = null,
    todayStatus: WakeStatus = WakeStatus.Sleeping,
    onTribeClick: (String) -> Unit,
    onJoinTribe: () -> Unit = {},
    onProposeClick: () -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = snackbarHost,
        contentWindowInsets = WindowInsets.systemBars
    ) { padding ->
        if (isLoading) {
            MelukoLoading(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            ScreenHeader(title = "Meluko")

            HomeNotices(isOnline = isOnline)

            AlarmHeroCard(
                nextAlarmTime = nextAlarmTime,
                hasTribe = hasTribe,
                hasOpenProposal = hasOpenProposal,
                onProposeClick = onProposeClick,
                onOpenProposal = { tribe?.let { onTribeClick(it.id) } },
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .staggeredEnter(0)
            )

            Spacer(Modifier.height(28.dp))

            SectionHeader("Your tribe", Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.height(10.dp))
            TribeSummaryCard(
                tribe = tribe,
                onClick = { tribe?.let { onTribeClick(it.id) } },
                onJoinTribe = onJoinTribe,
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .staggeredEnter(1)
            )

            Spacer(Modifier.height(28.dp))

            SectionHeader("Your stats", Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .staggeredEnter(2),
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
                    icon = Icons.Rounded.WbSunny,
                    polygon = MaterialShapes.Cookie9Sided,
                    value = totalWakes,
                    label = "Wake-ups",
                    container = MaterialTheme.colorScheme.secondaryContainer,
                    content = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    icon = Icons.Rounded.EmojiEvents,
                    polygon = MaterialShapes.SoftBurst,
                    value = rank,
                    prefix = "#",
                    label = "Tribe rank",
                    container = MaterialTheme.colorScheme.tertiaryContainer,
                    content = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(28.dp))

            SectionHeader("This week", Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.height(10.dp))
            WeekCard(
                streakDays = streakDays,
                bestStreak = bestStreak,
                lastWakeDate = lastWakeDate,
                todayStatus = todayStatus,
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .staggeredEnter(3)
            )

            // Clearance for the floating glass nav bar
            Spacer(Modifier.height(128.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Notices: offline, exact-alarm permission, battery optimisation
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HomeNotices(isOnline: Boolean) {
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { }
    )
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var exactAlarmGranted by remember { mutableStateOf(AlarmScheduler(context).canScheduleExactAlarms()) }
    val prefs = remember { context.getSharedPreferences("meluko_prefs", Context.MODE_PRIVATE) }
    var batteryBannerDismissed by remember { mutableStateOf(prefs.getBoolean("battery_banner_dismissed", false)) }
    var batteryOptIgnored by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }

    // Re-check when the user returns from system settings
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000L)
            exactAlarmGranted = AlarmScheduler(context).canScheduleExactAlarms()
            batteryOptIgnored = isIgnoringBatteryOptimizations(context)
        }
    }

    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        NoticeCard(
            visible = !isOnline,
            icon = Icons.Rounded.CloudOff,
            text = "You're offline. Showing your last synced data.",
        )
        NoticeCard(
            visible = !exactAlarmGranted,
            icon = Icons.Rounded.Alarm,
            text = "Exact alarms are off, so your alarm could ring late.",
            emphasis = true,
            actionLabel = "Allow",
            onAction = { AlarmScheduler(context).openExactAlarmSettings() }
        )
        NoticeCard(
            visible = !batteryOptIgnored && !batteryBannerDismissed,
            icon = Icons.Rounded.BatterySaver,
            text = "Some phones silence alarms to save battery. Let Meluko run freely.",
            actionLabel = "Allow",
            onAction = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:${context.packageName}")
                    )
                )
            },
            onDismiss = {
                batteryBannerDismissed = true
                prefs.edit().putBoolean("battery_banner_dismissed", true).apply()
            }
        )
    }
    if (!isOnline || !exactAlarmGranted || (!batteryOptIgnored && !batteryBannerDismissed)) {
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun NoticeCard(
    visible: Boolean,
    icon: ImageVector,
    text: String,
    emphasis: Boolean = false,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        val container = if (emphasis) MaterialTheme.colorScheme.errorContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh
        val content = if (emphasis) MaterialTheme.colorScheme.onErrorContainer
        else MaterialTheme.colorScheme.onSurface
        Surface(shape = MaterialTheme.shapes.medium, color = container) {
            Row(
                modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, tint = content, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    text = text,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    fontFamily = BodyFontFamily,
                    color = content,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp)
                )
                if (actionLabel != null && onAction != null) {
                    TextButton(onClick = onAction) {
                        Text(actionLabel, fontWeight = FontWeight.ExtraBold, fontFamily = DefaultFontFamily)
                    }
                }
                if (onDismiss != null) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, "Dismiss", tint = content.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Hero alarm card — a sunrise, not a box
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AlarmHeroCard(
    nextAlarmTime: String,
    hasTribe: Boolean,
    onProposeClick: () -> Unit,
    modifier: Modifier = Modifier,
    hasOpenProposal: Boolean = false,
    onOpenProposal: () -> Unit = {},
) {
    val context = LocalContext.current
    val sunrise = MelukoTheme.colors.sunrise
    val countdown by produceState(initialValue = computeCountdown(nextAlarmTime), key1 = nextAlarmTime) {
        while (true) {
            value = computeCountdown(nextAlarmTime)
            delay(30_000L)
        }
    }
    val (time, amPm) = to12HourParts(nextAlarmTime)

    val spin by rememberInfiniteTransition(label = "heroSun").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(40_000, easing = LinearEasing)),
        label = "heroSunSpin"
    )
    val sunShape = MaterialShapes.Sunny.toShape()
    val onHero = Color(0xFF2A1100)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.linearGradient(sunrise.reversed()))
            .drawBehind {
                // Soft highlight from the top-left, like low morning light
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.28f), Color.Transparent),
                        center = Offset(size.width * 0.1f, 0f),
                        radius = size.width * 0.8f
                    ),
                    radius = size.width * 0.8f,
                    center = Offset(size.width * 0.1f, 0f)
                )
            }
    ) {
        // Decorative expressive sun, slowly turning in the corner
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 56.dp, y = (-56).dp)
                .size(200.dp)
                .graphicsLayer { rotationZ = spin }
                .background(Color.White.copy(alpha = 0.16f), sunShape)
        )

        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 20.dp)) {
            Text(
                text = "NEXT ALARM",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = DefaultFontFamily,
                letterSpacing = 2.sp,
                color = onHero.copy(alpha = 0.7f)
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = time,
                    fontSize = 76.sp,
                    lineHeight = 80.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = DefaultFontFamily,
                    color = onHero
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = amPm,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = DefaultFontFamily,
                    color = onHero.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 14.dp)
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Frosted chip — glass over the warm gradient
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.26f),
                    modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                ) {
                    Text(
                        text = countdown,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = BodyFontFamily,
                        color = onHero,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                if (BuildConfig.DEBUG) {
                    IconButton(onClick = { AlarmScheduler(context).scheduleIn(5) }) {
                        Icon(Icons.Rounded.BugReport, "Test alarm in 5 seconds", tint = onHero.copy(alpha = 0.6f))
                    }
                }
                if (hasTribe && hasOpenProposal) {
                    Button(
                        onClick = onOpenProposal,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.3f),
                            contentColor = onHero
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        modifier = Modifier.pressScale()
                    ) {
                        Icon(Icons.Rounded.HowToVote, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Proposal open", fontWeight = FontWeight.ExtraBold, fontFamily = DefaultFontFamily)
                    }
                } else if (hasTribe) {
                    Button(
                        onClick = onProposeClick,
                        colors = ButtonDefaults.buttonColors(containerColor = onHero, contentColor = Color.White),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                        modifier = Modifier.pressScale()
                    ) {
                        Icon(Icons.Rounded.Alarm, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Propose", fontWeight = FontWeight.ExtraBold, fontFamily = DefaultFontFamily)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Tribe summary
// ─────────────────────────────────────────────────────────────────────────────
@Composable
internal fun TribeSummaryCard(
    tribe: TribeProgress?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onJoinTribe: (() -> Unit)? = null,
) {
    if (tribe == null) {
        MelukoCard(modifier = modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    text = "You're not in a tribe yet. Mornings are better together.",
                    fontSize = 14.sp,
                    fontFamily = BodyFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (onJoinTribe != null) {
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = onJoinTribe,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.pressScale()
                    ) {
                        Text("Join or create a tribe", fontWeight = FontWeight.ExtraBold, fontFamily = DefaultFontFamily)
                    }
                }
            }
        }
        return
    }
    MelukoCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TribeAvatar(photoUrl = tribe.photoUrl, name = tribe.name, size = 52.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = tribe.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = DefaultFontFamily,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${plural(tribe.totalCount, "member")} · ${tribe.awakeCount} up today",
                    fontSize = 13.sp,
                    fontFamily = BodyFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                AwakeMeter(awake = tribe.awakeCount, total = tribe.totalCount)
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = "Open tribe",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Segmented bar: one pill per member, filled amber when they're up. */
@Composable
private fun AwakeMeter(awake: Int, total: Int) {
    val count = total.coerceIn(1, 12)
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
        repeat(count) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(6.dp)
                    .background(
                        if (i < awake) MelukoTheme.colors.awake
                        else MaterialTheme.colorScheme.outlineVariant,
                        CircleShape
                    )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Week strip — the last 7 days, derived from the current streak
// ─────────────────────────────────────────────────────────────────────────────
private enum class DayMark { Woke, Missed, Today, Empty }

@Composable
private fun WeekCard(
    streakDays: Int,
    bestStreak: Int,
    lastWakeDate: String?,
    todayStatus: WakeStatus,
    modifier: Modifier = Modifier
) {
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val days = remember(streakDays, lastWakeDate, todayStatus, locale) {
        weekMarks(streakDays, lastWakeDate, todayStatus, locale)
    }
    MelukoCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                days.forEach { (label, mark) -> DayDot(label, mark) }
            }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.EmojiEvents, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Best streak: ${plural(bestStreak, "day")}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = BodyFontFamily,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = when {
                        streakDays == 0 -> "Start one tomorrow"
                        streakDays >= bestStreak -> "Personal best!"
                        else -> "${bestStreak - streakDays} to beat it"
                    },
                    fontSize = 13.sp,
                    fontFamily = BodyFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DayDot(label: String, mark: DayMark) {
    val c = MelukoTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val dotModifier = Modifier.size(32.dp)
        when (mark) {
            DayMark.Woke -> Box(
                dotModifier.background(c.awake, MaterialShapes.Cookie6Sided.toShape()),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.WbSunny, null, tint = Color(0xFF2A1100), modifier = Modifier.size(16.dp))
            }
            DayMark.Missed -> Box(
                dotModifier.background(c.missed.copy(alpha = 0.22f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Close, null, tint = c.missed, modifier = Modifier.size(16.dp))
            }
            DayMark.Today -> Box(
                dotModifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
            )
            DayMark.Empty -> Box(
                dotModifier.background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = DefaultFontFamily,
            color = if (mark == DayMark.Today) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * We only store the current streak and last wake date, so the strip shows the
 * run of consecutive wake-ups ending at [lastWakeDate]; anything before that
 * run is unknown (empty), not assumed missed.
 */
private fun weekMarks(
    streakDays: Int,
    lastWakeDate: String?,
    todayStatus: WakeStatus,
    locale: Locale
): List<Pair<String, DayMark>> {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val dayFormat = SimpleDateFormat("EEEEE", locale)
    val lastWake = lastWakeDate?.let { runCatching { dateFormat.parse(it) }.getOrNull() }
    val today = Calendar.getInstance()

    return (6 downTo 0).map { daysAgo ->
        val day = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
        val label = dayFormat.format(day.time)
        val isToday = daysAgo == 0
        val mark = when {
            isToday && todayStatus == WakeStatus.Missed -> DayMark.Missed
            lastWake != null && streakDays > 0 && withinStreak(day, lastWake, streakDays) -> DayMark.Woke
            isToday -> DayMark.Today
            else -> DayMark.Empty
        }
        label to mark
    }
}

private fun withinStreak(day: Calendar, lastWake: Date, streakDays: Int): Boolean {
    val dayStart = (day.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val diffDays = ((lastWake.time - dayStart) / 86_400_000L).toInt()
    return diffDays in 0 until streakDays
}

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

// ─────────────────────────────────────────────────────────────────────────────
//  Countdown helper
// ─────────────────────────────────────────────────────────────────────────────
fun computeCountdown(timeString: String): String {
    val parts = timeString.split(":")
    val alarmHour = parts.getOrElse(0) { "7" }.toIntOrNull() ?: 7
    val alarmMinute = parts.getOrElse(1) { "30" }.toIntOrNull() ?: 30

    val now = Calendar.getInstance()
    val alarm = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, alarmHour)
        set(Calendar.MINUTE, alarmMinute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    if (!alarm.after(now)) alarm.add(Calendar.DAY_OF_MONTH, 1)

    val diffMillis = alarm.timeInMillis - now.timeInMillis
    val hours = (diffMillis / (1000 * 60 * 60)).toInt()
    val minutes = ((diffMillis % (1000 * 60 * 60)) / (1000 * 60)).toInt()

    return when {
        hours > 0 -> "Rings in ${hours}h ${minutes}m"
        minutes > 0 -> "Rings in ${minutes}m"
        else -> "Ringing soon"
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Preview
// ─────────────────────────────────────────────────────────────────────────────
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomePreview() {
    MelukoTheme {
        HomeScreenContent(
            nextAlarmTime = "07:30",
            hasTribe = true,
            tribe = TribeProgress("t", "Hostel 4B", null, 3, 5),
            streakDays = 5,
            bestStreak = 9,
            totalWakes = 23,
            rank = 2,
            onTribeClick = {},
            onProposeClick = {}
        )
    }
}
