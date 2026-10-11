# Meluko — Changelog

> Human-readable summary of major repository changes. Not every commit — only meaningful milestones.

---

## 2026-10-11 — Alarm Screen + Tone Overhaul

**Fixed:**
- Back button no longer dismisses the ringing alarm (swipe is the only way out).
- Alarm audio now uses `USAGE_ALARM` via `MediaPlayer` (`AlarmTonePlayer`), so it rings on alarm volume even on silent.
- Alarm screen showed the raw 24h alarm string with a guessed AM/PM ("19 : 30 AM"); now a live clock.
- Custom tone no longer fetched from Firestore at ring time (network-dependent, audible switch); read from local `AlarmCache`.
- **HomeViewModel stats always 0 for returning users**: an early return when cached alarm time == Firestore time skipped attaching the tribe/member listeners on every cold start.

**Added:**
- Alarm screen redesign: rising sunrise backdrop, breathing glow, time-aware greeting, date, tribe strip ("N of M already up"), 30s volume crescendo, sunburst wake-up celebration with streak odometer. Respects "remove animations".
- Swipe-to-wake refinement ("Sunrise track"): horizon fill, sun thumb with rotating rays that lengthen with progress, slight lift, idle nudge hint, "Slide to rise" + chevron wave. Gesture logic unchanged.
- In-app `AlarmToneSheet` (system alarm tones only), tap-to-preview on the alarm stream with equalizer indicator; tone + title stored on-device; Account row shows tone name. One-time migration from the old Firestore field.

**Known gap:** wakes while offline are not recorded (Firestore transactions need network), so `MissedAlarmReceiver` can mark an offline riser as missed.

---

## 2026-10-08 — Beta-Readiness Pass

**Added:**
- Account deletion flow (`AccountViewModel.deleteAccount`) — best-effort tribe
  leave + Firestore doc cleanup + Storage photo delete, then deletes the
  Firebase Auth user, re-prompting Google Sign-In if the session is stale
  (`FirebaseAuthRecentLoginRequiredException`). Required by Google Play policy.
- Battery-optimization whitelist banner on HomeScreen (dismissible,
  `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) — many OEMs kill background exact
  alarms otherwise.
- Leave tribe, admin remove-member, and admin delete-tribe (only once the
  admin is the last member) in `SocialViewModel`, with Firestore-rule-safe
  write ordering inside each transaction.
- `isTribeAdmin()` Firestore rule helper; members can now delete their own
  membership or be deleted by an admin; tribes can be deleted by their admin
  once `memberCount <= 1`.
- `onMemberRemoved` Cloud Function trigger (clears a removed/left user's
  `primaryTribeId` server-side) — **written but not deployed** (see below).
- Client-side self-heal in `SocialViewModel`: if the members listener gets
  `PERMISSION_DENIED` or the tribe doc no longer exists, the app clears its
  own stale `primaryTribeId` instead of getting stuck in an error loop.
  Stands in for `onMemberRemoved` until Cloud Functions are deployed.
- Invite-code share sheet (`Intent.ACTION_SEND`) and visible invite code on
  TribeDetailScreen — previously generated at tribe creation but never shown
  to the user again.
- `AuthConstants.WEB_CLIENT_ID` — deduplicated from `AuthViewModel` so
  `AccountViewModel` can reuse it for re-auth.
- `celebrateBurst()` / `rememberIncreaseTrigger()` in `ExpressiveUtils` — a
  reusable springy pop animation, wired to the streak stat on Home/Account.
- Icon badges (flame / sun / trophy) replacing the blank emoji-removal
  placeholders on Home/Account stat cards and the Consistency card.
- Warmer empty-state copy in SocialScreen.

**Deployed:**
- Firestore rules — live on `meluko-routine`.

**Not deployed (needs a decision):**
- Cloud Functions require upgrading `meluko-routine` from Spark to Blaze
  (billing). User chose to skip this for now — functions are written and
  `npm install`'d in `functions/`, ready to deploy once Blaze is enabled.

**Housekeeping:**
- Committed ~6 weeks of uncommitted work from the 2026-08-27 audit session
  (see below) that had never been committed — verified it still builds and
  passes tests first.

---

## 2026-08-27 — Production Audit + Fixes

**Added:**
- Exact-alarm permission banner on HomeScreen with settings intent (Android 12+)
- `MissedAlarmReceiver` — marks user as missed after a 10-minute grace period
- `AlarmReceiver` now resets own `todayStatus` to sleeping at fire time (daily cycle)
- `BootReceiver` waits for FirebaseAuth restore with a 5s timeout before cache fallback
- Firestore transaction in `AlarmActivity.handleDismiss()` to prevent double-count
- `StreakCalculator` pure helper + unit tests
- `TimeUtils.to24HourTime()` shared helper + unit tests
- Firebase Cloud Functions under `functions/`:
  - `onProposalCreated` / `onProposalUpdated` — server-side auto-accept, updates all members
  - `joinTribeByInviteCode` — server-side invite validation and member creation
- Hilt-injected `FirebaseStorage` in `FirebaseModule`
- Firestore constants for statuses (`STATUS_AWAKE`, `STATUS_SLEEPING`, etc.) and roles
- Rank recomputation in `SocialViewModel`

**Changed:**
- `SetFirstAlarmScreen` now uses shared `to24HourTime()` — first alarm fires correctly
- `AuthViewModel` writes user doc before reading to avoid new-user misclassification
- `OnboardingViewModel.createTribe()` uses a Firestore batch
- `OnboardingViewModel.joinTribe()` uses a transaction + `FieldValue.increment(1)`
- All ViewModels now inject Firebase via Hilt (no more `getInstance()` calls)
- Firestore rules tightened: proposal transitions, proposer matching, member create guard
- Theme parent changed from Light to dark Material theme
- Removed emojis from all UI to comply with `AGENTS.md`
- Release signing uses env vars with new keystore; `google-services.json` no longer tracked

**Fixed:**
- `SetFirstAlarmScreen` stored 12h time, breaking alarm scheduler
- `memberCount` increment race when two users join simultaneously
- `TribeDetailScreen` clobbering Social tab member listener
- `AlarmActivity` double-dismiss race
- `BootReceiver` unnecessary cache fallback when auth was still restoring
- Lint warnings: redundant activity label, `NoCredentialException`

**Security:**
- Removed tracked `app/google-services.json` and generated new `app/release-key.jks`
- New release SHA-1/SHA-256 fingerprints

**Documentation:**
- Full `Ref/` interview-prep package mirroring ContextIQ format
- TCS Prime quick-sheet

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
