package com.alarm.meluko.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────────────────────
//  Tab model
//  Order: Social (left) | Home (centre) | Account (right)
//  This determines the slide direction — lower index = left, higher = right
// ─────────────────────────────────────────────────────────────────────────────
private data class Tab(
    val label:         String,
    val selectedIcon:  ImageVector,
    val unselectedIcon: ImageVector,
)

private val tabs = listOf(
    Tab("Social",  Icons.Rounded.Group,  Icons.Outlined.Group),
    Tab("Home",    Icons.Filled.Home,    Icons.Outlined.Home),
    Tab("Account", Icons.Filled.Person,  Icons.Outlined.Person),
)

private const val HOME_TAB = 1  // centre tab is the default

// ─────────────────────────────────────────────────────────────────────────────
//  MainScreen
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun MainScreen(
    onNavigateToTribe: (String) -> Unit = {}
) {
    // rememberSaveable: survives NavController back-stack pops and config changes
    // If we used remember here, popping from TribeDetailScreen resets to HOME_TAB
    var selectedTab by rememberSaveable { mutableIntStateOf(HOME_TAB) }

    // Background on the Box prevents the window white showing through
    // during the cross-slide animation between panes
    val bgColor = MaterialTheme.colorScheme.background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {

        // ── Tab content with directional slide ────────────────────────────────
        AnimatedContent(
            targetState  = selectedTab,
            transitionSpec = {
                // Direction: moving to a higher index = slide left (→ right pane)
                //            moving to a lower  index = slide right (← left pane)
                val direction = if (targetState > initialState) 1 else -1

                // Enter: new pane slides in from the correct side
                val enterSlide = slideInHorizontally(
                    animationSpec = tween(320, easing = FastOutSlowInEasing)
                ) { fullWidth -> direction * fullWidth }

                // Exit: old pane slides out to the opposite side
                val exitSlide = slideOutHorizontally(
                    animationSpec = tween(280, easing = FastOutLinearInEasing)
                ) { fullWidth -> -direction * fullWidth }

                // Subtle fade layered on top for the "weighted" feel —
                // the pane doesn't just move, it also gains/loses presence
                (enterSlide + fadeIn(tween(200)))
                    .togetherWith(exitSlide + fadeOut(tween(150)))
            },
            modifier = Modifier.fillMaxSize(),
            label    = "tabSlide"
        ) { tab ->
            when (tab) {
                0 -> SocialScreen(onNavigateToTribe = onNavigateToTribe)
                1 -> HomeScreen()
                2 -> AccountScreen()
                else -> HomeScreen()
            }
        }

        // ── Glass navigation bar ──────────────────────────────────────────────
        GlassNavigationBar(
            selectedTab   = selectedTab,
            onTabSelected = { selectedTab = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Glass Navigation Bar
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun GlassNavigationBar(
    selectedTab:   Int,
    onTabSelected: (Int) -> Unit,
    modifier:      Modifier = Modifier
) {
    val surfaceColor = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0.00f to Color.Transparent,
                    0.25f to surfaceColor.copy(alpha = 0.55f),
                    0.60f to surfaceColor.copy(alpha = 0.85f),
                    1.00f to surfaceColor.copy(alpha = 0.97f),
                )
            )
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            windowInsets   = WindowInsets.navigationBars,
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = selectedTab == index
                NavigationBarItem(
                    selected = isSelected,
                    onClick  = { onTabSelected(index) },
                    icon = {
                        Icon(
                            imageVector        = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = tab.label
                        )
                    },
                    label = {
                        Text(
                            text       = tab.label,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor      = MaterialTheme.colorScheme.secondaryContainer,
                        selectedIconColor   = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedTextColor   = MaterialTheme.colorScheme.onSurface,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                )
            }
        }
    }
}
