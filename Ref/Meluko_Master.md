# Meluko — Master Document

---

## What It Is

A native Android social alarm clock. You join a "tribe" — your hostel flatmates, gym crew, whoever — and everyone wakes to a shared alarm. Miss it, and your whole tribe sees it. No snooze. No lying.

**The core thesis:** Willpower is unreliable, but social accountability scales. Meluko turns "wake up on time" from an individual battle into a group contract — you can't let the tribe down. The tribe alarm is a shared system of record: proposals, vetoes, streaks, and live wake statuses all flow through Firestore in real time.

**Portfolio role:** Android credibility card — proves native Kotlin/Compose skill, Firebase architecture, system-level Android (alarm manager, boot receivers, exact alarms, notifications), and production thinking (release signing, ProGuard, Crashlytics, offline caching).

**Origin story:** Started as a concept for hostel life — the biggest accountability lever for students is their friend group. Instead of a personal alarm you can ignore, Meluko makes the alarm a social event. Built solo, shipped a signed v1.0 release APK, available on GitHub Releases.

---

## What Makes It Different From Every Other Alarm App

1. **The social contract** — the alarm isn't yours to snooze. Your status is visible to everyone.
2. **Proposal + veto democracy** — anyone can propose a new time; one veto kills it within 2 hours; otherwise it auto-accepts.
3. **Streak mechanics** — consecutive-day wakes build streaks, ranked in a leaderboard.
4. **System-level reliability** — exact alarms, boot rescheduling, full-screen notification over the lock screen, offline cache fallback.
5. **Production discipline** — signed release build, R8/ProGuard, Crashlytics, SHA registration, offline-first cache.

---

## Architecture — The Big Picture

```
┌─────────────────────────── ANDROID APP (single Activity) ───────────────────────────┐
│                                                                                     │
│  MainActivity ──► AppNavGraph (Compose Navigation)                                  │
│                    splash → login → onboarding → create_profile → join_tribe        │
│                    → set_first_alarm → home  (tribe_detail deep-dive)               │
│                                                                                     │
│  MainScreen (3-tab shell)                                                           │
│   ├── SocialScreen   → SocialViewModel   → tribe members, wake statuses             │
│   ├── HomeScreen     → HomeViewModel     → next alarm, hero card, countdown          │
│   └── AccountScreen  → AccountViewModel  → profile, streaks, alarm tone             │
│                                                                                     │
│  ProposeAlarmBottomSheet → ProposalViewModel → create proposal                       │
│  VetoBottomSheet        → ProposalViewModel → cast veto                              │
│  TribeDetailScreen      → tribe detail, leaderboard, proposal card                   │
│                                                                                     │
│  AlarmScheduler ──► AlarmManager ──► AlarmReceiver ──► AlarmActivity                │
│        ▲                                       │  │                                 │
│        │                                       │  └─► MissedAlarmReceiver           │
│        │                                       │      (+10min grace, marks missed,   │
│        │                                       │       resets streak if still asleep)│
│        └──────── BootReceiver (on reboot) ◄────┘   └─► Firestore writes (streaks,    │
│                                                          via transaction)            │
│                                                                                     │
│  AlarmCache (SharedPreferences) ── offline fallback for alarm time                   │
│  ConnectivityObserver (NetworkCallback) ── online/offline Flow                       │
└─────────────────────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
                    ┌─────────────────────────────┐
                    │          FIREBASE           │
                    │  Auth: Google Sign-In       │
                    │  Firestore: rules-protected │
                    │  Storage: profile photos    │
                    │  Crashlytics: crash reports │
                    └─────────────────────────────┘
```

---

## Stack — Every Layer

| Layer | Technology | Why |
|---|---|---|
| Language | Kotlin 2.2.10 | Modern, safe, official Android |
| UI | Jetpack Compose (Material 3), BOM 2026.05.01 | Declarative UI, one codebase for all screens |
| DI | Dagger Hilt 2.59.2 + KSP | Scoped ViewModels, injectable Firebase |
| Navigation | Compose Navigation 2.9.0 | Single-activity routing, animated transitions |
| Auth | Credential Manager 1.5.0 + Google ID | Modern Google Sign-In, no deprecated SDK |
| Database | Firebase Firestore (BOM 33.15.0) | Real-time listeners, server rules |
| Auth backend | Firebase Auth | Battle-tested identity, session persistence |
| Storage | Firebase Storage | Profile photos |
| Crash reporting | Firebase Crashlytics | Automatic stack traces + mapping upload |
| Images | Coil 2.7.0 | Async image loading |
| Local cache | SharedPreferences (`AlarmCache`) | Offline alarm reliability |
| Connectivity | `ConnectivityManager.NetworkCallback` | Reactive online/offline Flow |
| Min SDK | 24 (Android 7.0) | Broad device coverage |
| Target SDK | 36 | Latest platform behavior |

---

## Project Structure

