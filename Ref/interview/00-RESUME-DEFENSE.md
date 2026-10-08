# Resume Defense — Meluko

> Purpose: defend every word of the two Meluko resume bullets, and the Meluko-relevant rows of the
> shared Technical Skills table, against the **actual current source on disk**, not against what the
> other interview docs say about it, and not against the last commit alone. Ground-truthed directly
> against the Kotlin source, `firestore.rules`, `AndroidManifest.xml`, `app/build.gradle.kts`, and
> `gradle/libs.versions.toml` as they currently exist in the working tree — September 2026 audit.
>
> **Note on repo state:** the working tree has substantial uncommitted changes beyond the last commit
> (`1dcd2b4`, 2026-08-16) — `git status` shows `alarm/StreakCalculator.kt`, `alarm/MissedAlarmReceiver.kt`,
> and `util/TimeUtils.kt` as untracked new files, `navigation/SafeNavigation.kt` and `ui/UiState.kt` as
> deleted, and most of `alarm/`, `auth/`, and `ui/` as modified but uncommitted. Every citation below
> is checked against this current on-disk state (via direct file reads), which is more current — and
> in places meaningfully more sophisticated — than both the last commit and the interview docs written
> against it. Commit this work before an interview: an interviewer who clones the repo from its last
> pushed commit won't see `StreakCalculator`, `MissedAlarmReceiver`, or the transaction-based streak
> write this document cites as evidence.
>
> This is not marketing copy. Where the resume's wording is stronger than the code supports, that's
> called out as PARTIAL with the honest answer — the resume wording itself is left for you to decide;
> this document only tells you where the gap is.

## The two bullets, word for word

> **Bullet 1:** "Designed and built a native Android app (Kotlin, Jetpack Compose, Material 3) for
> tribe-based shared wake-up alarms, with real-time Firestore sync, alarm proposal/veto voting, and
> streak tracking."

> **Bullet 2:** "Engineered a custom SwipeToWake gesture, offline-first alarm caching with boot-time
> rescheduling, and resilient Google Sign-In (Credential Manager) — all under Dagger Hilt DI."

---

## Bullet 1

### "native Android app"

**Verdict: CONFIRMED**

The whole app is a standard Gradle Android application module (`app/build.gradle.kts`, plugin
`android.application`) targeting the Android SDK directly — no Flutter, React Native, or other
cross-platform layer anywhere in the tree. `namespace = "com.alarm.meluko"`, `minSdk = 24`,
`targetSdk = 36` (`app/build.gradle.kts:11-21`). Single Activity, `AndroidManifest.xml` declares
Android-native components only (`Activity`, three `BroadcastReceiver`s).

**Why native and not cross-platform?** This app leans hard on Android-specific system APIs that
cross-platform frameworks either don't expose cleanly or add a plugin-bridge layer around:
`AlarmManager.setExactAndAllowWhileIdle` for Doze-surviving exact alarms, `BroadcastReceiver` +
`goAsync()` for boot/alarm handling, `setShowWhenLocked`/`setTurnScreenOn` for a lock-screen alarm
activity, `NotificationManager` full-screen intents, and `Credential Manager`. All of these are
system-level enough that a native app removes an entire class of "does the plugin actually expose
this Doze/lock-screen edge case" risk — for a wake-up alarm, that risk is the whole product.

**Citations:** `app/build.gradle.kts:1-21`, `app/src/main/AndroidManifest.xml` (full file).

---

### "Kotlin"

**Verdict: CONFIRMED**

Every source file under `app/src/main/java/com/alarm/meluko/` is `.kt` (48 files, zero `.java`
files in the app module — verified with `find ... -name "*.java"`, zero results). Kotlin 2.2.10
(`gradle/libs.versions.toml:9`).

