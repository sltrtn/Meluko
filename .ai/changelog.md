# Meluko — Changelog

> Human-readable summary of major repository changes. Not every commit — only meaningful milestones.

---

## 2026-07-27 — Tribe Polish + Alarm Reliability

**Added:**
- Alarm tone picker — system ringtone picker in AccountScreen, saved to Firestore, played by AlarmActivity
- TribeInfo `id` field — tribe navigation now uses Firestore doc ID
- Empty states in SocialScreen ("Join or create a tribe" / "No members yet")
- Error state in TribeDetailScreen (shows error + retry on Firestore listener failure)
- `onError` callback in `ProposalViewModel.createProposal()`
- `isAccepting` guard against duplicate auto-accept calls
- `SettingsRow` subtitle parameter
- Tribe name max length (30 chars)

**Changed:**
- AlarmReceiver now reschedules alarm for next day **immediately on fire** (not on dismiss) — survives force-kill
- Notification channel: `IMPORTANCE_HIGH`, bypass DND, lockscreen public, `setOngoing(true)`
- Proposal time normalized to 24h format before Firestore write (`"07:30 AM"` → `"19:30"`)
- VetoBottomSheet waits for async write before showing success
- ProposeAlarmBottomSheet resets to Idle on error (was stuck in loading)
- `activeProposal!!` → `activeProposal?.let` (eliminated NPE race condition)
- `awakeCount` computed from live member statuses (was hardcoded to 0)
- Notification permission checked before requesting (was every composition)
- Propose button hidden when user has no tribe
- Stale error cleared on Join/Create tab switch
- Removed duplicate `rescheduleAlarmForTomorrow()` from AlarmActivity

**Fixed:**
- Tribe navigation broken — `TribeInfo` had no `id`, name was used as Firestore doc ID
- VetoBottomSheet showed false success (async write not awaited)
- ProposeAlarmBottomSheet stuck in loading state on error
- Time format inconsistency between onboarding ("07:30") and proposals ("07:30 AM")
- `computeCountdown` couldn't parse AM/PM format

---

## 2026-07-26 — Production Features + Alarm Polish

**Added:**
- Firebase Crashlytics — crash reporting auto-initializes, logs to Firebase Console
- BootReceiver — reschedules alarm after device reboot from Firestore
- Shimmer loading states — `ShimmerEffect.kt` with reusable composables, added to 4 screens
- MotionSpec centralization — 6 named spring specs ported from Cashiro
- Debug TEST button — `BuildConfig.DEBUG`-gated, triggers alarm in 5 seconds
- ProposalCard spring entrance animation
- Pulsing success checkmark in ProposeAlarmBottomSheet
- Error handling — UiState sealed class, snackbar errors in Home/Account/Social/Onboarding

**Changed:**
- Font migration — Clash Display → Outfit static TTF (9 weights), body at Medium, titles at ExtraBold
- Background depth — true black → `#0F0F12` base with 3-tier surface hierarchy
- AlarmScreen stripped — pure black, centered clock, hidden system bars, no animations
- SwipeToWakeButton — `pointerInput(Unit)` with `snapshotFlow` wait, fixed `onDragEnd` race
- Proposal query — removed `whereEqualTo` composite index dependency, in-memory filtering
- AlarmActivity — removed `enableEdgeToEdge()`, hidden system bars via `WindowInsetsControllerCompat`
- pressScale defaults — NoBouncy+Medium (Cashiro's `quick` spec)

**Fixed:**
- SwipeToWakeButton swipe not responding — pointerInput key race + onDragEnd stale value
- Alarm screen glitchy launch — stripped to system alarm aesthetic
- Proposal Firestore error — removed composite index requirement

---

## 2026-07-02 — Polish Pass

**Fixed:**
- Alarm screen white flash — added `@AndroidEntryPoint`, dark theme, black window background
- `ProposeAlarmBottomSheet` now actually calls `ProposalViewModel.createProposal()` instead of doing nothing
- HomeScreen stats (streak, wake-ups, rank) now sourced from Firestore instead of hardcoded values
- Removed mock `WeeklyConsistencyGraph` — replaced with real `ConsistencyCard`
- `acceptProposal()` uses `WriteBatch` for atomic updates

**Added:**
- `SetFirstAlarmScreen` — new onboarding step after tribe join/create with time picker
- `Theme.Meluko.Alarm` — dark window background theme for AlarmActivity
- `primaryTribeId`, `streakDays`, `totalWakes`, `rank` to `HomeViewModel`
- `id` field to `TribeProgress` data class

**Changed:**
- `FirebaseModule` now provides `FirebaseFirestore`
- `AppNavGraph` routes onboarding → `SetFirstAlarmScreen` → Home (instead of directly to Home)

---

## [Prior Milestones — App Development]

- **Initial commit** — Project scaffold with AGP, Compose, Hilt, Navigation
- **Google Sign-In** — Credential Manager + Firebase Auth integration
- **Onboarding flow** — 3-page pager, profile creation, tribe join/create
- **Main app shell** — 3-tab layout with animated switching
- **Alarm system** — AlarmManager, AlarmReceiver, AlarmActivity, SwipeToWakeButton
- **Social features** — Tribe member list, real-time status, proposals, veto
- **Design system** — Material 3 Sunrise Amber theme, Outfit fonts, spring animations, scatter-letter splash