```
Meluko/
├── app/src/main/java/com/alarm/meluko/
│   ├── MainActivity.kt              # @AndroidEntryPoint, single Activity
│   ├── MelukoApplication.kt         # @HiltAndroidApp
│   ├── alarm/
│   │   ├── AlarmScheduler.kt        # AlarmManager wrapper, exact alarms
│   │   ├── AlarmReceiver.kt         # BroadcastReceiver → notification + reschedule
│   │   ├── AlarmActivity.kt         # Full-screen alarm UI, tone, vibration, streak write (transaction)
│   │   ├── StreakCalculator.kt      # Pure streak math (unit-testable, no Android deps)
│   │   ├── MissedAlarmReceiver.kt   # +10min grace check → marks missed, resets streak to 0
│   │   └── BootReceiver.kt          # Reschedules after device reboot (auth-restore aware)
│   ├── auth/
│   │   └── AuthViewModel.kt         # Google Sign-In + session check + resilience
│   ├── data/
│   │   ├── FirestoreConstants.kt    # Collection + field name constants
│   │   └── AlarmCache.kt            # SharedPreferences cache
│   ├── di/
│   │   └── FirebaseModule.kt        # Hilt providers for Firebase singletons
│   ├── navigation/
│   │   ├── AppNavGraph.kt           # Route constants + NavHost + auth-driven routing
│   │   ├── MelukoBottomNavigation.kt
│   │   └── BottomNavItem.kt
│   ├── ui/                          # 20+ screens/composables/ViewModels
│   │   ├── theme/                   # Color (Sunrise Amber), Type (Outfit), Theme
│   │   ├── MotionSpec.kt            # Named spring specs
│   │   ├── ShimmerEffect.kt         # Loading shimmer
│   │   ├── ExpressiveUtils.kt       # pressScale() animations
│   │   ├── HomeScreen.kt            # Hero card, countdown, offline banner
│   │   ├── HomeViewModel.kt         # Alarm listeners + cache + connectivity
│   │   ├── SocialScreen.kt          # Members, wake statuses, leaderboard
│   │   ├── SocialViewModel.kt
│   │   ├── ProposalViewModel.kt     # Proposals, vetoes, auto-accept
│   │   ├── ProposeAlarmBottomSheet.kt  # WheelPicker time selection
│   │   ├── VetoBottomSheet.kt       # Veto confirmation sheet
│   │   └── ... (Account, Onboarding, Login, Splash, CreateProfile, etc.)
│   └── util/
│       └── ConnectivityObserver.kt  # Reactive network status
├── app/build.gradle.kts             # Signing, R8, Crashlytics config
├── app/proguard-rules.pro           # Keep rules for Firebase/Hilt/Coil
├── app/google-services.json         # Firebase config (debug SHA only)
├── app/release-key.jks              # Release signing keystore (gitignored)
├── firestore.rules                  # Security rules (deployed)
└── README.md
```

---

## Data Model

### Collections

| Collection | Document ID | Purpose |
|---|---|---|
| `users/{uid}` | Firebase Auth UID | Profile: displayName, username, bio, email, photoUrl, createdAt |
| `user_alarms/{uid}` | Firebase Auth UID | Per-user alarm: primaryTribeId, followedAlarmTime, alarmToneUri |
| `tribes/{tribeId}` | Auto-generated | Tribe: name, inviteCode, memberCount, alarmTime, createdBy |
| `tribes/{tribeId}/members/{uid}` | Firebase Auth UID | Member: displayName, photoUrl, streakDays, bestStreak, totalWakes, todayStatus, todayWakeTime, rank, vetoesUsed, joinedAt |
| `tribes/{tribeId}/proposals/{proposalId}` | UUID | Proposal: proposedBy, proposedByName, proposedTime, status, vetoedBy, vetoCount, createdAt, expiresAt |

### Key invariants

- `followedAlarmTime` is the **24-hour** string the user is actually following (`"07:30"`).
- `alarmTime` on the tribe doc is the tribe's shared proposal target.
- A proposal's `expiresAt` = `createdAt + 2 hours`. When the listener sees expiry and nobody vetoed, it auto-accepts via a Firestore batch.
- One veto (`status = "vetoed"`, `vetoCount + 1`) kills a proposal permanently.

---

## Security Rules (firestore.rules)

- `users/{uid}` — read/write **only own** document.
- `user_alarms/{uid}` — read/write **only own** document.
- `tribes/{tribeId}` — read by any authenticated user (needed to join by invite code); create by any auth user; update only by a member; delete forbidden.
- `tribes/{tribeId}/members/{memberUid}` — read by tribe members; create self-only **and only if the member doc doesn't already exist** (blocks re-joining/overwriting); update self-only; delete forbidden.
- `tribes/{tribeId}/proposals/{proposalId}` — read/create/update restricted to tribe members; **create additionally requires `proposedBy == request.auth.uid` and `status == 'pending'`**; **update is restricted to valid status transitions only (`pending → vetoed|accepted`) via `isValidProposalStatusTransition()`, and `proposedBy`/`proposedTime` are immutable after creation**; delete forbidden (audit trail).

