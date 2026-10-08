# Lesson 9 — UI and Animations: The Material 3 Design System

Files: `ui/theme/`, `ui/MotionSpec.kt`, `ui/ExpressiveUtils.kt`, various screens

## What this lesson covers

- The brand system (Sunrise Amber, Outfit, dark theme)
- The animation philosophy (springs everywhere)
- `MotionSpec` — named spring constants
- `pressScale()` — the signature interaction
- Bottom sheets and cards as designed components

## The design tokens

| Token | Value | Why |
|---|---|---|
| Seed color | Sunrise Amber `#FF8C00` | Warm, energetic — mornings |
| Background | Near-black `#0F0F12` (true black on some screens) | Contrast makes amber glow |
| Font | Outfit (9 weights) | Friendly geometric sans, not default Roboto |
| Card elevation | 0dp, 20–24dp rounding | Flat, modern — color does the separation |
| Buttons | 56dp tall, 20dp rounding | Large, thumb-friendly |
| Bottom sheets | No drag handle, 32dp top rounding | Clean, minimal |
| Section headers | Uppercase + letter spacing | Editorial hierarchy |
| Dynamic color | Android 12+ wallpaper-based | Feels native on modern devices |

Material 3's dynamic theming means: on Pixel devices the app can adopt the user's wallpaper palette while staying on-brand on everything else.

## MotionSpec — the animation vocabulary

`ui/MotionSpec.kt` defines **named** spring specs so every animation in the app uses a shared, consistent feel:

```kotlin
val MotionSpec.touch: SpringSpec<Float> = spring(...)       // fast, small interactions
val MotionSpec.content: SpringSpec<Float> = spring(...)     // list/body content
val MotionSpec.page: SpringSpec<Float> = spring(...)        // screen/page transitions
val MotionSpec.splash: SpringSpec<Float> = spring(...)      // splash letters
val MotionSpec.cardEnter: SpringSpec<Float> = spring(...)   // cards sliding in
```

Why springs instead of tweens? Springs are physically-based — they overshoot slightly and settle naturally instead of linearly interpolating. They read as "alive," which matches a brand that's supposed to make mornings feel energetic. Naming them means the feel is consistent across every screen and easy to tune in one place.

## pressScale() — the signature interaction

`ui/ExpressiveUtils.kt`. The real implementation drives a `graphicsLayer` scale with an `Animatable` (not a `remember { mutableStateOf }` + `animateFloatAsState` pair) so the press/release animation is driven by explicit coroutine jobs it can cancel mid-flight:

```kotlin
fun Modifier.pressScale(
    pressedScale: Float = 0.94f,       // default target scale — NOT 0.96f
    releasedScale: Float = 1.0f,
    stiffness: Float = Spring.StiffnessMedium,
    dampingRatio: Float = Spring.DampingRatioNoBouncy
): Modifier = composed {
    val scale = remember { Animatable(releasedScale) }
    val scope = rememberCoroutineScope()
    this
        .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)   // lets a parent clickable still see the tap
                val pressJob = scope.launch { scale.animateTo(pressedScale, spring(dampingRatio, stiffness)) }
                waitForUpOrCancellation()
                pressJob.cancel()
                scope.launch { scale.animateTo(releasedScale, spring(dampingRatio, stiffness)) }
            }
        }
}
```

Two sibling variants share the same function with different tuning: `chipPressScale()` (0.92f, `DampingRatioLowBouncy`) and `fabPressScale()` (0.90f, `StiffnessMediumLow`/`DampingRatioMediumBouncy`) — chips and FABs compress more than a generic button because they're smaller, high-frequency tap targets where a bigger squash reads better.

Note `pressScale()` builds its own `spring(...)` inline from the `stiffness`/`dampingRatio` params — it does **not** call into `MotionSpec` (that object is used elsewhere, for page/content/splash transitions). Every interactive element scales down slightly on press and springs back. It's a small detail, but it's **applied consistently** — buttons, cards, list rows — which is what makes an app feel designed rather than assembled.

## The signature screens

### Splash
Scatter-letter animation — the word "MELUKO" letters animate in with `MotionSpec.splash`. It doubles as the loading gate: navigation waits for `isSplashFinished` (Lesson 2).

### Home
- Hero card with the next alarm time (24h→12h display), the countdown, and your streak.
- Offline banner (AnimatedVisibility) when `!isOnline` (Lesson 7).
- Swipe-to-wake gesture on the alarm screen.

### Social
Member cards with `WakeStatus` chips (Awake/Sleeping/Missed), leaderboard sorted by streak, tribe progress card with the awake count.

### Account
Profile editing, streak stats (current, best, total wakes), rank, alarm tone picker via `RingtoneManager.ACTION_RINGTONE_PICKER`.

### Bottom sheets
ProposeAlarmBottomSheet (wheel picker) and VetoBottomSheet both use the shared sheet styling — no drag handle, 32dp rounding, `Modifier.safeDrawingPadding()` so content avoids the nav bar.

## Loading states

`ShimmerEffect.kt` — skeletons pulse while data streams in. Used on screens with async Firestore loads so the layout doesn't jump when real data arrives. This is a polish detail interviewers notice: empty states, loading states, and error states are all designed, not an afterthought.

## UI → ViewModel contract

Screens are stateless: they render `StateFlow` state and emit callbacks.

```kotlin
val vm: SocialViewModel = hiltViewModel()
val uiState by vm.uiState.collectAsState()

when (val state = uiState) {
    is SocialUiState.Loading -> ShimmerList()
    is SocialUiState.Success -> MemberList(state.members)
    is SocialUiState.Error   -> ErrorBanner(state.message) { vm.retry() }
}
```

No ViewModel is ever instantiated in a composable; no screen owns business logic. That's what makes the screens trivially previewable and the ViewModels testable.

## Common trap

**"What's the difference between state hoisting here and just putting state in the composable?"**

Strong answer: all state lives in the ViewModel as `StateFlow`, and the composable only collects and renders. Hoisting the state to the ViewModel means the screen survives configuration changes (rotation), the data survives via the ViewModel's scope, and the UI has exactly one render path — no `remember { mutableStateOf }` shadowing real app state.

## Self-check

1. What three tokens define the Meluko brand, and what do they communicate?
2. Why springs instead of tweens?
3. How does `pressScale()` work at a gesture level?
4. What are the Loading/Success/Error states on the Social screen and how are they rendered?
5. Why is a stateless screen with a ViewModel better than state inside the composable?

## Code map

| Concept | Location |
|---|---|
| Theme | `ui/theme/{Color,Type,Theme}.kt` |
| Springs | `ui/MotionSpec.kt` |
| Press interaction | `ui/ExpressiveUtils.kt` |
| Shimmer | `ui/ShimmerEffect.kt` |
| Home + banner | `ui/HomeScreen.kt` |
| Member list | `ui/SocialScreen.kt` |
| Alarm swipe | `ui/SwipeToWakeButton.kt` |
