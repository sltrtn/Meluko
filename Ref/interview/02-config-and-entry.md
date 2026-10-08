# Lesson 2 — Config and Entry: How the app starts

## What this lesson covers

- The single-Activity architecture (`MainActivity`)
- The navigation graph (`AppNavGraph`) and its routes
- Auth-driven routing (why auth state owns navigation)
- Dependency injection with Hilt
- The design system entry points

## The single Activity

File: `MainActivity.kt`

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MelukoTheme {
                Surface(...) {
                    AppNavGraph()
                }
            }
        }
    }
}
```

What this means:
- Meluko is **one Activity** with a Compose `NavHost` inside. Every screen is a Composable in the graph.
- `@AndroidEntryPoint` tells Hilt to generate injection code for this Activity — required for `hiltViewModel()` to work inside it.
- `enableEdgeToEdge()` makes the app draw behind the system bars (Material 3 design choice).
- The theme wraps the whole graph, so every screen gets the same colors/typography.

## The navigation graph

File: `navigation/AppNavGraph.kt`

Route constants:

```kotlin
object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val ONBOARDING = "onboarding"
    const val CREATE_PROFILE = "create_profile"
    const val JOIN_TRIBE = "join_tribe"
    const val SET_FIRST_ALARM = "set_first_alarm"
    const val WELCOME_BACK = "welcome_back"
    const val HOME = "home"
    const val TRIBE_DETAIL = "tribe_detail/{tribeId}"
}
```

Two flows:

```
NEW USER:    splash → login → onboarding → create_profile → join_tribe → set_first_alarm → home
RETURNING:   splash → welcome_back → home
```

The `NavHost` declares each route as a `composable(...)` block, so navigation is declarative — the current route decides what Composable renders.

## Auth-driven routing — the key pattern

`AuthViewModel` is hoisted at the graph level (not inside LoginScreen) so that auth state can own navigation:

```kotlin
val authViewModel: AuthViewModel = hiltViewModel()
val authState by authViewModel.authState.collectAsState()

LaunchedEffect(authState, isSplashFinished) {
    if (!isSplashFinished) return@LaunchedEffect
    when (authState) {
        is AuthState.Success -> {
            val dest = if (successState.isNewUser) Routes.ONBOARDING else Routes.WELCOME_BACK
            navController.navigate(dest) { popUpTo(0) { inclusive = true } }
        }
        is AuthState.Idle, is AuthState.Error -> {
            navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
        }
        else -> { /* Loading handled implicitly */ }
    }
}
```

Why this matters:
- **Single source of truth.** Auth state is a sealed class (`Idle / Loading / Success / Error`) in the ViewModel; the UI only reacts.
- **No dead-end screens.** An error state always routes to Login, and `popUpTo(0) { inclusive = true }` clears the back stack so the user can't navigate back into a protected screen.
- **Splash gates navigation.** The `isSplashFinished` flag prevents navigation until the animation completes.

The sealed class forces exhaustive handling:

```kotlin
sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data class Success(val displayName: String?, val isNewUser: Boolean) : AuthState()
    data class Error(val message: String) : AuthState()
}
```

## Dependency injection (Hilt)

File: `di/FirebaseModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {
    @Provides @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
}
```

Every ViewModel is a `@HiltViewModel` with an `@Inject constructor`. Hilt resolves Firebase dependencies from the module. The benefit: ViewModels are testable (swap Firebase for a fake) and lifecycle-scoped (auto-cleaned with the composable).

Also in the DI graph: `ConnectivityObserver` (a `@Singleton` that injects the `@ApplicationContext`).

## The design system entry points

| File | What it defines |
|---|---|
| `ui/theme/Color.kt` | Sunrise Amber seed, near-black background |
| `ui/theme/Type.kt` | Outfit font family, type scale |
| `ui/theme/Theme.kt` | `MelukoTheme` composable (dark mode, dynamic color) |
| `ui/MotionSpec.kt` | Named spring specs (`touch`, `content`, `page`, `splash`, `cardEnter`) |
| `ui/ExpressiveUtils.kt` | `pressScale()` spring-on-press modifier |

`MelukoTheme` wraps everything at the top, so all screens inherit the same tokens — no hardcoded colors in screens.

## Common trap

**"Why is auth state in the NavGraph instead of the Activity?"**

Strong answer: the NavGraph is where navigation decisions live. Hoisting `AuthViewModel` there lets auth state drive routing declaratively — when the state changes, the effect navigates. If it lived in MainActivity, you'd either pass callbacks down or duplicate logic.

## Self-check

1. Why is there exactly one Activity?
2. What is `popUpTo(0) { inclusive = true }` doing and why is it important after login?
3. What is `isSplashFinished` gating?
4. How does Hilt provide FirebaseAuth to ViewModels?
5. List the seven onboarding-flow routes in order.

## Code map

| Concept | File |
|---|---|
| Activity entry | `MainActivity.kt` |
| Routes + NavHost | `navigation/AppNavGraph.kt` |
| Auth state sealed class | `auth/AuthViewModel.kt` |
| Firebase DI | `di/FirebaseModule.kt` |
| Theme | `ui/theme/Theme.kt` |
| Animations | `ui/MotionSpec.kt`, `ui/ExpressiveUtils.kt` |
