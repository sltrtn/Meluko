package com.alarm.meluko.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.alarm.meluko.auth.AuthState
import com.alarm.meluko.auth.AuthViewModel
import com.alarm.meluko.ui.LoginScreen
import com.alarm.meluko.ui.MainScreen

/**
 * Route constants — typed strings in one place so a typo doesn't silently
 * navigate to a dead route at runtime.
 */
object Routes {
    const val LOGIN = "login"
    const val HOME  = "home"   // points to MainScreen (3-pane shell)
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

    // On first composition, check if the user is already logged in.
    // LaunchedEffect with Unit key runs exactly once — like onCreate.
    LaunchedEffect(Unit) {
        authViewModel.checkExistingSession()
    }

    // React to auth state changes and drive navigation
    // LaunchedEffect(authState) re-runs whenever authState emits a new value
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                navController.navigate(Routes.HOME) {
                    // popUpTo LOGIN and remove it from the back stack —
                    // pressing Back from Home should exit the app, not go to Login
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }
            }
            is AuthState.Idle -> {
                // If signed out, navigate back to Login and clear Home from stack
                if (navController.currentDestination?.route == Routes.HOME) {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            }
            else -> { /* Loading / Error handled inside LoginScreen */ }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                authViewModel = authViewModel
            )
        }
        composable(Routes.HOME) {
            MainScreen()
        }
    }
}
