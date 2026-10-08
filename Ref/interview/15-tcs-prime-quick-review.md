# TCS Prime — Overnight Quick Review

> Last-minute study sheet. Highest-probability interview questions + short answers.
> Project = Meluko (social alarm clock, Android/Kotlin). Rehearse the bolded lines out loud.
>
> Overlaps with `quick-sheet-tcs.md` (same interview, same project) — this is the shorter cram pass; it adds generic OOP/SQL/coding-round basics not covered there. For project-specific technology defenses, `00-RESUME-DEFENSE.md` is the code-verified source of truth.

---

## 1. Introduce yourself (~60s)

> "I'm a final-year student who builds Android apps with Kotlin and Jetpack Compose. My main project is **Meluko**, a social alarm clock — you join a tribe, everyone wakes to the same alarm, and if you snooze, the whole tribe sees it. It uses Firebase for real-time sync, Dagger Hilt for dependency injection, and Material 3 design. I've also built **ContextIQ**, a document Q&A system using retrieval-augmented generation. I enjoy the full journey — from idea to a signed, release-ready APK on GitHub."

## 2. "Walk me through your project"

> "Meluko solves a simple problem: willpower fails at 7am. So I made waking up social. The flow: sign in with Google → join or create a tribe → set a shared alarm time. Anyone can **propose a new time**; members have a **2-hour window to veto**; one veto kills it, otherwise it auto-accepts. Every morning the alarm fires on **AlarmManager**, a receiver posts a **full-screen notification** that launches the alarm screen over the lock screen, and when you swipe to wake, your **streak updates in Firestore** and the whole tribe sees you're awake in real time."

## 3. Top 10 "tell me" questions

| Question | One-liner answer |
|---|---|
| Biggest challenge? | Making the alarm **reliable at the system level** — it must fire exactly, through Doze, over the lock screen, even after a reboot or offline. |
| How does it stay reliable offline? | **AlarmCache** (SharedPreferences) + a **BootReceiver** that reschedules after reboot, falling back to cache when Firestore is unreachable. |
| Real-time updates? | **Firestore snapshot listeners** — when a member wakes up, every phone updates within a second. |
| How is the data secured? | **Firestore security rules** — users can only read/write their own profile; tribe members only their own member docs; proposals only by members. |
| Design pattern used? | **MVVM** with Hilt DI. ViewModels hold StateFlow state; Compose screens are stateless and only render. |
| Why not a relational database? | Firestore gives **real-time listeners + server rules + zero server infra**. Perfect for live social sync. |
| What is your most proud feature? | The proposal/veto democracy loop and the offline-first alarm that survives force-kills and reboots. |
| What would you improve? | Server-side expiry via Cloud Functions, push notifications for members, Room instead of prefs if the cache grows. |
| How do you handle errors? | Retries with backoff, timeouts, and **graceful fallbacks** (e.g. trust the auth session if Firestore is down). |
| What did you learn? | System-level Android (alarm permissions, boot receivers), real-time architecture, and shipping a signed release build. |

## 4. Core Android (very likely)

**Q: Activity lifecycle.** onCreate → onStart → onResume → onPause → onStop → onDestroy. onResume = app visible & interactive; onPause = about to lose focus; onSaveInstanceState for rotation.

**Q: What is a BroadcastReceiver?** A component that reacts to system-wide events (boot, network change). Stateless, ~10s budget → use `goAsync()` for async work. In Meluko: `BootReceiver`, `AlarmReceiver`.

**Q: What is AlarmManager?** The system service for scheduling alarms. `setExactAndAllowWhileIdle` fires even in Doze. Needs `SCHEDULE_EXACT_ALARM` (API 31+).

**Q: Service vs BroadcastReceiver?** A Service runs background work (long tasks); a Receiver is a short, event-triggered handler. AlarmManager fires a receiver, not a service.

**Q: ViewModel?** Survives configuration changes, holds UI state, lives as long as the screen's lifecycle. Created via `hiltViewModel()`.

**Q: Coroutines?** Kotlin's way to do async without callbacks. `viewModelScope.launch { }`, `suspend` functions, `async/await` for parallel work. In Meluko, auth reads two Firestore docs in parallel.

**Q: StateFlow?** An observable state holder. Screens `collect` it and recompose when it changes.

