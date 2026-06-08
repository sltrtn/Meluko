package com.alarm.meluko.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alarm.meluko.auth.AuthViewModel
import com.alarm.meluko.ui.theme.DefaultFontFamily
import com.alarm.meluko.ui.theme.BodyFontFamily
import com.alarm.meluko.ui.theme.MelukoTheme

// ─────────────────────────────────────────────────────────────────────────────
//  AccountScreen
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AccountScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    accountViewModel: AccountViewModel = hiltViewModel(),
    socialViewModel: SocialViewModel = hiltViewModel()
) {
    val displayName by accountViewModel.displayName.collectAsState()
    val streakDays by accountViewModel.streakDays.collectAsState()
    val bestStreak by accountViewModel.bestStreak.collectAsState()
    val totalWakes by accountViewModel.totalWakes.collectAsState()
    val username by accountViewModel.username.collectAsState()
    val tribesProgress by socialViewModel.tribes.collectAsState()

    // rank and accuracy are not yet computed server-side; hide until available

    val activeTribes = tribesProgress.map { 
        TribeInfo(it.name, it.photoUrl, it.totalCount) 
    }

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
                    text          = "ACCOUNT",
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

            // ── Hero profile card ─────────────────────────────────────────────
            Card(
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape     = RoundedCornerShape(24.dp),
                colors    = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier            = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Avatar
                        UserAvatar(
                            photoUrl = null,
                            name     = displayName,
                            size     = 64.dp
                        )

                        // Name + email
                        Column {
                            Text(
                                text       = displayName,
                                fontSize   = 24.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = DefaultFontFamily,
                                color      = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text       = "@$username",
                                fontSize   = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = BodyFontFamily,
                                color      = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Pills row — streak + wakes
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AccountPill(
                            text  = "🔥 $streakDays-day streak",
                            color = MaterialTheme.colorScheme.tertiary,
                            on    = MaterialTheme.colorScheme.onTertiary
                        )
                        AccountPill(
                            text  = "☀️ $totalWakes wake-ups",
                            color = MaterialTheme.colorScheme.primary,
                            on    = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Stats row ─────────────────────────────────────────────────────
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AccountStatCard("☀️", "$totalWakes", "Wake-ups",  Modifier.weight(1f))
                AccountStatCard("🔥", "$streakDays d", "Streak", Modifier.weight(1f))
                AccountStatCard("🏅", "$bestStreak d", "Best",  Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))

            // ── Your tribes ───────────────────────────────────────────────────
            Text(
                text          = "Your Tribes",
                fontSize      = 13.sp,
                fontWeight    = FontWeight.Bold,
                fontFamily    = DefaultFontFamily,
                color         = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp,
                modifier      = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(10.dp))
            Column(
                modifier            = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                activeTribes.forEach { tribe ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Row(
                            modifier              = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TribeAvatar(
                                photoUrl = tribe.photoUrl,
                                name     = tribe.name,
                                size     = 32.dp
                            )
                            Text(
                                text       = tribe.name,
                                fontSize   = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = DefaultFontFamily,
                                color      = MaterialTheme.colorScheme.onSurface,
                                modifier   = Modifier.weight(1f)
                            )
                            Text(
                                text       = "${tribe.memberCount} members",
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = BodyFontFamily,
                                color      = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Settings ──────────────────────────────────────────────────────
            Text(
                text          = "Settings",
                fontSize      = 13.sp,
                fontWeight    = FontWeight.Bold,
                fontFamily    = DefaultFontFamily,
                color         = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp,
                modifier      = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(10.dp))

            Surface(
                modifier = Modifier.padding(horizontal = 20.dp),
                shape    = RoundedCornerShape(24.dp),
                color    = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsRow(
                        icon  = Icons.Outlined.Edit,
                        label = "Edit Profile",
                        onClick = { /* TODO */ }
                    )
                    HorizontalDivider(
                        modifier  = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingsRow(
                        icon  = Icons.Outlined.Notifications,
                        label = "Notifications",
                        onClick = { /* TODO */ }
                    )
                    HorizontalDivider(
                        modifier  = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingsRow(
                        icon  = Icons.Outlined.Info,
                        label = "About Meluko",
                        onClick = { /* TODO */ }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Sign out — separate card, destructive action
            Surface(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth(),
                shape    = RoundedCornerShape(24.dp),
                color    = MaterialTheme.colorScheme.errorContainer
            ) {
                SettingsRow(
                    icon      = Icons.AutoMirrored.Outlined.ExitToApp,
                    label     = "Sign Out",
                    tint      = MaterialTheme.colorScheme.onErrorContainer,
                    textColor = MaterialTheme.colorScheme.onErrorContainer,
                    onClick   = { authViewModel.signOut() }
                )
            }

            Spacer(Modifier.height(120.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Sub-components
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AccountPill(text: String, color: androidx.compose.ui.graphics.Color, on: androidx.compose.ui.graphics.Color) {
    Surface(shape = RoundedCornerShape(50), color = color) {
        Text(
            text       = text,
            fontSize   = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = DefaultFontFamily,
            color      = on,
            modifier   = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun AccountStatCard(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier  = modifier.height(90.dp),
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(emoji, fontSize = 18.sp)
            Column {
                Text(
                    text       = value,
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = DefaultFontFamily,
                    color      = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp
                )
                Text(
                    text       = label,
                    fontSize   = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = DefaultFontFamily,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.3.sp
                )
            }
        }
    }
}

@Composable
private fun SettingsRow(
    icon:      ImageVector,
    label:     String,
    tint:      androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    textColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick:   () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color    = androidx.compose.ui.graphics.Color.Transparent,
        onClick  = onClick
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = label,
                tint               = tint,
                modifier           = Modifier.size(20.dp)
            )
            Text(
                text       = label,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = DefaultFontFamily,
                color      = textColor,
                modifier   = Modifier.weight(1f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Preview
// ─────────────────────────────────────────────────────────────────────────────
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AccountPreview() {
    MelukoTheme { AccountScreen() }
}
