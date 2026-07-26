# Meluko — Progress Log

> Chronological work log. Append entries as milestones are reached.

---

## 2026-07-27 — Tribe Polish + Alarm Reliability

### Alarm Tone Picker:
- System ringtone picker (`TYPE_ALARM`) in AccountScreen settings
- Selected tone URI saved to Firestore at `user_alarms/{uid}.alarmToneUri`
- AlarmActivity plays default immediately, then async loads custom tone from Firestore
- AccountViewModel: `alarmToneUri` state + `updateAlarmTone()`
- FirestoreConstants: added `ALARM_TONE_URI`, `TONE_URI`, `TONE_SOURCE`
- SettingsRow: added optional `subtitle` parameter

### Alarm Reliability:
- AlarmReceiver now reschedules alarm for next day **immediately when it fires** (not on dismiss)
- Survives force-kill, missed dismissals, app crashes
- Notification channel: `IMPORTANCE_HIGH`, bypass DND, lockscreen public
- Notification: `setOngoing(true)`, full-screen intent
- Removed duplicate `rescheduleAlarmForTomorrow()` from AlarmActivity
- Fixed duplicate `TODAY_WAKE_TIME` key in Firestore update map

### Tribe System Polish (13 fixes):
1. `TribeInfo` now has `id` — tribe navigation uses Firestore doc ID, not name
2. `activeProposal!!` → `activeProposal?.let` — eliminated NPE race condition
3. `awakeCount` computed from live member statuses (was hardcoded to 0)
4. VetoBottomSheet waits for async write before showing success
5. ProposeAlarmBottomSheet resets to Idle on error
6. Proposal time normalized to 24h format before Firestore write
7. Stale error cleared on Join/Create tab switch
8. TribeDetailScreen shows error state + retry
9. SocialScreen shows empty states
10. Notification permission checked before requesting
11. Propose button hidden when no tribe
12. Auto-accept guarded against duplicate calls
13. Tribe name limited to 30 chars

---

## 2026-07-26 — Production Features + Alarm Polish

### Crashlytics & BootReceiver:
- Added `firebase-crashlytics-ktx` dependency + `com.google.firebase.crashlytics` Gradle plugin (v3.0.4)
- Auto-initializes — crashes now logged to Firebase Console
- Created `BootReceiver.kt` — reads `user_alarms/{uid}.followedAlarmTime` on `BOOT_COMPLETED`, reschedules via `AlarmScheduler`
- Uses `goAsync()` for async Firestore read in BroadcastReceiver
- Registered in manifest with `BOOT_COMPLETED` intent filter

### Alarm Screen Strip-down:
- Removed `enableEdgeToEdge()` from AlarmActivity — was causing layout shift
- Added `WindowInsetsControllerCompat` to hide status bar + navigation bar entirely
- Stripped AlarmScreen to pure black background, centered clock (HH:MM), AM/PM indicator, SwipeToWake at bottom
- Removed: pulse animation, day label, "Time to Rise" headline, tribe context chip, gradient background
- Now looks like a clean system alarm

### Debug TEST button:
- Added `BuildConfig.DEBUG`-gated "TEST" button to HomeScreen hero card
- Calls `AlarmScheduler(context).scheduleIn(5)` — triggers alarm 5 seconds later
- Enabled `buildConfig = true` in build.gradle.kts

### SwipeToWakeButton fix:
- Changed `pointerInput(maxDrag)` to `pointerInput(Unit)` with `snapshotFlow` wait
- Fixed `onDragEnd` race condition — captures `dragOffset` before launching animation coroutine

### Proposal fix:
- Removed `whereEqualTo("status", "pending")` from Firestore query — avoids composite index requirement
- In-memory filtering for pending proposals

### ProposalCard animation:
- Added spring `scaleIn + fadeIn` entrance via `AnimatedVisibility` in TribeDetailScreen
- Added pulsing animation (0.92x–1.08x, 800ms Reverse) to success checkmark in ProposeAlarmBottomSheet

### Shimmer loading states:
- Created `ShimmerEffect.kt` with reusable composables (ShimmerBox, ShimmerCard, ShimmerLine, etc.)
- Added to HomeScreen, SocialScreen, AccountScreen, TribeDetailScreen

### Font migration:
- Replaced Clash Display (6 OTF weights) with Outfit static TTF (9 weights)
- Body at Medium weight, titles at ExtraBold
- Fixed rendering inconsistency with variable font on Android

### MotionSpec centralization:
- Created `MotionSpec.kt` with 6 named spring specs ported from Cashiro
- Updated `ExpressiveUtils.kt` pressScale defaults

### Background depth:
- Changed from true black to `#0F0F12` base
- Added 3-tier surface hierarchy (`surfaceVariant=#18181C`, `surfaceContainerHigh=#222226`)

---

## 2026-07-02 — Polish Pass: Onboarding, Alarm Flash, Propose Wiring, Real Stats

Completed a comprehensive polish pass across the app.

### Fixes & Improvements:

**Alarm Screen Flash:**
- Added `@AndroidEntryPoint` to `AlarmActivity` with Hilt-injected FirebaseAuth/FirebaseFirestore
- Set `window.decorView.setBackgroundColor(Color.BLACK)` before any rendering to eliminate white flash
- Created `Theme.Meluko.Alarm` (black window background) and applied in manifest
- Added `FirebaseFirestore` provider to `FirebaseModule`

**Propose Flow Wired:**
- `ProposeAlarmBottomSheet` now takes `tribeId` + `ProposalViewModel`, calls `createProposal()` on submit
- Shows loading spinner while proposal is being created
- Removed dead `onPropose` callback
- Updated `TribeDetailScreen` to use new signature

**Onboarding Completed:**
- Created `SetFirstAlarmScreen` — full-screen wheel time picker after tribe join/create
- Proposes chosen time to tribe via `ProposalViewModel.createProposal()`
- Skip link preserves default 07:30
- Added `Routes.SET_FIRST_ALARM` with clean backstack navigation

**Real HomeScreen Stats:**
- Added `streakDays`, `totalWakes`, `rank` StateFlows to `HomeViewModel` via Firestore member doc listener
- Replaced hardcoded "12"/"2"/"#2" with live data from Firestore
- Removed mock `WeeklyConsistencyGraph` — replaced with `ConsistencyCard` showing real streak + total wakes

**TribeProgress Data Model:**
- Added `id: String` field so tribe navigation uses the Firestore document ID
- Updated `SocialViewModel` to pass tribe ID
- Updated `SocialScreen` tribe click handler to use `tribe.id` instead of `tribe.name`

**Atomic Firestore Writes:**
- `ProposalViewModel.acceptProposal()` now uses `WriteBatch` for atomic multi-document updates

**Pre-existing Build Fixes:**
- Fixed `launch` unresolved reference in `ExpressiveUtils.kt` pressScale animation
- Wrapped suspend `springOffset.snapTo()` calls in `scope.launch` in `SwipeToWakeButton.kt`
- Added missing `graphicsLayer` import to `AlarmScreen.kt`

### Known Issues (carried forward):
- **Google Sign-In fixed** — SHA-1/SHA-256 fingerprints added to Firebase Console
- **Crashes still occurring** — tribe screen and elsewhere, Crashlytics now active

---

## 2026-07-02 — Project Memory System Setup

Created the `.ai/` project memory system and `AGENTS.md` with permanent project instructions.

**Added:**
- `.ai/project.md` — project overview, architecture, Firebase data model, alarm flow
- `.ai/roadmap.md` — all completed milestones, current milestone, future roadmap
- `.ai/current_task.md` — documentation setup task
- `.ai/progress.md` — this file
- `.ai/decisions.md` — architectural decisions log (5 entries)
- `.ai/changelog.md` — major milestones
- `.ai/handoff.md` — session handoff for next agent
- `AGENTS.md` — permanent project instructions

---

## [Prior Work — Before Documentation]

### App Foundation
- Created Meluko Android project with AGP 9.2.1, Kotlin 2.2.10, Compose BOM 2026.05.01
- Set up Hilt DI (@HiltAndroidApp, FirebaseModule)
- Implemented Navigation Compose NavHost with all screen routes
- Added animated screen transitions

### Authentication
- Integrated Android Credential Manager (Google Sign-In)
- Connected Firebase Authentication with Google ID tokens
- Implemented auth state flow (Splash → check session → route accordingly)
- Created AuthViewModel with sign-in/sign-out/session check

### Onboarding Flow
- Created 3-page horizontal pager (Welcome, Join a Tribe, No Snooze)
- CreateProfileScreen with username, bio, Firebase Storage photo upload
- JoinOrCreateTribeScreen with invite code input + Firestore tribe creation
- WelcomeBackScreen for returning users

### Main App Shell
- MainScreen with 3-tab layout (Social, Home, Account)
- Animated tab switching
- Glass-morphism navigation bar

### Alarm System
- AlarmScheduler with AlarmManager (setExactAndAllowWhileIdle)
- AlarmReceiver BroadcastReceiver → notification with full-screen intent
- AlarmActivity — full-screen lock-screen alarm UI
- SwipeToWakeButton with custom swipe gesture detection
- ProposalViewModel — Firestore-based alarm proposal CRUD
- ProposeAlarmBottomSheet — time picker + submit
- VetoBottomSheet — single tap veto with ConfirmationDialog

### Social Features
- SocialViewModel — real-time Firestore tribe member listener
- Tribe member list with status indicators (awake/sleeping/missed)
- Tribe progress cards
- HomeViewModel — alarm time listener + scheduler integration
- AccountViewModel — profile editing, streaks, notification prefs
- TribeDetailScreen — member list + proposal history

### Design System
- Material 3 theme seeded from Sunrise Amber (#FF8C00)
- Dynamic color support (wallpaper-based)
- True black dark mode
- Clash Display font family (6 weights)
- Spring-based pressScale() animation utility
- ScreenHeader composable with uppercase styling
- Scatter-letter SplashScreen animation
