# Lesson 13 — Flashcards

Read the question, cover the answer, say the answer out loud. Repeat until automatic.

> **Interactive version:** Run `python Ref/interview/flashcard_quiz.py` for a terminal drill that keeps missed cards in a review pile.

## Mental model

**Q: What is Meluko?**
A: A social alarm clock. You join a tribe, everyone wakes to the same alarm, and if you snooze, the whole tribe sees it.

**Q: Why does social accountability beat a louder alarm?**
A: Willpower resets every morning; embarrassment doesn't. Failing publicly is a stronger motivator than a ringtone.

**Q: What are the four components of the accountability loop?**
A: Shared tribe alarm → proposal/veto to change it → public wake status → streak and leaderboard.

**Q: What does the tribe alarm target vs the followed alarm mean?**
A: The tribe has one shared `alarmTime`; each user's `followedAlarmTime` is what their device actually schedules.

**Q: Why can't WorkManager replace AlarmManager here?**
A: WorkManager is for deferrable work and does not guarantee exact fire times. A wake-up alarm must fire at the exact minute.

## Config and entry

**Q: What is the app's architecture at the top level?**
A: Single Activity (`MainActivity`) with a Compose `NavHost`; every screen is a composable in the graph.

**Q: What is the entry annotation and why?**
A: `@AndroidEntryPoint` on MainActivity — it tells Hilt to generate injection code so `hiltViewModel()` works.

**Q: List the onboarding-flow routes in order.**
A: splash → login → onboarding → create_profile → join_tribe → set_first_alarm → home.

**Q: What is the returning-user route?**
A: splash → welcome_back → home.

**Q: How does auth state drive navigation?**
A: `AuthState` (Idle/Loading/Success/Error) is collected at the graph level; a LaunchedEffect routes on change with `popUpTo(0) { inclusive = true }`.

**Q: Why is `popUpTo(0) { inclusive = true }` important after login?**
A: It clears the back stack so the user can't navigate back into a protected screen.

## Auth

**Q: What modern API is used for Google Sign-In?**
A: Android Credential Manager (GetGoogleIdOption → GetCredentialRequest → GoogleIdTokenCredential).

**Q: What is the exchange step?**
A: `GoogleAuthProvider.getCredential(googleIdToken, null)` then `firebaseAuth.signInWithCredential(...).await()`.

**Q: What determines `isNewUser`?**
A: Firestore data: a user is returning if they have a `users` doc AND a `primaryTribeId` in `user_alarms`.

**Q: Name the four resilience techniques in `checkExistingSession`.**
A: Parallel reads (`coroutineScope` + `async`), 2 retries with 500ms/1000ms backoff, a 10s `withTimeoutOrNull`, and a fallback that trusts Firebase Auth assuming returning user.

**Q: What happens if Firestore is unreachable during session restore?**
A: The app trusts the Firebase Auth session, assumes returning user, and lets the user in (worst case: onboarding once more).

**Q: What timeout guards the credential exchange?**
A: `withTimeoutOrNull(15_000)` — on failure the user sees "Sign-in timed out — check your connection".

## Firestore data model

**Q: List the top-level collections and the subcollections.**
A: `users`, `user_alarms`, `tribes`; subcollections `tribes/{id}/members` and `tribes/{id}/proposals`.

**Q: What are the member statuses?**
A: `"awake"` / `"missed"` / `"sleeping"` — mapped to a sealed `WakeStatus`.

**Q: What is the rule for `users/{uid}`?**
A: Read/write only by `request.auth.uid == uid` — strictly self-only.

**Q: Why is tribe read open to any authenticated user?**
A: The join-by-invite-code flow needs to read the tribe doc before the user has a member doc.

**Q: What does `allow delete: if false` do?**
A: Explicitly forbids deletion — proposals remain as an audit trail; a tribe can't be deleted.

**Q: What write operations are allowed on members?**
A: Create and update are self-only; read requires an existing membership; delete is forbidden.

## Tribes and members

**Q: What are the three states of `SocialUiState`?**
A: Loading, Success(members, tribes), Error(message).

**Q: Walk the three-listener chain on the Social screen.**
A: Listen on `user_alarms/{uid}` for `primaryTribeId` → listen on `tribes/{id}` for tribe info → listen on `tribes/{id}/members` for member list.