**Q: Compose vs XML Views?** Compose is declarative — UI described in Kotlin, recomposes when state changes. XML is imperative with findViewById. Meluko uses 100% Compose.

**Q: Single Activity architecture?** One Activity, navigation via NavHost. Simpler back stack, no fragment transactions.

## 5. Databases (very likely)

**Q: SQL vs NoSQL difference?** SQL = tables, fixed schema, JOINs, ACID transactions. NoSQL (Firestore) = documents/collections, flexible schema, horizontal scale, real-time listeners.

**Q: What is a primary key / foreign key?** PK uniquely identifies a row; FK references a row in another table.

**Q: Normalization?** Removing redundancy by splitting tables. But Meluko deliberately *denormalizes* (tribe time + followed time) for reliability.

**Q: CRUD?** Create, Read, Update, Delete.

**Q: Index?** A structure that speeds up lookups at the cost of write speed and storage.

**Q: Firestore security rules?** Server-side rules deciding who can read/write each document. Better than trusting the client.

**Q: What is a transaction/batch?** A group of operations that succeed or fail together atomically. Meluko uses a batch to accept a proposal (updates 3 docs at once).

## 6. OOP + Java/Kotlin basics (very likely)

**Q: Four pillars of OOP.**
- **Encapsulation** — hide data, expose methods (private fields + getters).
- **Inheritance** — a class reuses/extends another.
- **Polymorphism** — same method name, different behavior (override; interfaces).
- **Abstraction** — hide implementation detail (interfaces, abstract classes).

**Q: Overloading vs overriding?** Overloading = same name, different params (compile-time). Overriding = subclass replaces a parent method (runtime).

**Q: Interface vs abstract class?** Interface = contract, can have multiple; abstract class = partial implementation, single inheritance.

**Q: What is a class vs object?** Class = blueprint; object = instance. (In Kotlin, `object` is also a singleton.)

**Q: static keyword?** Belongs to the class, not instances — one shared copy.

**Q: List vs Set vs Map?** List = ordered, duplicates allowed. Set = no duplicates. Map = key→value pairs.

**Q: What is a HashMap?** Key-value store with O(1) average lookups using hashing.

**Q: Stack vs Queue?** Stack = LIFO (last in, first out). Queue = FIFO.

**Q: String vs StringBuilder?** String is immutable — every concat creates a new object. StringBuilder mutates in place (faster for many concats).

**Q: Exception handling?** try → catch → finally. Kotlin has no checked exceptions; `suspend` functions throw normally.

**Q: What is Kotlin null safety?** Types can't be null by default; `?` marks nullable, `?:` default, `!!` force (avoid), `?.` safe-call.

## 7. Coding round patterns (if any)

- Two-pointer, sliding window, hash map counting, string reversal, palindrome, Fibonacci, factorial, prime check.
- Arrays: find max/min, second largest, duplicates.
- Strings: anagram check, palindrome, reverse words.
- Number: reverse digits, Armstrong, even/odd, swap without temp.

## 8. Behavioral (very likely at TCS)

- **Tell me about yourself** → section 1.
- **Strengths / weaknesses** → "Strength: I learn by building and I finish things — Meluko went from idea to a signed release APK. Weakness: I used to spend too long perfecting details; I've learned to ship the core first and iterate."
- **Team conflict scenario** → "I listen first, separate the person from the problem, and look for the best technical answer rather than winning."
- **Why TCS?** → "A place with structured learning, global projects, and real scale. I want to grow from a strong Android/Kotlin engineer into a full-stack engineer."
- **Where do you see yourself in 5 years?** → "Leading a small team while staying hands-on with architecture."
- **Why should we hire you?** → "I don't just learn — I ship. You get a self-driven engineer who's already built and released a production app end to end."

## 9. Questions to ask THEM (shows interest)

1. "What does the first 90 days look like for a fresher on a project?"
2. "What technologies are most common in your current projects?"
3. "How is learning and mentorship structured at TCS?"

---

## The 5 most important things to rehearse TONIGHT

1. **The 30-second project pitch** (section 2) — word perfect.
2. **OOP four pillars** + overloading/overriding + interface/abstract.
3. **SQL vs NoSQL** and why Firestore for real-time.
4. **Activity lifecycle** + ViewModel + BroadcastReceiver + AlarmManager.
5. **Introduction** (section 1) — confident, 60 seconds.