**Why Kotlin over Java?** Null-safety at the type level removes an entire class of NPEs that this
codebase actively depends on being caught at compile time — e.g. every Firestore field read is
`doc.getString(...)  ?: default` (`ProposalViewModel.kt:110-117`), and the sealed-class exhaustive
`when` on `AuthState`/`WakeStatus` (`AuthViewModel.kt:37-42`, `SocialViewModel.kt:73-76`) only pays
off because the compiler enforces exhaustiveness — that's a Kotlin feature Java lacks pre-records/
sealed-interfaces, and even then not with the same ergonomics. Coroutines (`viewModelScope.launch`,
`async`/`await`, `withTimeoutOrNull`) are the backbone of the auth-resilience logic
(`AuthViewModel.kt:74-129`) — doing that in Java would mean `AsyncTask`/callbacks or RxJava, both
more ceremony for the same result. It's also simply the officially recommended, Compose-first
language for Android since 2019 — not a contrarian choice.

**Citations:** file tree under `app/src/main/java/com/alarm/meluko/`, `gradle/libs.versions.toml:9`.

---

### "Jetpack Compose"

**Verdict: CONFIRMED**

`buildFeatures { compose = true }` (`app/build.gradle.kts:59`), Compose BOM `2026.05.01`
(`gradle/libs.versions.toml:10`). All ~20 screens are `@Composable` functions — e.g.
`SwipeToWakeButton.kt:64-65` (`@Composable fun SwipeToWakeButton(...)`), `HomeScreen.kt`,
`ExpressiveUtils.kt:72-73` (`@Composable fun ScreenHeader`). No XML layout files, no `findViewById`,
no `Activity.setContentView(R.layout...)` — every screen is built with `setContent { ... }`
(`MainActivity.kt`, `AlarmActivity.kt:128-137`).

**Why Compose over XML Views?** Every screen in this app is driven by `StateFlow` collected via
`collectAsState()` — e.g. `HomeScreen.kt:65-80` collects six separate `StateFlow`s from
`HomeViewModel`. That's a reactive, state-drives-UI model; XML Views would require manually pushing
each field into a `TextView`/`ImageView` via observers or `LiveData` binding adapters — more
boilerplate for the same reactivity, and no compile-time guarantee that a state field maps to a UI
update. `SwipeToWakeButton` (see Bullet 2) would be substantially more code as a custom `View`
subclass overriding `onTouchEvent`/`onDraw` versus the declarative `pointerInput` + `Animatable` +
`drawBehind` approach actually used.

**Citations:** `app/build.gradle.kts:58-61`, `SwipeToWakeButton.kt:64-65`, `HomeScreen.kt:61-80`,
`ExpressiveUtils.kt:72-102`.

---

### "Material 3"

**Verdict: CONFIRMED**

Dependency `androidx.compose.material3:material3` via the Compose BOM
(`gradle/libs.versions.toml:33`, `app/build.gradle.kts:72`). `androidx.compose.material3.Icon` and
`androidx.compose.material3.Text` are imported and used directly in `SwipeToWakeButton.kt:19-20`;
`ExpressiveUtils.kt:90-99` reads `MaterialTheme.colorScheme.primary` /
`MaterialTheme.colorScheme.onSurfaceVariant` (Material 3's `MaterialTheme`, not the Material 2
package). `enableEdgeToEdge()` in `MainActivity.kt` is a Material 3-era API.

**Why Material 3 over Material 2 or a fully custom design system?** Material 3 gives dynamic color
(wallpaper-derived theming on Android 12+) essentially for free through `MaterialTheme` — the app
still layers a custom brand (Sunrise Amber `#FF8C00` seed, Outfit font, near-black background,
0dp-elevation flat cards) on top rather than shipping stock Material, but doing that on M3's token
system (`colorScheme`, `typography`) means every screen inherits the brand consistently instead of
hardcoding colors per-screen — and it comes with Compose's official, actively maintained component
set instead of hand-rolling buttons/sheets/cards from scratch.

**Citations:** `gradle/libs.versions.toml:33`, `app/build.gradle.kts:72`,
`SwipeToWakeButton.kt:19-20`, `ExpressiveUtils.kt:90,99`.

---

### "tribe-based shared wake-up alarms"

**Verdict: CONFIRMED**

