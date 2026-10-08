#!/usr/bin/env python3
"""Meluko flashcard drill.

Run with:
    python Ref/interview/flashcard_quiz.py

Rules:
- Read the question out loud.
- Say your answer.
- Press Enter to reveal.
- Grade yourself: y = correct, n = incorrect, s = skip.
- Missed cards go into a review pile until you get them right.
"""

import random
import sys

FLASHCARDS = [
    ("What is Meluko?", "A social alarm clock. You join a tribe, everyone wakes to the same alarm, and if you snooze, the whole tribe sees it."),
    ("Why does social accountability beat a louder alarm?", "Willpower resets every morning; embarrassment doesn't. Failing publicly is a stronger motivator than a ringtone."),
    ("What are the four components of the accountability loop?", "Shared tribe alarm, proposal/veto to change it, public wake status, streak and leaderboard."),
    ("What does the tribe alarm target vs the followed alarm mean?", "The tribe has one shared alarmTime; each user's followedAlarmTime is what their device actually schedules."),
    ("Why can't WorkManager replace AlarmManager here?", "WorkManager is for deferrable work and does not guarantee exact fire times. A wake-up alarm must fire at the exact minute."),
    ("What is the app's architecture at the top level?", "Single Activity (MainActivity) with a Compose NavHost; every screen is a composable in the graph."),
    ("What is the entry annotation and why?", "@AndroidEntryPoint on MainActivity — it tells Hilt to generate injection code so hiltViewModel() works."),
    ("List the onboarding-flow routes in order.", "splash, login, onboarding, create_profile, join_tribe, set_first_alarm, home."),
    ("What is the returning-user route?", "splash, welcome_back, home."),
    ("How does auth state drive navigation?", "AuthState (Idle/Loading/Success/Error) is collected at the graph level; a LaunchedEffect routes on change with popUpTo(0) { inclusive = true }."),
    ("Why is popUpTo(0) { inclusive = true } important after login?", "It clears the back stack so the user can't navigate back into a protected screen."),
    ("What modern API is used for Google Sign-In?", "Android Credential Manager (GetGoogleIdOption → GetCredentialRequest → GoogleIdTokenCredential)."),
    ("What is the exchange step?", "GoogleAuthProvider.getCredential(googleIdToken, null) then firebaseAuth.signInWithCredential(...).await()."),
    ("What determines isNewUser?", "Firestore data: a user is returning if they have a users doc AND a primaryTribeId in user_alarms."),
    ("Name the four resilience techniques in checkExistingSession.", "Parallel reads (coroutineScope + async), 2 retries with 500ms/1000ms backoff, a 10s withTimeoutOrNull, and a fallback that trusts Firebase Auth assuming returning user."),
    ("What happens if Firestore is unreachable during session restore?", "The app trusts the Firebase Auth session, assumes returning user, and lets the user in (worst case: onboarding once more)."),
    ("What timeout guards the credential exchange?", "withTimeoutOrNull(15_000) — on failure the user sees 'Sign-in timed out — check your connection'."),
    ("List the top-level collections and the subcollections.", "users, user_alarms, tribes; subcollections tribes/{id}/members and tribes/{id}/proposals."),
    ("What are the member statuses?", "\"awake\" / \"missed\" / \"sleeping\" — mapped to a sealed WakeStatus."),
    ("What is the rule for users/{uid}?", "Read/write only by request.auth.uid == uid — strictly self-only."),
    ("Why is tribe read open to any authenticated user?", "The join-by-invite-code flow needs to read the tribe doc before the user has a member doc."),
    ("What does 'allow delete: if false' do?", "Explicitly forbids deletion — proposals remain as an audit trail; a tribe can't be deleted."),
    ("What write operations are allowed on members?", "Create and update are self-only; read requires an existing membership; delete is forbidden."),
    ("What are the three states of SocialUiState?", "Loading, Success(members, tribes), Error(message)."),
    ("Walk the three-listener chain on the Social screen.", "Listen on user_alarms/{uid} for primaryTribeId → listen on tribes/{id} for tribe info → listen on tribes/{id}/members for member list."),
    ("How is the leaderboard ordered?", "sortedByDescending { it.streakDays } — highest streak first."),
    ("How is the awake count computed?", "list.count { it.status == WakeStatus.Awake }, stamped onto the tribe card."),
    ("Where are listeners removed?", "In onCleared() and before re-registering with a new tribe ID — preventing leaks and stacked listeners."),
    ("Why does the chain listen instead of read once?", "When primaryTribeId changes, the listeners re-register automatically; a one-shot read would freeze on the stale tribe."),
    ("Which AlarmManager method is used and why?", "setExactAndAllowWhileIdle — fires at the exact time even during Doze mode."),
    ("What is the fallback if exact-alarm permission is missing?", "alarmManager.set(...) — inexact, still fires, possibly a few minutes off."),
    ("Which two permissions matter and on which API levels?", "SCHEDULE_EXACT_ALARM (API 31+), USE_EXACT_ALARM (API 33+)."),
    ("What does nextOccurrenceMillis do?", "Computes today's target; if it has already passed, adds one day so the alarm fires tomorrow."),
    ("What are the two jobs of AlarmReceiver at fire time?", "(1) Reschedule the next day's alarm immediately; (2) post a full-screen notification that launches AlarmActivity."),
    ("Why reschedule in the receiver instead of on dismiss?", "The receiver is launched by the system regardless of app state — force-kills, reboots, and missed swipes can't skip it."),
    ("What makes the notification alarm-like?", "IMPORTANCE_HIGH, setBypassDnd(true), VISIBILITY_PUBLIC, CATEGORY_ALARM, setFullScreenIntent(..., true), setOngoing(true)."),
    ("What does AlarmActivity do over the lock screen?", "setShowWhenLocked + setTurnScreenOn + FLAG_KEEP_SCREEN_ON, loops the ringtone, repeats a vibration waveform [0, 800, 600], then on swipe updates streaks in Firestore."),
    ("What is the streak rule?", "If lastWakeDate == yesterday, streak + 1; else streak = 1. bestStreak = max(streak, bestStreak); totalWakes + 1."),
    ("What is AlarmCache and what does it store?", "A SharedPreferences layer storing alarmTime, tribeId, alarmToneUri — the fallback source for the system layer."),
    ("Walk BootReceiver's fallback chain.", "No signed-in user → cache. Signed-in → try Firestore; on success use the alarm time (and refresh cache); on no-doc or failure → cache. No cache → log and skip."),
    ("What is goAsync() protecting?", "A BroadcastReceiver is killed ~10s after onReceive returns; goAsync() lets the Firestore read finish before the process is cut off."),
    ("Why can the boot path never throw?", "parts.getOrElse(0) { \"7\" }.toIntOrNull() ?: 7 — malformed data degrades to 07:30 instead of crashing the boot path."),
    ("How does ConnectivityObserver work?", "callbackFlow wrapping ConnectivityManager.NetworkCallback; emits true/false, re-checks NET_CAPABILITY_INTERNET on capabilities change, emits initial state, and uses .conflate()."),
    ("What is the HomeViewModel load strategy?", "Cache-first for instant display, then the live listener replaces it with Firestore data and refreshes the cache on every update."),
    ("What does the offline banner tell the user and why is it true?", "'You're offline. Alarm still set' — true because of AlarmCache + BootReceiver rescheduling."),
    ("What are the proposal rules?", "Anyone can propose a new time; there's a 2-hour veto window; one veto kills it; otherwise it auto-accepts."),
    ("Where is expiresAt set?", "createdAt + 2 * 60 * 60 * 1000L (2 hours) at creation time, with createdAt as a server timestamp."),
    ("How does auto-accept trigger?", "The proposal listener sees a pending proposal whose expiresAt has passed and commits an atomic batch."),
    ("What does isAccepting prevent?", "Duplicate acceptProposal calls when the listener fires repeatedly across the expiry deadline."),
    ("What are the three writes in the accept batch?", "Proposal status → accepted, tribe alarmTime → proposed time, this user's followedAlarmTime → proposed time."),
    ("Why a Firestore batch?", "Atomicity — all three docs change together or not at all, and it's one round-trip."),
    ("How does castVeto stay race-safe?", "FieldValue.arrayUnion(uid) for the vetoer list and FieldValue.increment(1) for the count — both atomic server-side."),
    ("Why is createdAt a server timestamp?", "The server clock is authoritative; expiry math isn't corrupted by a device with the wrong time."),
    ("Why is the time stored in 24-hour format?", "No AM/PM ambiguity in the data layer; display converts to 12h at the edge."),
    ("What is the design system?", "Sunrise Amber #FF8C00 seed, Outfit font (9 weights), near-black #0F0F12, 0dp-elevation cards with 20–24dp rounding, 56dp buttons, uppercase letter-spaced headers."),
    ("Why springs instead of tweens?", "Springs are physical — they overshoot and settle naturally, reading as alive; named specs in MotionSpec keep the feel consistent."),
    ("What does pressScale() do?", "An Animatable-driven graphicsLayer scale modifier -- scales to 0.94f on press (0.92f for chips, 0.90f for FABs) and springs back on release, applied to interactive elements app-wide."),
    ("Why are screens stateless?", "State lives in ViewModels as StateFlow; composables only collect and render — the screens survive rotation and have one render path."),
    ("What are the Loading/Success/Error renderings?", "Shimmer skeletons for Loading, real content for Success, error banner with retry for Error."),
    ("What does the release build do differently?", "isMinifyEnabled = true (R8) + isShrinkResources = true → ~4.3 MB APK, signed with the release keystore, Crashlytics mapping uploaded."),
    ("What are R8's three jobs?", "Shrinking (remove unused code), obfuscation (rename classes), optimization (inline/dead-code elimination)."),
    ("Why are keep rules needed?", "Firebase, Hilt, and Coil use reflection — obfuscation would break their runtime lookups."),
    ("What do SourceFile/LineNumberTable keep attributes do?", "Keep readable crash traces — stack traces point at the exact line instead of obfuscated names."),
    ("Why does release Google Sign-In need a separate SHA?", "Debug and release keystores produce different fingerprints; Firebase only trusts registered ones. Release SHA-1/SHA-256 are registered in the Console."),
    ("Why can't you lose the release keystore?", "It's the cryptographic identity of the app — lose it and you can never publish an update under the same package name."),
    ("Where are release passwords sourced?", "Env vars KEYSTORE_PASSWORD / KEY_PASSWORD with meluko-secure-2026 as a local fallback."),
    ("Why reschedule-on-fire instead of on-dismiss?", "Dismiss requires a live user; fire happens in a system-launched receiver — the only point that can't be skipped."),
    ("Why client-side auto-accept instead of a Cloud Function?", "A listener committing an idempotent batch is free and near-real-time; the status update makes it race-safe at this scale."),
    ("Why followedAlarmTime AND alarmTime — isn't that redundant?", "Deliberate denormalization: the followed time is what this device schedules, survives mid-transition, and the redundancy is the resilience."),
    ("Why SharedPreferences instead of Room for the cache?", "Three tiny values that must be readable synchronously from a broadcast receiver with zero setup — Room would be over-engineering."),
    ("What is the honest anti-cheat answer for streaks?", "Security rules gate writes and todayStatus == \"awake\" guards double-counting, but the real anti-cheat is the social pressure."),
    ("What happens if Firestore is down on app open?", "Auth state comes first; the check falls back to trusting the session; HomeViewModel already rendered the cached time and shows the offline banner."),
    ("Say the one-line pitch.", "Meluko is a social alarm clock — you join a tribe, everyone wakes to the same alarm, and if you snooze, the whole tribe sees it."),
]


