# Handoff — Session End

> **Last updated:** 2026-07-02
> **Last action:** Polish pass — alarm flash, onboarding, propose wiring, real stats, build fixes

---

## Current Goal

Polish Meluko's onboarding, fix alarm screen flickering, wire propose flow, and replace mock/hardcoded data with real Firestore values.

---

## Completed Work

- **Alarm screen flash fix** — Added `@AndroidEntryPoint` + injected Firebase to `AlarmActivity`. Set `window.decorView.setBackgroundColor(Color.BLACK)` before any rendering. Created `Theme.Meluko.Alarm` with black window background. Updated manifest. Added `FirebaseFirestore` provider to `FirebaseModule`.
- **Propose button wired** — `ProposeAlarmBottomSheet` now takes `tribeId` + `ProposalViewModel`, calls `createProposal()` with loading state. Removed dead `onPropose` callback. Updated `TribeDetailScreen` to match.
- **Onboarding completed** — New `SetFirstAlarmScreen` appears after tribe join/create with full wheel time picker. Proposes chosen time to tribe. Skip link preserves default 07:30.
- **Real HomeScreen stats** — Added `streakDays`, `totalWakes`, `rank` to `HomeViewModel` from Firestore member doc. Replaced hardcoded "12"/"2"/"#2" in `StatsRow` with live data.
- **Mock data removed** — Replaced `WeeklyConsistencyGraph` (hardcoded mock week) with `ConsistencyCard` showing real streak + total wakes.
- **Atomic writes** — `ProposalViewModel.acceptProposal()` now uses `db.batch()`.
- **TribeProgress.id** — Added `id` field to `TribeProgress` data class. Fixed `SocialScreen` to use it for tribe navigation.
- **Build fixes** — Fixed pre-existing compile errors in `ExpressiveUtils.kt` (launch in PressInteractionScope), `SwipeToWakeButton.kt` (suspend calls in drag handlers), `AlarmScreen.kt` (missing graphicsLayer import).

### Previously completed (not in this session):
- Full app architecture: Single Activity, Compose Navigation, Hilt DI
- Google Sign-In with Credential Manager + Firebase Auth
- Onboarding flow (3 pages) + profile creation + tribe join/create
- 3-tab main shell (Social/Home/Account) with animated switching
- Alarm system: AlarmManager, BroadcastReceiver, full-screen AlarmActivity, SwipeToWakeButton
- Social features: Tribe member list, real-time status, proposals/veto (2-hour window), streaks
- Full Material 3 design system: Sunrise Amber, Clash Display, spring animations, dark mode
- Scatter-letter splash screen, glass-morphism nav bar, expressive bottom sheets

---

## Files Modified/Created

| File | Action |
|---|---|
| `app/.../alarm/AlarmActivity.kt` | Modified — added @AndroidEntryPoint, injected Firebase, black window bg |
| `app/.../di/FirebaseModule.kt` | Modified — added FirebaseFirestore provider |
| `app/.../ui/ProposeAlarmBottomSheet.kt` | Modified — new signature (tribeId, ProposalViewModel), wiring to createProposal() |
| `app/.../ui/HomeScreen.kt` | Modified — real stats from HomeViewModel, new ConsistencyCard, wired propose |
| `app/.../ui/HomeViewModel.kt` | Modified — added primaryTribeId, streakDays, totalWakes, rank StateFlows |
| `app/.../ui/SocialScreen.kt` | Modified — TribeProgress now includes id, fixed tribe nav |
| `app/.../ui/SocialViewModel.kt` | Modified — passes tribeId to TribeProgress |
| `app/.../ui/TribeDetailScreen.kt` | Modified — updated ProposeAlarmBottomSheet call |
| `app/.../ui/SetFirstAlarmScreen.kt` | Created — onboarding step with wheel time picker |
| `app/.../ui/ExpressiveUtils.kt` | Modified — fixed launch reference in pressScale |
| `app/.../ui/SwipeToWakeButton.kt` | Modified — wrap suspend calls in scope.launch |
| `app/.../ui/AlarmScreen.kt` | Modified — added graphicsLayer import |
| `app/.../navigation/AppNavGraph.kt` | Modified — added SET_FIRST_ALARM route, wired new screen |
| `app/.../ui/ProposalViewModel.kt` | Modified — acceptProposal uses WriteBatch |
| `app/src/main/res/values/themes_alarm.xml` | Created — dark alarm theme |
| `app/src/main/AndroidManifest.xml` | Modified — AlarmActivity uses Theme.Meluko.Alarm |