Data model has a `tribes/{tribeId}` collection with a shared `alarmTime` field, and each user has a
`user_alarms/{uid}` doc with `followedAlarmTime` kept in sync with the tribe's target
(`data/FirestoreConstants.kt`, cited by field name throughout). `HomeViewModel.kt:101-115` listens
on the tribe doc and self-heals `followedAlarmTime` to match `tribeAlarm` whenever they diverge —
this is the actual "shared" mechanic: every member's device reconciles to one tribe-level value.

**Citations:** `HomeViewModel.kt:80-134` (the whole listener chain: `user_alarms` → tribe →
member), `firestore.rules:36-43` (`tribes/{tribeId}` schema/rules).

---

### "real-time Firestore sync"

**Verdict: CONFIRMED**

`addSnapshotListener` (Firestore's push-based real-time listener, not `.get()` one-shot reads) is
used for every piece of live data: `HomeViewModel.kt:81` (user alarm), `:104` (tribe),
`:120` (member); `SocialViewModel.kt:60,112,128` (members, user-alarm, tribe); `ProposalViewModel.kt:67,124`
(proposals, own member doc for veto count).

**Why listeners over polling or a custom backend?** The product's core mechanic — "your tribe sees
you miss the alarm, live" — is only true if a state change on one device reaches every other tribe
member's screen without a manual refresh. Polling would mean picking an interval that's either too
slow (stale status) or too battery-costly (frequent reads on N devices — Firestore document reads
are billed per-read, so naive polling also multiplies cost linearly with tribe size × poll
frequency). A custom backend doing the equivalent would need to stand up WebSockets or SSE, its own
auth, and its own database — Firestore's client SDK gives real-time push, server-enforced security
rules (`firestore.rules`), and zero infrastructure to operate, which matters directly for a
solo-built, self-funded app with no ops budget.

**Citations:** `HomeViewModel.kt:81,104,120`, `SocialViewModel.kt:60,112,128`,
`ProposalViewModel.kt:67,124`.

---

### "alarm proposal/veto voting"

**Verdict: CONFIRMED**

`ProposalViewModel.createProposal` (`ProposalViewModel.kt:134-175`) writes a `pending` proposal with
a 2-hour `expiresAt`. `castVeto` (`:177-210`) sets `status = "vetoed"` via
`FieldValue.arrayUnion`/`FieldValue.increment`. The listener in `listenToTribeProposals`
(`:61-119`) auto-accepts an expired, un-vetoed proposal via an atomic `db.batch()`
(`acceptProposal`, `:212-240`) that updates the proposal, the tribe's `alarmTime`, and the caller's
`followedAlarmTime` together. This is enforced **server-side**, not just in the ViewModel:
`firestore.rules:62-72` requires `create` to set `proposedBy == request.auth.uid` and
`status == 'pending'`, and restricts `update` to `pending → vetoed` or `pending → accepted` via
`isValidProposalStatusTransition()` — a client cannot forge someone else's proposal, resurrect a
vetoed one, or rewrite `proposedTime` after creation, regardless of what the app's own code does.

**Why a veto-based system with a 2-hour expiry, instead of simple majority voting?** Majority voting
needs a quorum rule (what happens with 2 members and a 1-1 split? what if half the tribe doesn't
vote before the alarm fires tomorrow?) and a mechanism to close the vote (a scheduled tally). A
single veto killing the proposal sidesteps both: the default outcome is *change happens* (low
friction — anyone can nudge the group's wake time), and the check on that default is *any one
member* objecting, not a counted majority — which matches the product's real trust model (a tribe is
a small, high-trust friend group; one person's objection should be enough to block a change that
affects their morning, not require them to rally votes against it). The 2-hour window is long enough
to be seen and acted on (most tribes will be awake or checking their phone within 2 hours of any
reasonable proposal time) but short enough that a stale proposal doesn't linger indefinitely; expiry
is computed from a Firestore **server timestamp** (`createdAt`, `ProposalViewModel.kt:161`), not the
proposing device's clock, so a wrong device clock can't corrupt the window.

**Citations:** `ProposalViewModel.kt:61-240`, `firestore.rules:57-72`.

---

### "streak tracking"

**Verdict: CONFIRMED**

`StreakCalculator.computeNewStreak` (`alarm/StreakCalculator.kt:12-24`) is a pure function (no
Android/Firebase types) implementing "yesterday → +1, else reset to 1." It's invoked inside a
Firestore **transaction** in `AlarmActivity.handleDismiss` (`alarm/AlarmActivity.kt:161-197`), which
reads the member doc and writes `streakDays`/`bestStreak`/`totalWakes`/`lastWakeDate` atomically.
`MissedAlarmReceiver` (`alarm/MissedAlarmReceiver.kt:50-67`) fires 10 minutes after the main alarm
and resets `streakDays` to 0 if the user never dismissed. `SocialViewModel.kt:80,92-107` sorts the
tribe leaderboard by `streakDays` descending and recomputes/persists each user's own `rank`.

**Citations:** `alarm/StreakCalculator.kt`, `alarm/AlarmActivity.kt:140-213`,
`alarm/MissedAlarmReceiver.kt:27-72`, `ui/SocialViewModel.kt:80,92-107`.

---

## Bullet 2

### "Engineered a custom SwipeToWake gesture"

**Verdict: CONFIRMED**

`SwipeToWakeButton` (`ui/SwipeToWakeButton.kt:64-274`) is hand-built from Compose's low-level
gesture primitive `detectHorizontalDragGestures` inside `pointerInput` (`:117-151`) — it is not
Android's `SeekBar`, not a copy of a swipe-to-action library, and not a stock Compose component.
It implements: a computed drag-completion `threshold` at 85% of track width (`:82`); an
`Animatable`-driven spring-back on release below threshold, or a spring-to-end + `onWake()` callback
above it (`:126-141`); two-stage haptic feedback (`TextHandleMove` on crossing threshold,
`LongPress` on completion, `:147,131`); a custom shimmer text effect that only animates while
visible (`:99-110`); and a glow/aura effect drawn with `drawBehind` that intensifies as drag
progress increases (`:234-245`).

**Why build this instead of using a stock slider or a swipe library?** A `Slider` is a
value-picker, not a momentary confirm gesture — it has no "complete and reset" semantics, no
threshold-triggered callback, and defaults to Material's drag/track visuals rather than the
"sunrise" glow/shimmer brand. A generic swipe-to-confirm library would need to be forked anyway to
get the specific threshold behavior (85%, not the library's default), the two-tier haptics, and the
brand-specific visuals — at which point it's simpler and more maintainable to own the ~200 lines
directly against Compose's gesture and animation primitives, which is what happened here.

**Citations:** `ui/SwipeToWakeButton.kt:64-274` (full component).

---

### "offline-first alarm caching"

**Verdict: PARTIAL**

The caching is real and it does make the alarm system work with zero network — that outcome is
true. But "offline-first" is a specific architectural term (local storage is the source of truth;
the network syncs opportunistically in the background), and that is **not** the pattern actually
implemented. The code and the project's own docs are explicit about the opposite priority:
`AlarmCache.kt:6-12`'s own doc comment says *"Firestore is the source of truth; this is a fallback
for system-level reliability."* Concretely:

- `BootReceiver.kt:62-93` (`loadAlarmAndSchedule`) tries **Firestore first**, and only reads
  `AlarmCache` on failure or a missing field (`:73-92`).
- `HomeViewModel.kt:62-99` does read the cache first for **instant display** (`:62-69`), but the
  live Firestore listener immediately overwrites it once the first snapshot arrives (`:94-99`) —
  this is a "cache-then-network" / stale-while-revalidate read pattern for UI responsiveness, not an
  offline-first write/sync architecture.
- There is no local write queue, no conflict resolution, and no background sync-when-reconnected
  logic — `AlarmCache` stores exactly three flat values (`alarm_time`, `tribe_id`,
  `alarm_tone_uri`, `AlarmCache.kt:16-18`) written as a side effect whenever the corresponding
  Firestore value is read, not as the primary write target.

**Honest interview answer:** "It's not offline-first in the strict architectural sense — Firestore
is the single source of truth, and the SharedPreferences cache is a narrow, deliberate fallback for
the handful of values the alarm and boot receivers need to read synchronously with no guaranteed
network. What I'd actually defend is the *outcome*: the alarm keeps firing correctly with zero
connectivity, because those three values are cached and the receivers degrade to them. If asked to
build genuine offline-first behavior — e.g. letting a user *create* a proposal or dismiss an alarm
while offline and have it sync later — that would need a local write queue (Room, or Firestore's own
offline persistence, which is a distinct feature from `AlarmCache` and isn't currently enabled) and
conflict-resolution logic that doesn't exist today."

**What fixing the resume wording (or the code) would take:** Either (a) reword to something like
"resilient offline alarm caching" or "boot-safe local alarm cache" which matches what's built, or
(b) if the "offline-first" claim needs to stay, actually build the missing piece — enable Firestore's
offline persistence (`FirebaseFirestoreSettings.setPersistenceEnabled`) and/or move `AlarmCache` to
be read-first everywhere (not just in `HomeViewModel`'s initial paint) with explicit background
reconciliation. Flagging only — not deciding the wording for you.

**Citations:** `data/AlarmCache.kt:1-47`, `alarm/BootReceiver.kt:62-93`, `ui/HomeViewModel.kt:60-99`.

---

### "boot-time rescheduling"

**Verdict: CONFIRMED**

`BootReceiver` is registered in `AndroidManifest.xml` for `android.intent.action.BOOT_COMPLETED`
with `exported="true"` (required for the system to deliver the boot broadcast). On receipt, it
either uses the already-current `FirebaseAuth.currentUser` or waits up to 5 seconds via an
`AuthStateListener` for Firebase's own async session restore (`BootReceiver.kt:22-59`) — this exists
because `FirebaseAuth.currentUser` can still be `null` for a moment right after boot even for a
signed-in user, purely due to Firebase's own restore timing, and treating that as "signed out" would
silently skip rescheduling on every single reboot. It then reads `user_alarms/{uid}` from Firestore,
falling back to `AlarmCache` on any failure (`:62-93`), and calls
`AlarmScheduler(context).schedule(hour, minute)` (`:100`), which re-arms
`setExactAndAllowWhileIdle` (`AlarmScheduler.kt:53-57`) — necessary because Android clears all
`AlarmManager` alarms on every reboot.

**Why reschedule-on-fire (in `AlarmReceiver`) instead of reschedule-on-dismiss, as the primary
guarantee, with `BootReceiver` as the second layer?** Dismissal requires a live user who actually
opens and swipes the alarm — a force-kill, a missed dismissal, or the user simply not touching the
phone would mean "reschedule on dismiss" never runs, silently killing every future alarm.
Rescheduling happens instead in `AlarmReceiver.onReceive` (`AlarmReceiver.kt:37`,
`rescheduleTomorrow`), which the OS invokes unconditionally when the alarm fires, regardless of app
state — it is the one point in the whole flow that cannot be skipped by anything except the device
being off, which is exactly the case `BootReceiver` exists to cover.

**Citations:** `AndroidManifest.xml` (`<receiver android:name=".alarm.BootReceiver" ...>` block),
`alarm/BootReceiver.kt:15-101` (full file), `alarm/AlarmReceiver.kt:32-42`,
`alarm/AlarmScheduler.kt:35-74`.

---

### "resilient Google Sign-In (Credential Manager)"

**Verdict: CONFIRMED**

`AuthViewModel.signInWithGoogle` (`auth/AuthViewModel.kt:137-198`) uses
`androidx.credentials.CredentialManager` + `GetGoogleIdOption` + `GoogleIdTokenCredential`
(`:142-158`) — the modern unified credential API — not the deprecated
`com.google.android.gms.auth.api.signin.GoogleSignInClient`. That deprecated class does not appear
anywhere in the source tree (verified by grep).

"Resilient" is backed by concrete, citable mechanisms, not just adjective:
- The credential exchange itself is wrapped in a 15-second `withTimeoutOrNull`
  (`AuthViewModel.kt:162`), surfacing "Sign-in timed out" instead of an infinite spinner.
- `checkExistingSession` (`:65-95`) reads two Firestore docs **in parallel** via `coroutineScope` +
  `async` (`:103-113`), retries up to 2 times with 500ms/1000ms backoff (`:100-127`), times out at
  10 seconds (`:102`), and **falls back to trusting the Firebase Auth session** (assuming a
  returning user) if Firestore is completely unreachable (`:85-93`) — so a network blip degrades to
  "onboarding shown once more," never a stuck login screen.
- Post-auth Firestore sync (`syncFirestoreAfterAuth`, `:205-254`) is explicitly non-blocking:
  a failed user-doc write only logs a warning (`:226-228`); the whole sync is wrapped in
  `withTimeoutOrNull(...) ?: true` with a `catch { true }` fallback (`:249-253`) — auth success is
  never held hostage by a Firestore write.

**Why Credential Manager over the old `GoogleSignInClient` SDK?** The old SDK is deprecated by
Google and built around `onActivityResult`-style callbacks that don't compose well with Compose's
lifecycle or coroutines. Credential Manager is a single suspend call
(`credentialManager.getCredential(...)`, `:153`) that fits directly into `viewModelScope.launch`
alongside the rest of the resilience logic (timeouts, retries) — the two are the same design
decision, not separate ones: resilience with callback-based `onActivityResult` code would mean
threading timeouts/retries through callback state machines instead of straight-line suspend code.

**Citations:** `auth/AuthViewModel.kt:1-267` (full file, cited lines above), grep for
`GoogleSignInClient` across the source tree (zero matches).

---

### "Dagger Hilt DI"

**Verdict: CONFIRMED**

`@HiltAndroidApp` on `MelukoApplication` (`MelukoApplication.kt:11-12`); `@AndroidEntryPoint` on
`MainActivity` and `AlarmActivity` (`alarm/AlarmActivity.kt:40`); every ViewModel is `@HiltViewModel`
with `@Inject constructor(...)` (`AuthViewModel.kt:44-47`, `HomeViewModel.kt:22-28`,
`ProposalViewModel.kt:34-38`, `SocialViewModel.kt:24-28`); `FirebaseModule`
(`di/FirebaseModule.kt:12-27`) is a `@Module @InstallIn(SingletonComponent::class) object`
`@Provides`-ing `FirebaseAuth`/`FirebaseFirestore`/`FirebaseStorage` as `@Singleton`; KSP is used for
Hilt's annotation processing (`ksp(libs.hilt.compiler)`, `app/build.gradle.kts:84`, faster than
kapt). `AlarmActivity` also field-injects (`@Inject lateinit var firebaseAuth`/`firebaseFirestore`,
`:47-48`) since a `BroadcastReceiver`/non-Fragment-non-ViewModel Android component can't use
constructor injection the same way.

**Why Dagger Hilt over manual DI or Koin?** Manual DI here would mean either a hand-written
singleton locator or threading `FirebaseAuth`/`FirebaseFirestore` instances through every
constructor by hand across ~9 ViewModels plus `AlarmActivity` — mechanical, error-prone boilerplate
that Hilt's `@Provides` module removes entirely. Compile-time correctness is the concrete advantage
over Koin (a popular alternative): Koin resolves dependencies at runtime via a service-locator-style
DSL, so a missing binding is a runtime crash; Hilt's KSP-generated components fail the *build* if a
dependency graph is incomplete (see `FirebaseModule`'s explicit providers) — for a solo project with
no dedicated QA, catching a wiring mistake at compile time instead of in a crash report is the
concrete reason to prefer it. Hilt's `@HiltViewModel` + `hiltViewModel()` also handles
`ViewModel`-scoping to the right lifecycle automatically, which a hand-rolled factory would have to
reimplement.

**Citations:** `MelukoApplication.kt:11-12`, `alarm/AlarmActivity.kt:40,47-48`,
`auth/AuthViewModel.kt:44-47`, `ui/HomeViewModel.kt:22-28`, `di/FirebaseModule.kt:1-27`,
`app/build.gradle.kts:5,82-85`.

---

## Technical Skills table (Meluko-relevant rows)

### Android Development: Kotlin, Jetpack Compose, Material 3, MVVM

**Kotlin / Jetpack Compose / Material 3 — Verdict: CONFIRMED.** Same evidence as Bullet 1 above;
not repeated.

**MVVM — Verdict: CONFIRMED, with one honest caveat.**

The pattern is real and consistently applied: **Model** = Firestore documents mapped to plain
Kotlin data classes (`AlarmProposal`, `ProposalViewModel.kt:22-32`; `TribeMember`/`TribeProgress`,
referenced in `SocialViewModel.kt`); **ViewModel** = `@HiltViewModel` classes exposing
`StateFlow`/`MutableStateFlow` (every `*ViewModel.kt` file — e.g. `HomeViewModel.kt:30-52`); **View**
= stateless `@Composable` screens that only `collectAsState()` and render, with zero business logic
and zero `ViewModel` instantiated by hand (always via `hiltViewModel()`) — e.g.
`HomeScreen.kt:61-80`. ViewModels survive configuration changes by construction (standard
`androidx.lifecycle.ViewModel` behavior) and are unit-testable in isolation because Firebase
instances are injected, not constructed internally (`HomeViewModel.kt:22-28` takes `FirebaseAuth`/
`FirebaseFirestore` as constructor params, not `FirebaseAuth.getInstance()` inline — contrast with
`BootReceiver.kt`/`MissedAlarmReceiver.kt`, which *do* call `.getInstance()` directly because
`BroadcastReceiver`s aren't part of the Hilt-ViewModel graph).

**The caveat:** there is **no Repository layer**. ViewModels talk directly to the injected
`FirebaseFirestore`/`FirebaseAuth` SDK objects (e.g. `ProposalViewModel.kt:36-37`,
`HomeViewModel.kt:26-27`) rather than through a repository abstraction that would sit between them
and Firebase. This is a legitimate, common lightweight-MVVM variant for an app this size (a
repository only earns its keep once there are multiple data sources to reconcile, or a real need to
swap Firestore for a fake in tests beyond what constructor injection already allows) — but if an
interviewer asks "so where's your repository layer / data layer," the honest answer is "there isn't
one — the ViewModels are thin enough that Hilt-injected Firebase instances play that role directly,
and I'd introduce a Repository if the app needed to abstract over more than one backend or needed
deeper testing isolation than swapping the injected Firebase instance already gives me."

**Citations:** `ui/HomeViewModel.kt:22-52`, `ui/ProposalViewModel.kt:22-38`,
`ui/SocialViewModel.kt:15-28`, `ui/HomeScreen.kt:61-80`, `alarm/BootReceiver.kt:26`,
`alarm/MissedAlarmReceiver.kt:31`.

---

### Languages & Tools: Java, C, Kotlin, Python, Git/GitHub

**Kotlin — Verdict: CONFIRMED.** Same evidence as above.

**Git/GitHub — Verdict: CONFIRMED.** The project is a git repository with 13 commits from
`5d50b8a` (2026-04-21, "Initial commit") through `1dcd2b4` (2026-08-16); `Meluko_Master.md` states
the signed v1.0 release APK is published via GitHub Releases. `git log --oneline` is directly
citable as evidence of iterative, dated development (initial commit → auth → UI → alarm system →
release hardening → doc additions).

**Java — NOT evidenced by this project.** A full search of `app/src/main/java/` found **zero
`.java` files** — every source file is Kotlin. There is no NDK, no `externalNativeBuild`, no CMake,
and no `.java` anywhere in the app module (verified by `find` for `*.java` and by checking
`app/build.gradle.kts`/`settings.gradle.kts` for any NDK/native config — none present). If "Java" on
the resume is meant to be backed by this project, it is currently **UNSUPPORTED** here — Meluko is
100% Kotlin. This is very likely legitimately sourced from coursework or another project instead, in
which case no action is needed on Meluko's account — just don't reach for a Meluko example if asked
to demonstrate Java specifically.

**C — NOT evidenced by this project.** Same check as above: no `.c`/`.cpp`/`.h` files, no
`CMakeLists.txt`, no NDK dependency, no JNI (`System.loadLibrary`, `external fun`, `native` keyword —
none found). **UNSUPPORTED** by Meluko. Almost certainly coursework (a typical CS-program C course)
rather than this project — again, no fix needed on Meluko's side, just don't cite Meluko as the C
example.

**Python — weakly evidenced, and arguably shouldn't be cited from this project.** The only Python
in the repo is `Ref/interview/flashcard_quiz.py` — a personal study CLI tool for drilling this very
interview material, not part of the shipped Android app, not something an interviewer reviewing
"the Meluko project" would ever see or that shows Python engineering skill (it's a ~14KB
flashcard-and-grading script, not a Meluko feature). Treat Python on the resume as backed by a
different project (the docs reference "ContextIQ," described elsewhere in this Ref/interview series
as a Python RAG system) — if pressed on Python specifically in the context of *this* project, the
honest answer is "Meluko itself is 100% Kotlin; my Python work is on [the other project]."

**Citations:** `find app/src/main/java -name "*.java"` → no results; `find . -iname "*.cpp" -o
-iname "*.c" -o -iname "CMakeLists.txt"` → no results (excluding `build/`); `git log --oneline`
(13 commits, `5d50b8a`..`1dcd2b4`); `Ref/interview/flashcard_quiz.py` (exists, but is tooling, not
app code).

---

## Summary table

| Term | Verdict | One-line reason |
|---|---|---|
| native Android app | CONFIRMED | Pure Android Gradle module, no cross-platform layer |
| Kotlin | CONFIRMED | 100% `.kt`, zero `.java` in app module |
| Jetpack Compose | CONFIRMED | `compose = true`, all screens `@Composable`, no XML layouts |
| Material 3 | CONFIRMED | `material3` artifact, `MaterialTheme.colorScheme` used directly |
| tribe-based shared wake-up alarms | CONFIRMED | `tribes.alarmTime` ↔ `user_alarms.followedAlarmTime` sync loop |
| real-time Firestore sync | CONFIRMED | `addSnapshotListener` throughout, no `.get()` polling for live data |
| alarm proposal/veto voting | CONFIRMED | Full lifecycle + server-enforced state machine in `firestore.rules` |
| streak tracking | CONFIRMED | Transactional streak write + pure `StreakCalculator` + missed-check reset |
| custom SwipeToWake gesture | CONFIRMED | Hand-built on `detectHorizontalDragGestures`, not a stock component |
| offline-first alarm caching | **PARTIAL** | Real fallback cache, but Firestore is authoritative — not architecturally "offline-first" |
| boot-time rescheduling | CONFIRMED | `BootReceiver` + `BOOT_COMPLETED` + auth-restore race handling |
| resilient Google Sign-In (Credential Manager) | CONFIRMED | Modern API + timeouts/retries/fallback, concretely citable |
| Dagger Hilt DI | CONFIRMED | `@HiltAndroidApp`/`@HiltViewModel`/`@Module` throughout |
| MVVM (skills table) | CONFIRMED* | Real MVVM; *no Repository layer — be ready for that follow-up |
| Java (skills table) | UNSUPPORTED (by this project) | Zero `.java` files; almost certainly coursework |
| C (skills table) | UNSUPPORTED (by this project) | No C/C++/NDK anywhere; almost certainly coursework |
| Python (skills table) | UNSUPPORTED (by this project) | Only Python present is this Ref/ study tooling, not app code |

**Bottom line:** 14 of 16 checkable terms are fully CONFIRMED with direct file:line evidence. One
(**offline-first alarm caching**) has real underlying functionality but uses an architectural label
that doesn't match the actual cache-as-fallback pattern — see that section for the exact honest
answer to give if probed. Java, C, and Python are cleanly outside this project's actual footprint;
that's expected (they're a shared cross-project skills row) and not a defect in Meluko specifically,
just something to be precise about if an interviewer asks "show me where you used C" and points at
this project.