def main():
    print("=" * 60)
    print("Meluko Interview Flashcards")
    print("=" * 60)
    print("Read each question, say the answer, press Enter to reveal.")
    print("Grade yourself: y = correct, n = incorrect, s = skip, q = quit\n")

    deck = list(FLASHCARDS)
    random.shuffle(deck)
    correct = 0
    incorrect = 0
    missed = []

    while deck:
        question, answer = deck.pop(0)
        print(f"\nQ: {question}")
        try:
            input("[Press Enter to reveal...]")
        except (EOFError, KeyboardInterrupt):
            print("\nQuitting.")
            sys.exit(0)

        print(f"A: {answer}")
        while True:
            grade = input("Did you get it? (y/n/s/q): ").strip().lower()
            if grade in ("y", "n", "s", "q"):
                break
            print("Please enter y, n, s, or q.")

        if grade == "q":
            break
        elif grade == "y":
            correct += 1
        elif grade == "n":
            incorrect += 1
            missed.append((question, answer))
        elif grade == "s":
            missed.append((question, answer))

    # Review missed cards
    if missed:
        print(f"\n\nReview round: {len(missed)} card(s) to retry.")
        random.shuffle(missed)
        deck = missed
        while deck:
            question, answer = deck.pop(0)
            print(f"\nQ: {question}")
            try:
                input("[Press Enter to reveal...]")
            except (EOFError, KeyboardInterrupt):
                print("\nQuitting.")
                sys.exit(0)
            print(f"A: {answer}")
            while True:
                grade = input("Correct now? (y/n/q): ").strip().lower()
                if grade in ("y", "n", "q"):
                    break
                print("Please enter y, n, or q.")
            if grade == "q":
                break
            if grade == "n":
                deck.append((question, answer))
                random.shuffle(deck)

    total = correct + incorrect
    if total > 0:
        pct = correct / total * 100
        print(f"\n\nSession complete. Correct: {correct}, Incorrect: {incorrect}, Accuracy: {pct:.1f}%")
    else:
        print("\n\nNo cards graded.")


if __name__ == "__main__":
    main()