---

## Remaining Work

### Critical:
- [x] **Google Sign-In fixed** — Added debug keystore SHA-1 (`90:20:E1:20:94:73:5E:6A:01:EA:4E:C8:50:F5:28:A3:CD:61:E4:3D`) and SHA-256 (`07:17:99:44:AE:2F:B0:AB:D6:AC:10:1A:D9:73:82:72:3A:96:AE:75:5F:92:53:71:92:E2:38:95:77:8C:17:B7`) to Firebase Console. Sign-in should now work.
- [ ] **Crashes still happening** — User reports crashes on tribe screen and elsewhere. Need crash logs (Logcat or Firebase Crashlytics) to diagnose.

### Testing:
- [ ] Unit tests for ViewModels (AuthViewModel, HomeViewModel, SocialViewModel, ProposalViewModel, AccountViewModel)
- [ ] Compose UI tests for critical flows (login, create tribe, propose alarm, veto, swipe-to-wake)
- [ ] Firebase emulator setup for local testing

### Features:
- [ ] Notification deep links (tap → open correct screen)
- [ ] Weekly tribe stats (average wake time, best streak race)
- [ ] Tribe leaderboard
- [ ] Custom alarm sounds per tribe
- [ ] Timezone support
- [ ] "Panic" alarm escalation
- [ ] Home screen widget
- [ ] Wear OS companion

### Infrastructure:
- [ ] GitHub Actions CI/CD
- [ ] Privacy policy and terms of service
- [ ] Play Store preparation
- [ ] Firebase Crashlytics + Performance Monitoring
- [ ] App signing and release management

### Tech Debt:
- [ ] Extract Firestore collection/field names into constants
- [ ] Add sealed class UiState (Loading/Success/Error) to all ViewModels
- [ ] Add loading skeleton/shimmer UI
- [ ] Improve error messages in UI
- [ ] Document Firestore security rules in repo

---

## Known Bugs

1. **Crashes on tribe screen and elsewhere** — User reports crashes. Need Logcat trace to identify root cause.
2. ~~**Google Sign-In fails** — Fixed. SHA-1/SHA-256 added to Firebase Console.~~

---

## Open Questions

- Should Google Sign-In handle rotation configuration changes? (Currently credentials may need re-authentication on config change)
- What happens to tribe alarms when a member leaves or is removed? No current handling.
- Should veto notifications be sent to the proposal creator? Currently only the veto result is reflected in Firestore.
- What is the expected behavior for alarm proposals when the tribe has no active members?

---

## Important Context

- **Repo:** `https://github.com/sltrtn/Meluko` (origin: sltrtn/Meluko, branch: master)
- **Local path:** `/home/mad/StudioProjects/Meluko`
- **Package:** `com.alarm.meluko`
- **Min SDK:** 24, **Target SDK:** 36
- **Build:** `./gradlew assembleDebug` (needs `JAVA_HOME=/home/mad/android-studio/jbr`)
- **Firebase project:** `meluko-routine` (configured in `app/google-services.json`)
- **Auth:** Google Sign-In only (no email/password)
- **DI:** Dagger Hilt with KSP
- **Alarm permissions:** Must handle `SCHEDULE_EXACT_ALARM` + `USE_EXACT_ALARM` runtime requests (Android 14+)
- **Debug keystore SHA-1:** `90:20:E1:20:94:73:5E:6A:01:EA:4E:C8:50:F5:28:A3:CD:61:E4:3D`
- **Debug keystore SHA-256:** `07:17:99:44:AE:2F:B0:AB:D6:AC:10:1A:D9:73:82:72:3A:96:AE:75:5F:92:53:71:92:E2:38:95:77:8C:17:B7`

---

## Suggested Next Step

1. **Fix SHA-1 mismatch** — Add fingerprints to Firebase Console, then verify Google Sign-In works end-to-end.
2. **Diagnose crashes** — Run `adb logcat` while testing the app to catch crash stack traces.