**Q: How is the leaderboard ordered?**
A: `sortedByDescending { it.streakDays }` — highest streak first.

**Q: How is the awake count computed?**
A: `list.count { it.status == WakeStatus.Awake }`, stamped onto the tribe card.

**Q: Where are listeners removed?**
A: In `onCleared()` and before re-registering with a new tribe ID — preventing leaks and stacked listeners.

**Q: Why does the chain listen instead of read once?**
A: When `primaryTribeId` changes, the listeners re-register automatically; a one-shot read would freeze on the stale tribe.

## Alarm scheduling

**Q: Which AlarmManager method is used and why?**
A: `setExactAndAllowWhileIdle` — fires at the exact time even during Doze mode.

**Q: What is the fallback if exact-alarm permission is missing?**
A: `alarmManager.set(...)` — inexact, still fires, possibly a few minutes off.

**Q: Which two permissions matter and on which API levels?**
A: `SCHEDULE_EXACT_ALARM` (API 31+), `USE_EXACT_ALARM` (API 33+).

**Q: What does `nextOccurrenceMillis` do?**
A: Computes today's target; if it has already passed, adds one day so the alarm fires tomorrow.

**Q: What are the two jobs of AlarmReceiver at fire time?**
A: (1) Reschedule the next day's alarm immediately; (2) post a full-screen notification that launches AlarmActivity.

**Q: Why reschedule in the receiver instead of on dismiss?**
A: The receiver is launched by the system regardless of app state — force-kills, reboots, and missed swipes can't skip it.

**Q: What makes the notification alarm-like?**
A: IMPORTANCE_HIGH, `setBypassDnd(true)`, `VISIBILITY_PUBLIC`, `CATEGORY_ALARM`, `setFullScreenIntent(..., true)`, `setOngoing(true)`.

**Q: What does AlarmActivity do over the lock screen?**
A: `setShowWhenLocked` + `setTurnScreenOn` + `FLAG_KEEP_SCREEN_ON`, loops the ringtone, repeats a vibration waveform `[0, 800, 600]`, then on swipe updates streaks in Firestore.

**Q: What is the streak rule?**
A: `if (lastWakeDate == yesterday) streak + 1 else streak = 1`; bestStreak = max(streak, bestStreak); totalWakes + 1.

## Reliability and offline

**Q: What is `AlarmCache` and what does it store?**
A: A SharedPreferences layer storing `alarmTime`, `tribeId`, `alarmToneUri` — the fallback source for the system layer.

**Q: Walk BootReceiver's fallback chain.**
A: No signed-in user → cache. Signed-in → try Firestore; on success use the alarm time (and refresh cache); on no-doc or failure → cache. No cache → log and skip.

**Q: What is `goAsync()` protecting?**
A: A BroadcastReceiver is killed ~10s after onReceive returns; `goAsync()` lets the Firestore read finish before the process is cut off.

**Q: Why can the boot path never throw?**
A: `parts.getOrElse(0) { "7" }.toIntOrNull() ?: 7` — malformed data degrades to 07:30 instead of crashing the boot path.

**Q: How does ConnectivityObserver work?**
A: `callbackFlow` wrapping `ConnectivityManager.NetworkCallback`; emits `true/false`, re-checks `NET_CAPABILITY_INTERNET` on capabilities change, emits initial state, and uses `.conflate()`.

**Q: What is the HomeViewModel load strategy?**
A: Cache-first for instant display, then the live listener replaces it with Firestore data and refreshes the cache on every update.

**Q: What does the offline banner tell the user and why is it true?**
A: "You're offline. Alarm still set" — true because of AlarmCache + BootReceiver rescheduling.

## Proposals and veto

**Q: What are the proposal rules?**
A: Anyone can propose a new time; there's a 2-hour veto window; one veto kills it; otherwise it auto-accepts.

**Q: Where is `expiresAt` set?**
A: `createdAt + 2 * 60 * 60 * 1000L` (2 hours) at creation time, with `createdAt` as a server timestamp.

**Q: How does auto-accept trigger?**
A: The proposal listener sees a pending proposal whose `expiresAt` has passed and commits an atomic batch.

