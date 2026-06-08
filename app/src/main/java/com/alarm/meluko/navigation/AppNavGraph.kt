package com.alarm.meluko.navigation

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.alarm.meluko.auth.AuthState
import com.alarm.meluko.auth.AuthViewModel
import com.alarm.meluko.ui.LoginScreen
import com.alarm.meluko.ui.MainScreen
import com.alarm.meluko.ui.TribeDetailScreen

/**
 * Route constants — typed strings in one place so a typo doesn't silently
 * navigate to a dead route at runtime.
 */
object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val HOME  = "home"   // points to MainScreen (3-pane shell)
    const val TRIBE_DETAIL = "tribe_detail/{tribeId}"
    fun createTribeDetailRoute(tribeId: String) = "tribe_detail/$tribeId"
}

/**
 * AppNavGraph is the single source of truth for where the user can go.
 *
 * NavController: manages the back stack
 * NavHost: renders whichever composable matches the current route
 *
 * The AuthViewModel is hoisted here (not inside LoginScreen) so that auth
 * state can drive navigation decisions — LoginScreen itself stays stateless.
 */
@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    // hiltViewModel() — Hilt-aware version of viewModel() from hilt-navigation-compose
    // It scopes the ViewModel to the nav back stack entry, not the whole activity
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()

    // State to hold back navigation until the splash animation finishes
    var isSplashFinished by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    // On first composition, check if the user is already logged in.
    LaunchedEffect(Unit) {
        authViewModel.checkExistingSession()
    }

    // React to auth state changes AND splash completion
    LaunchedEffect(authState, isSplashFinished) {
        if (!isSplashFinished) return@LaunchedEffect

        when (authState) {
            is AuthState.Success -> {
                if (navController.currentDestination?.route != Routes.HOME) {
                    navController.navigate(Routes.HOME) {
                        // popUpTo(0) effectively clears the ENTIRE back stack,
                        // so the user cannot hit the system Back button to return to Splash or Login.
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            is AuthState.Idle, is AuthState.Error -> {
                if (navController.currentDestination?.route != Routes.LOGIN) {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            else -> { /* Loading handled implicitly */ }
        }
    }

    // ── NavHost — default transitions apply to all routes unless overridden ──
    // Default: simple fade so Login→Home doesn't hard-cut
    NavHost(
        navController    = navController,
        startDestination = Routes.SPLASH,
        enterTransition  = { fadeIn(tween(200)) },
        exitTransition   = { fadeOut(tween(150)) },
        popEnterTransition  = { fadeIn(tween(200)) },
        popExitTransition   = { fadeOut(tween(150)) },
    ) {
        composable(Routes.SPLASH) {
            com.alarm.meluko.ui.SplashScreen(
                onSplashComplete = { isSplashFinished = true }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(authViewModel = authViewModel)
        }

        composable(Routes.HOME) {
            MainScreen(
                onNavigateToTribe = { tribeId ->
                    navController.navigate(Routes.createTribeDetailRoute(tribeId))
                }
            )
        }

        // ── TribeDetailScreen — directional slide (right-to-left push / left-to-right pop)
        // Overrides the default fade so it feels like drilling into a screen
        composable(
            route      = Routes.TRIBE_DETAIL,
            arguments  = listOf(navArgument("tribeId") { type = NavType.StringType }),

            // Enter: new screen slides in from the right
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec  = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(tween(220))
            },
            // Exit (navigating forward from tribe detail): slides out to left
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(270, easing = FastOutLinearInEasing)
                ) + fadeOut(tween(150))
            },
            // Pop-enter: when something pops back to reveal tribe detail again
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec  = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(tween(220))
            },
            // Pop-exit: back button — screen slides back out to the right
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(280, easing = FastOutLinearInEasing)
                ) + fadeOut(tween(150))
            }
        ) { backStackEntry ->
            val tribeId = backStackEntry.arguments?.getString("tribeId") ?: return@composable
            TribeDetailScreen(
                tribeId = tribeId,
                onBack  = { navController.popBackStack() }
            )
        }
    }
}