Every rule requires `request.auth != null`. No unauthenticated reads, no cross-user writes. The proposal rules in particular enforce state-machine correctness server-side — a client can't forge someone else's proposal, resurrect a vetoed one, or mutate the proposed time after the fact.

---

## Alarm Flow (the reliability story)

```
Set time (proposal accepted / onboarding)
  → HomeViewModel listener updates followedAlarmTime
  → AlarmScheduler.schedule(hour, minute)
  → AlarmManager.setExactAndAllowWhileIdle (falls back to inexact if permission denied)
          │
          ▼  (alarm fires)
AlarmReceiver.onReceive
  → rescheduleTomorrow() — schedules next day IMMEDIATELY (survives force-kill)
  → resets own todayStatus to "sleeping" for the new day (Firestore, best-effort)
  → MissedAlarmReceiver.schedule() — a +10min grace-period check
  → posts full-screen notification (IMPORTANCE_HIGH, bypass DND, visible on lockscreen)
  → AlarmActivity launches over lock screen, screen-on, ringtone loops, vibrates
  → user swipes → handleDismiss()
      → Firestore transaction on the member doc (StreakCalculator.computeNewStreak):
        todayStatus=awake, todayWakeTime, streak math, bestStreak, totalWakes+1
        (transaction, not a plain update — guards against a double-fire double-count)
          │
          ▼  (user never swipes — +10 min later)
MissedAlarmReceiver.onReceive
  → if todayStatus is still "sleeping" → todayStatus=missed, streakDays=0
          │
          ▼  (device rebooted overnight)
BootReceiver.onReceive
  → if a Firebase Auth session is already current, load immediately
  → otherwise waits (AuthStateListener, 5s timeout) for auth to restore post-boot
  → reads user_alarms/{uid} from Firestore
  → falls back to AlarmCache (SharedPreferences) if offline, no doc, or auth-restore times out
  → AlarmScheduler.schedule(hour, minute)
```

**Why reschedule-on-fire instead of reschedule-on-dismiss?** If the app is force-killed or the user never opens the alarm, the next day's alarm must still be set. Rescheduling at fire time guarantees it without depending on UI.

---

## Key Architectural Decisions

| Decision | Why |
|---|---|
| Single Activity + Compose Navigation | One back stack, smooth transitions, auth-driven routing |
| Firestore listeners over one-shot reads | Real-time tribe sync — statuses update live across devices |
| 24-hour time as the canonical format | No AM/PM ambiguity in the system of record |
| Reschedule alarm on fire, not on dismiss | Force-kill survival |
| BootReceiver + local cache | Alarm works with no network after reboot |
| Google-only auth via Credential Manager | No password infra to maintain; Google handles security |
| Streak write is a Firestore transaction, not a plain `update` | Read-then-write atomicity prevents a double-fire from double-incrementing the streak |
| Proposal state transitions enforced in `firestore.rules`, not just client logic | `isValidProposalStatusTransition()` makes `pending→vetoed`/`pending→accepted` the only legal moves server-side; the client's `isAccepting` guard is a convenience, not the security boundary |
| MissedAlarmReceiver (+10min grace) resets streak on a no-show | Reschedule-on-fire guarantees tomorrow's alarm fires; it does not by itself enforce "no snooze" — a separate check is needed to catch a user who simply never dismisses |
| R8 + ProGuard + Crashlytics mapping | Production-size APK + readable crash traces |
| `isNewUser` derived from Firestore data, not just auth | Handles orphaned auth sessions gracefully |

---

## Build & Release

```bash
./gradlew assembleDebug     # debug build
./gradlew assembleRelease   # signed release with R8
```

- Release signing: `app/release-key.jks`, alias `meluko`, password overridable via `KEYSTORE_PASSWORD`/`KEY_PASSWORD` env vars.
- R8 minification + resource shrinking enabled for release.
- Crashlytics mapping file uploaded automatically on release build.
- Keystore is gitignored (`*.jks`).
- Release SHA-1/SHA-256 must be registered in Firebase Console for Google Sign-In to work on the release build.

---

## Resume Bullets

> "Designed and built a native Android app (Kotlin, Jetpack Compose, Material 3) for tribe-based shared wake-up alarms, with real-time Firestore sync, alarm proposal/veto voting, and streak tracking."

> "Engineered a custom SwipeToWake gesture, offline-first alarm caching with boot-time rescheduling, and resilient Google Sign-In (Credential Manager) — all under Dagger Hilt DI."

---

*Last updated: September 2026 — ground-truthed against source as of commit `1dcd2b4` (see `Ref/interview/00-RESUME-DEFENSE.md` for a line-by-line audit of the resume bullets against this code).*
