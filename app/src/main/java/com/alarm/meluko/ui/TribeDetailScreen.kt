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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TribeDetailScreen(
    tribeId: String,
    onBack: () -> Unit
) {
    // Mock data for the selected tribe
    val tribeName = tribeId.replaceFirstChar { it.uppercase() } // simple mock name
    var isFollowedTone by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "8 Members",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = DefaultFontFamily,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Next Alarm: 07:30 AM",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = DefaultFontFamily,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
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

            // Mock Leaderboard Rows
            val members = listOf("Aisha" to 14, "You" to 12, "Sachin" to 8)
            members.forEachIndexed { index, (name, streak) ->
                TribeDetailLeaderboardRow(
                    rank = index + 1,
                    name = name,
                    streak = streak,
                    isYou = name == "You",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }
            
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun TribeDetailLeaderboardRow(
    rank: Int,
    name: String,
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
                photoUrl = null,
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

@Preview(showBackground = true)
@Composable
private fun TribeDetailPreview() {
    MelukoTheme {
        TribeDetailScreen(tribeId = "Hostel D4", onBack = {})
    }
}