**Q: What does `isAccepting` prevent?**
A: Duplicate `acceptProposal` calls when the listener fires repeatedly across the expiry deadline.

**Q: What are the three writes in the accept batch?**
A: Proposal status → "accepted", tribe `alarmTime` → proposed time, this user's `followedAlarmTime` → proposed time.

**Q: Why a Firestore batch?**
A: Atomicity — all three docs change together or not at all, and it's one round-trip.

**Q: How does castVeto stay race-safe?**
A: `FieldValue.arrayUnion(uid)` for the vetoer list and `FieldValue.increment(1)` for the count — both atomic server-side.

**Q: Why is `createdAt` a server timestamp?**
A: The server clock is authoritative; expiry math isn't corrupted by a device with the wrong time.

**Q: Why is the time stored in 24-hour format?**
A: No AM/PM ambiguity in the data layer; display converts to 12h at the edge.

## UI and animations

**Q: What is the design system?**
A: Sunrise Amber `#FF8C00` seed, Outfit font (9 weights), near-black `#0F0F12`, 0dp-elevation cards with 20–24dp rounding, 56dp buttons, uppercase letter-spaced headers.

**Q: Why springs instead of tweens?**
A: Springs are physical — they overshoot and settle naturally, reading as alive; named specs in `MotionSpec` keep the feel consistent.

**Q: What does `pressScale()` do?**
A: An Animatable-driven graphicsLayer scale modifier — scales to 0.94f on press (0.92f for chips, 0.90f for FABs) and springs back on release, applied to interactive elements app-wide.

**Q: Why are screens stateless?**
A: State lives in ViewModels as StateFlow; composables only collect and render — the screens survive rotation and have one render path.

**Q: What are the Loading/Success/Error renderings?**
A: Shimmer skeletons for Loading, real content for Success, error banner with retry for Error.

## Build and release

**Q: What does the release build do differently?**
A: `isMinifyEnabled = true` (R8) + `isShrinkResources = true` → ~4.3 MB APK, signed with the release keystore, Crashlytics mapping uploaded.

**Q: What are R8's three jobs?**
A: Shrinking (remove unused code), obfuscation (rename classes), optimization (inline/dead-code elimination).

**Q: Why are keep rules needed?**
A: Firebase, Hilt, and Coil use reflection — obfuscation would break their runtime lookups.

**Q: What do SourceFile/LineNumberTable keep attributes do?**
A: Keep readable crash traces — stack traces point at the exact line instead of obfuscated names.

**Q: Why does release Google Sign-In need a separate SHA?**
A: Debug and release keystores produce different fingerprints; Firebase only trusts registered ones. Release SHA-1/SHA-256 are registered in the Console.

**Q: Why can't you lose the release keystore?**
A: It's the cryptographic identity of the app — lose it and you can never publish an update under the same package name.

**Q: Where are release passwords sourced?**
A: Env vars `KEYSTORE_PASSWORD` / `KEY_PASSWORD` with `meluko-secure-2026` as a local fallback.

## Judgment layer

**Q: Why reschedule-on-fire instead of on-dismiss?**
A: Dismiss requires a live user; fire happens in a system-launched receiver — the only point that can't be skipped.

**Q: Why client-side auto-accept instead of a Cloud Function?**
A: A listener committing an idempotent batch is free and near-real-time; the status update makes it race-safe at this scale.

**Q: Why `followedAlarmTime` AND `alarmTime` — isn't that redundant?**
A: Deliberate denormalization: the followed time is what this device schedules, survives mid-transition, and the redundancy is the resilience.

**Q: Why SharedPreferences instead of Room for the cache?**
A: Three tiny values that must be readable synchronously from a broadcast receiver with zero setup — Room would be over-engineering.

**Q: What is the honest anti-cheat answer for streaks?**
A: Security rules gate writes and `todayStatus == "awake"` guards double-counting, but the real anti-cheat is the social pressure.

**Q: What happens if Firestore is down on app open?**
A: Auth state comes first; the check falls back to trusting the session; HomeViewModel already rendered the cached time and shows the offline banner.

**Q: Say the one-line pitch.**
A: "Meluko is a social alarm clock — you join a tribe, everyone wakes to the same alarm, and if you snooze, the whole tribe sees it."
