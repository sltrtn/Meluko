# AGENTS.md — Meluko Project Instructions

> Permanent project instructions for any AI agent working on Meluko.

---

## Project Overview

Meluko is a **social alarm clock** Android app. It reimagines the alarm clock as a shared, social experience centered around "tribes" (groups). Key concepts:

- **Social Accountability**: Users join tribes where a shared alarm time wakes everyone. If you miss your alarm, your whole tribe knows — no snooze.
- **Tribe-based Alarm Proposals**: Any tribe member can propose a new alarm time. Other members have a 2-hour veto window; a single veto kills the proposal, otherwise it auto-accepts.
- **Streaks & Wake Tracking**: Members track their wake streaks, best streaks, and total wakes. Statuses (Awake/Sleeping/Missed) visible to tribe.
- **Google Sign-In**: Android Credential Manager + Firebase Auth with Google ID tokens.
- **Real-time Sync**: Firestore listeners keep tribe data, member statuses, proposals, and user profiles synchronized.

---

## Repository Structure

```
Meluko/
├── .ai/                      ← project memory (read first)
├── app/
│   └── src/main/java/com/alarm/meluko/
│       ├── MainActivity.kt           ← Single Activity entry point
│       ├── MelukoApplication.kt      ← @HiltAndroidApp
│       ├── alarm/                    ← Alarm system (scheduler, receiver, activity)
│       ├── auth/                     ← Google Sign-In auth
│       ├── di/                       ← Hilt DI modules
│       ├── navigation/               ← NavHost with all routes
│       └── ui/                       ← Screens, components, theme
│           ├── theme/                ← Color, Theme, Type (Material 3)
│           ├── *.kt                  ← 20+ screen/composable files
│           └── ExpressiveUtils.kt    ← Spring animations
├── ExamplePhotos/            ← UI design references
├── gradle/libs.versions.toml ← Version catalog
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

---

## Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.2.10 |
| UI | Jetpack Compose (Material 3), BOM 2026.05.01 |
| Build | AGP 9.2.1, Gradle 9.4.1, Kotlin DSL |
| DI | Dagger Hilt 2.59.2, KSP |
| Navigation | Compose Navigation 2.9.0 |
| Backend | Firebase Auth, Firestore, Storage (BOM 33.15.0) |
| Auth | AndroidX Credential Manager 1.5.0 + Google Identity Services |
| Images | Coil 2.7.0 |
| Min SDK | 24 |
| Target SDK | 36 |
| Namespace | `com.alarm.meluko` |

---

## Design Language

- **Seed color:** Sunrise Amber (#FF8C00) — warm, energetic
- **Fonts:** Clash Display (6 weights) for headings/text, Inter for fallback
- **Dark mode:** True black (#000000) backgrounds
- **Dynamic colors:** Android 12+ wallpaper-based color support
- **Cards:** 0dp elevation, 20-24dp rounding
- **Buttons:** 56dp tall, 20dp rounding
- **Animations:** Spring-based `pressScale()` on all interactive elements
- **Bottom sheets:** No drag handle, 32dp top rounding
- **Typography:** Uppercase section headers with letter spacing
- **Splash:** Scatter-letter animation for "MELUKO"

---

## Key Screens

| Screen | Description |
|---|---|
| SplashScreen | Animated scatter-letter "MELUKO" |
| LoginScreen | Google Sign-In button |
| OnboardingScreen | 3-page horizontal pager |
| CreateProfileScreen | Username, bio, photo picker |
| JoinOrCreateTribeScreen | Join by code or create new tribe |
| WelcomeBackScreen | Returning user interstitial |
| MainScreen | 3-tab shell (Social/Home/Account) |
| HomeScreen | Next alarm, tribe overview, Swipe-to-Wake |
| SocialScreen | Tribe member list with wake statuses |
| AccountScreen | Profile editing, streaks, settings |
| TribeDetailScreen | Tribe deep-dive |
| AlarmScreen | Full-screen alarm UI (ringing) |
| ProposeAlarmBottomSheet | Propose new alarm time |
| VetoBottomSheet | Veto an active proposal |
| SwipeToWakeButton | Custom swipe gesture to dismiss alarm |

---

## Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Run lint
./gradlew lint

# Run tests
./gradlew test

# Build release bundle
./gradlew bundleRelease
```

---

## Critical Constraints

1. **Firebase config** (`google-services.json`) is already committed — do not modify or regenerate without checking current values.
2. **No API keys** in Kotlin code — all Firebase config is in `google-services.json`.
3. **Hilt** is the DI framework — all ViewModels use `@HiltViewModel` + `@Inject constructor`.
4. **Single Activity** architecture — all navigation via Compose Navigation `NavHost`.
5. **Google Sign-In** uses Android Credential Manager — do not switch to legacy Google Sign-In SDK.
6. **Alarm permissions** require runtime checks for `SCHEDULE_EXACT_ALARM` and `USE_EXACT_ALARM` (Android 14+).
7. **Firestore listeners** must be added/removed in ViewModel `init`/`onCleared()` lifecycle.
8. **Design must be followed strictly** — Sunrise Amber, Clash Display, spring animations, 0dp elevation cards, uppercase headers.

---

## Things Agents Should Never Do

- Never add hardcoded API keys or secrets to Kotlin code
- Never switch from Hilt to manual DI or another DI framework
- Never change the single-Activity architecture
- Never replace Google Sign-In with email/password auth
- Never remove `.ai/` documentation without migrating content
- Never skip the startup read procedure (AGENTS.md → .ai/*)
- Never end a session without updating handoff.md
- Never add emojis to code or UI unless explicitly asked
