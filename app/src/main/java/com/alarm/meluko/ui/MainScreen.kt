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
import com.alarm.meluko.navigation.MelukoBottomNavigation
import kotlinx.coroutines.launch

private const val HOME_TAB = 1

@Composable
fun MainScreen(
    onNavigateToTribe: (String) -> Unit = {}
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(HOME_TAB) }
    val pagerState = rememberPagerState(
        initialPage = HOME_TAB,
        pageCount = { 3 }
    )
    val scope = rememberCoroutineScope()
    val bgColor = MaterialTheme.colorScheme.background

    LaunchedEffect(pagerState.currentPage) {
        selectedTab = pagerState.currentPage
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> SocialScreen(onNavigateToTribe = onNavigateToTribe)
                1 -> HomeScreen(onTribeClick = onNavigateToTribe)
                2 -> AccountScreen()
                else -> HomeScreen(onTribeClick = onNavigateToTribe)
            }
        }

        MelukoBottomNavigation(
            selectedTab = selectedTab,
            onTabSelected = { page ->
                scope.launch {
                    pagerState.animateScrollToPage(page)
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
