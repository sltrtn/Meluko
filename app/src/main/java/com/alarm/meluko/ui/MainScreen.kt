package com.alarm.meluko.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import com.alarm.meluko.navigation.MelukoBottomNavigation
import com.alarm.meluko.ui.components.LocalHazeState
import com.alarm.meluko.ui.components.aurora
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private const val HOME_TAB = 1

@Composable
fun MainScreen(
    onNavigateToTribe: (String) -> Unit = {},
    onJoinTribe: () -> Unit = {}
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(HOME_TAB) }
    val pagerState = rememberPagerState(initialPage = HOME_TAB, pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val hazeState = rememberHazeState()

    LaunchedEffect(pagerState.currentPage) {
        selectedTab = pagerState.currentPage
    }

    CompositionLocalProvider(LocalHazeState provides hazeState) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Everything the glass nav bar frosts lives inside the haze source.
            Box(
                Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState)
                    .aurora()
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1
                ) { page ->
                    // Pages recede slightly as they're swiped away — depth, not a hard slide.
                    Box(
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val offset = pagerState.getOffsetDistanceInPages(page).absoluteValue.coerceIn(0f, 1f)
                                val scale = lerp(1f, 0.94f, offset)
                                scaleX = scale
                                scaleY = scale
                                alpha = lerp(1f, 0.55f, offset)
                            }
                    ) {
                        when (page) {
                            0 -> SocialScreen(onNavigateToTribe = onNavigateToTribe, onJoinTribe = onJoinTribe)
                            2 -> AccountScreen()
                            else -> HomeScreen(onTribeClick = onNavigateToTribe, onJoinTribe = onJoinTribe)
                        }
                    }
                }
            }

            MelukoBottomNavigation(
                selectedTab = selectedTab,
                onTabSelected = { page -> scope.launch { pagerState.animateScrollToPage(page) } },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
