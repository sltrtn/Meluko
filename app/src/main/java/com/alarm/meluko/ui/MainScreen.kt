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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alarm.meluko.ui.theme.DefaultFontFamily

private data class Tab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val tabs = listOf(
    Tab("Social", Icons.Rounded.Group, Icons.Outlined.Group),
    Tab("Home", Icons.Filled.Home, Icons.Outlined.Home),
    Tab("Account", Icons.Filled.Person, Icons.Outlined.Person),
)

private const val HOME_TAB = 1

@Composable
fun MainScreen(
    onNavigateToTribe: (String) -> Unit = {}
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(HOME_TAB) }
    val bgColor = MaterialTheme.colorScheme.background

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                val enter = slideInHorizontally(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) { it * direction / 3 } + fadeIn(tween(180))
                val exit = slideOutHorizontally(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) { -it * direction / 3 } + fadeOut(tween(140))
                enter.togetherWith(exit)
            },
            modifier = Modifier.fillMaxSize(),
            label = "tabSlide"
        ) { tab ->
            when (tab) {
                0 -> SocialScreen(onNavigateToTribe = onNavigateToTribe)
                1 -> HomeScreen(onTribeClick = onNavigateToTribe)
                2 -> AccountScreen()
                else -> HomeScreen(onTribeClick = onNavigateToTribe)
            }
        }

        ExpressiveNavigationBar(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun ExpressiveNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceColor = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0.00f to Color.Transparent,
                    0.20f to surfaceColor.copy(alpha = 0.6f),
                    0.55f to surfaceColor.copy(alpha = 0.88f),
                    1.00f to surfaceColor.copy(alpha = 0.98f),
                )
            )
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            windowInsets = WindowInsets.navigationBars,
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = selectedTab == index

                val iconScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.20f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "iconScale$index"
                )

                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onTabSelected(index) },
                    modifier = Modifier.pressScale(pressedScale = 0.86f),
                    icon = {
                        Icon(
                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = tab.label,
                            modifier = Modifier.graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                            }
                        )
                    },
                    label = {
                        Text(
                            text = tab.label,
                            fontFamily = DefaultFontFamily,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                            fontSize = if (isSelected) 11.sp else 10.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                )
            }
        }
    }
}

@Composable
fun GlassNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) = ExpressiveNavigationBar(selectedTab, onTabSelected, modifier)
