# Meluko — TCS Prime Interview Quick Sheet (Last-Minute Revision)

> This is the 20% of the material that produces 80% of the questions. Memorize Section A cold. Skim the rest. Always bring answers back to YOUR code.

---

## SECTION A — The 6 questions you MUST nail

### 1. "Tell me about your project." (This is guaranteed. 90% of the interview hinges on it.)

> "I built **Meluko**, a native Android social alarm clock. The problem: individual willpower fails at 7am — a louder alarm doesn't fix snoozing. My insight was that **social accountability beats willpower** — so you join a tribe, everyone wakes to a shared alarm, and if you miss it, your whole tribe sees your status live. The app has three main parts: **auth** (Google Sign-In via Credential Manager + Firebase Auth), **real-time tribe sync** (Firestore listeners — you see who woke up, live), and **system-level alarm reliability** (exact alarms through Doze, a full-screen lock-screen alarm, rescheduling that survives force-kills and reboots, plus offline fallback). Built in Kotlin with Jetpack Compose, Material 3, Dagger Hilt, and Firebase — shipped as a signed release APK on GitHub."

### 2. "What was the biggest technical challenge?" / "What was hard?"

Pick ONE of these (they're your strongest stories):

**A. Making the alarm fire no matter what.**
> "A chat app can show 'offline' and still be fine — an alarm CANNOT miss. The wake-up must fire at the exact second through Doze mode, over the lock screen, even if the phone rebooted or there's no network. I solved it with `setExactAndAllowWhileIdle`, a full-screen notification that launches the alarm activity, rescheduling the next day's alarm *at fire time* (so force-kills can't skip it), and a boot receiver that reads from a local SharedPreferences cache when Firestore is unreachable."

**B. Auth that doesn't hang on a bad network.**
> "First-run is the highest-failure moment. The session check used to block forever when Firestore was slow. I rewrote it with **parallel reads** of two Firestore docs, **2 retries with backoff**, a **10-second timeout**, and a **fallback** that trusts the Firebase session if Firestore is down — so a returning user always gets in instead of staring at a spinner."

**C. Real-time sync with no server.**
> "The whole product is real-time — the tribe sees who woke up. Firestore listeners push updates to every device within ~a second, no polling. And changing the alarm time is a small democracy: anyone proposes, there's a **2-hour veto window**, one veto kills it, otherwise it **auto-accepts** with an atomic Firestore batch."

### 3. "Why did you use X technology?" (table below covers all of them)

| Tech | Why (one-liner) |
|---|---|
| Kotlin | Modern, null-safe, official Android language |
| Jetpack Compose | Declarative UI — state drives the UI, one codebase, less boilerplate than XML |
| Material 3 | Modern design system, dynamic colors, our Sunrise Amber brand |
| Dagger Hilt | Dependency injection — makes ViewModels testable and scoped to lifecycle |
| Firebase Firestore | Real-time listeners, server-side security rules, zero server to maintain |
| Firebase Auth | No password infrastructure, Google handles security |
| Coroutines/Flow | Async without callbacks; StateFlow = observable UI state |
| Credential Manager | Modern Google Sign-In (replaces deprecated GoogleSignInClient) |

### 4. "What is your role in the project?"

> "I designed and built the entire app end-to-end — architecture, data model, security rules, UI, alarm system, auth, and the release build. I made all the architectural decisions, and I can defend each one."

### 5. "What is the one-line pitch / what does it do?"

> "Meluko is a social alarm clock — you join a tribe, everyone wakes to the same alarm, and if you snooze, the whole tribe sees it."

### 6. "What would you improve / what's next?"

> "First, a background rescheduler via a periodic check (in case the alarm is somehow missed). Then push notifications and a proper streak-reset cron. A leaderboard with more granular stats. And a Cloud Function for proposal auto-accept if the user base grows — plus tests (I'd add unit tests for the streak logic and ViewModel logic)." — This shows self-awareness. Interviewers love honest limitations.

---

## SECTION B — Android fundamentals (very likely asked)

**Q: What is AlarmManager?**
A: A system service that schedules an app action at a specific time, even when the app isn't running. The system delivers a broadcast to our `AlarmReceiver`.

**Q: What is the difference between AlarmManager and WorkManager?**
A: WorkManager is for *deferrable* background work (sync, cleanup) — it batches and delays. AlarmManager fires at an *exact* time — required for a wake-up alarm.

**Q: What is Doze mode and how does it affect alarms?**
A: Android's battery optimization that pauses background work when the screen is off. `set()` and `setExact()` get deferred during Doze. `setExactAndAllowWhileIdle()` is allowed to fire even in Doze — that's why we use it.

**Q: What are the exact-alarm permissions?**
A: On Android 12+ (`API 31+`), apps need `SCHEDULE_EXACT_ALARM`; on Android 13+ (`API 33+`), `USE_EXACT_ALARM` (granted automatically for alarm-clock apps). We check `canScheduleExactAlarms()` at runtime and fall back to inexact if denied.

**Q: What is a BroadcastReceiver?**
A: A component that responds to system-wide or app broadcasts — like `BOOT_COMPLETED`. It's launched by the system even if the app is dead. We use `goAsync()` because a receiver is killed ~10 seconds after `onReceive` returns and our Firestore read needs more time.

**Q: What is a full-screen intent / notification?**
A: A high-priority notification (`setFullScreenIntent`) that launches an Activity over the lock screen — used for alarms and calls. Combined with `setShowWhenLocked` + `setTurnScreenOn` so the alarm screen appears with the device asleep.

**Q: What is SharedPreferences?**
A: Android's simple key-value storage. We use it (`AlarmCache`) to save the alarm time locally so the boot receiver can reschedule offline.

**Q: What is MVVM?**
A: Architecture pattern: **Model** (data/Firestore), **View** (Composable UI), **ViewModel** (holds UI state, survives configuration changes). Our ViewModels expose `StateFlow`; the UI collects and renders. Single Activity + Compose Navigation.

**Q: What is DI and why use Hilt?**
A: Dependency injection — you don't `new` up dependencies, they're provided. Hilt generates the injection code at compile time, scopes ViewModels to their lifecycle, and makes testing easy (swap Firebase for a fake).

**Q: What are Coroutines and Flow?**
A: Coroutines = lightweight threads for async without blocking. Flow = reactive data stream; `StateFlow` is the observable state holder our UI collects. e.g. `authState: StateFlow<AuthState>`.

**Q: What is a sealed class and why do we use them?**
A: A class with a fixed set of subtypes. We use them for `AuthState` (Idle/Loading/Success/Error) and `SocialUiState` — forces the UI to handle every state exhaustively.

**Q: What is Jetpack Compose vs XML Views?**
A: Compose is declarative — describe UI from state, and the framework re-renders on state change. XML is imperative — manually update views. Compose = less code, fewer bugs, reactive by default.

**Q: What is a PendingIntent?**
A: A token that lets another process (the system) execute an Intent on your behalf — we use it so AlarmManager can fire our receiver. It uses `FLAG_IMMUTABLE` for security.

---

## SECTION C — Firebase / backend (very likely asked)

**Q: What is Firestore?**
A: Google's NoSQL document database. Data is organized as collections of documents. Key features we use: **real-time listeners** (push updates), **server-side security rules**, **atomic batches**, and **server timestamps**.

**Q: SQL vs NoSQL — why Firestore?**
A: SQL is relational, rigid schema, great for complex queries/joins. NoSQL (Firestore) is document-based, flexible, scales horizontally, and — critical for us — supports **real-time listeners** natively. Perfect for social/real-time data like wake statuses.

**Q: What is your data model?**
A: `users/{uid}` (profile), `user_alarms/{uid}` (alarm time + tribe), `tribes/{id}` (tribe), with subcollections `members/{uid}` and `proposals/{id}`. Every user only writes their own docs; rules are enforced server-side.

**Q: What are Firebase Security Rules?**
A: Server-side rules controlling who can read/write what — clients can't bypass them. Examples: `users` are self-only; tribe updates require an existing membership doc; deletes are forbidden (`if false`) to keep an audit trail.

**Q: What is a Firestore batch and why use it?**
A: Multiple writes committed **atomically** — all succeed or none. Auto-accepting a proposal updates 3 docs (proposal status, tribe time, followed time) in one batch so they can't contradict each other.

**Q: What is OAuth 2.0 / how does Google Sign-In work?**
A: OAuth 2.0 lets a user grant an app access to their identity without sharing a password. Flow: app asks Google for an **ID token** (a JWT) → app sends it to Firebase Auth → Firebase verifies it with Google and creates a session. The ID token is a JWT — a signed JSON web token containing the user's identity claims.

**Q: What is a JWT?**
A: JSON Web Token — a signed, three-part token (header, payload, signature) used to pass verified identity between parties. Our Google ID token is one.

**Q: What is Firebase Auth?**
A: Backend-as-a-service authentication. Supports Google/email/etc. Gives you a signed-in user session that persists across app restarts. We use Google Sign-In so we never store passwords.

**Q: What is Crashlytics?**
A: Firebase crash reporting. The release build uploads R8's mapping file so crash stack traces are de-obfuscated back to readable class/line names.

---

## SECTION D — Your design decisions (they'll probe here)

**Q: "Why do you store the alarm time twice?"**
A: "Deliberate denormalization. The tribe has one `alarmTime`; each user has `followedAlarmTime` — what *their device* schedules. They're kept in sync by a listener. This way a user's alarm survives tribe-doc changes. The redundancy is the resilience."

**Q: "How do you prevent someone cheating their streak?"**
A: "Security rules gate every write, and a `todayStatus == 'awake'` guard prevents double-counting. The honest answer: the real anti-cheat is social pressure — you can't fake waking up to your friends."

**Q: "How do you handle offline?"**
A: "Two layers. **User experience:** HomeViewModel loads the alarm time from cache instantly, then Firestore updates it live; an offline banner shows 'alarm still set'. **System:** AlarmCache (SharedPreferences) lets the boot receiver and alarm receiver work with zero network. Firestore is the source of truth; cache is the fallback."

**Q: "How would you scale this to 10,000 users?"**
A: "Firestore scales horizontally, so the data layer is fine. I'd (1) move proposal auto-accept to a Cloud Function, (2) add Firestore indexes for the leaderboard query, (3) add push notifications, (4) add caching/queries for the member list, and (5) write unit tests before scaling."

**Q: "Why did you choose this architecture?"**
A: "Single Activity + Compose Navigation + MVVM with StateFlow. One back stack, no fragment lifecycle pain, declarative UI driven by state, and ViewModels survive configuration changes. Firebase handles the server side so I could ship solo."

---

## SECTION E — Behavioral (almost always asked)

**Q: Tell me about yourself.**
A: "I'm a Computer Science student who builds Android apps end-to-end. I've built two projects: Meluko, a social alarm clock (native Kotlin/Compose/Firebase, shipped as a signed release APK), and ContextIQ, a retrieval-augmented AI system that answers questions from research papers with cited sources. I like measurable engineering — ContextIQ benchmarks its retrieval quality, and Meluko is designed around system reliability."

**Q: Why do you want to join TCS Prime?**
A: "TCS Prime works on high-value digital transformation for enterprise clients — large-scale, production-grade systems. That's exactly the environment I want: real users, real scale, and modern tech. I've already shipped production software, so I'm looking for a team where I can go deeper on distributed systems and large-scale app engineering."

**Q: What's your strength?**
A: "End-to-end ownership and shipping — I build the whole thing, decide the architecture, and ship a signed production build. And I can explain every decision I made."

**Q: What's a weakness?**
A: "I'm still learning to write tests before features — Meluko has manual QA but fewer automated tests than I'd like. I've started adding unit tests for the streak and proposal logic." (Turn a real weakness into an active fix.)

**Q: How do you handle conflict / a disagreement?**
A: "I listen first, then present data. In projects I defend decisions with concrete reasoning — e.g. 'reschedule at fire time' vs 'on dismiss' — and I'm happy to change when the data says I'm wrong."

**Q: Where do you see yourself in 5 years?**
A: "Growing from a developer into a senior/lead engineer — owning features end-to-end, mentoring juniors, and eventually leading architecture for a product team at TCS."

---

## SECTION F — "Do you know anything we didn't cover?" (last question — always say yes and be ready to talk)

Offer one of these briefly:
- Real-time sync with Firestore listeners
- How the alarm system survives reboots/force-kills
- The proposal/veto auto-accept logic (atomic batch)
- Auth resilience (retries + fallback)

---

## Rapid-fire recall (say these out loud before bed)

1. Meluko = social alarm clock, tribe accountability beats willpower.
2. Stack: Kotlin + Compose + Material 3 + Hilt + Firebase.
3. Alarm = `setExactAndAllowWhileIdle` (fires through Doze) + full-screen notification over lock screen.
4. Reschedule tomorrow's alarm AT FIRE TIME — survives force-kill.
5. Boot receiver + SharedPreferences cache = works offline.
6. Auth: Credential Manager → Google ID token (JWT) → Firebase Auth.
7. `isNewUser` decided by Firestore data, checked with parallel reads + retries + 10s timeout + fallback.
8. Proposals: 2h veto window, one veto kills, auto-accept via atomic batch.
9. Security rules: self-only writes, member-gated updates, no deletes.
10. Release: R8 minify + ProGuard + mapping upload + separate release SHA fingerprint.

**Final tip:** For every answer, name the FILE (HomeViewModel, AlarmScheduler, AuthViewModel). It sounds like you own the code — because you do.
