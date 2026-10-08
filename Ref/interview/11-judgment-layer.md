# Lesson 11 — Judgment Layer: Decisions, Traps, and the Pitch

Files: `.ai/decisions.md`, this series

## What this lesson covers

- The one-line pitch and the 30-second story
- The decisions you made and can defend
- Traps interviewers set and the answers that disarm them
- What to say when you're stumped

## The one-line pitch

> "Meluko is a social alarm clock — you join a tribe, everyone wakes to the same alarm, and if you snooze, the whole tribe sees it."

## The 30-second story

> "I built Meluko because individual willpower fails at 7am — a louder alarm just makes the problem more annoying. The insight is that social accountability scales better than willpower: when your wake-up streak is visible to your friend group, failing becomes embarrassing. So the app is built around tribes, shared alarm times, and a proposal/veto vote for changing them. The hard engineering was system-level Android: exact alarms that fire during Doze, a full-screen lock-screen alarm, rescheduling that survives force-kills, and a reboot receiver that works offline. Plus Firestore real-time sync so the whole tribe sees who woke up — live."

## Decisions you can defend

| Decision | Defense |
|---|---|
| **24-hour `"HH:MM"` canonical time** | No AM/PM ambiguity in the data layer. Display converts to 12h at the edge. |
| **Reschedule-on-fire, not on-dismiss** | Dismiss requires a live user; fire happens in a system-launched receiver. The only point that can't be skipped. |
| **BootReceiver + local cache** | Alarm receivers run without guaranteed network. The cache is a tiny dependency-free fallback. |
| **Google-only auth (Credential Manager)** | No password infrastructure, no reset flows, identity handled by Google. |
| **Client-side auto-accept with `isAccepting` guard** | A listener crossing the expiry deadline commits an idempotent batch — no server code needed at this scale. |
| **Firestore listeners over one-shot reads** | The whole product is real-time social sync; listeners make it live and keep one code path. |
| **Parallel reads + retries + timeouts in auth** | First-run is the highest-failure moment; a session check that hangs forever is a broken door. |
| **Batches + `arrayUnion`/`increment`** | Atomicity and race-safety for votes and streaks without read-modify-write. |
| **Single Activity + Compose Navigation** | One back stack, auth-driven routing, smooth transitions. |
| **Spring animations, not tweens** | Physical motion reads as "alive"; named specs keep the feel consistent. |
| **R8 + mapping upload** | Production size and readable crash traces. |
| **Streak = consecutive-day wakes** | `lastWakeDate == yesterday` → +1, else reset. Simple, honest, motivating. |
| **`goAsync()` + greedy parsing in receivers** | The boot path must never throw and never outlive its time budget. |

## Traps and the answers

**"This is just an alarm clock app. What's hard about that?"**
> "The wake-up alarm is the one feature that can't be gracefully degraded. It must fire at the exact second through Doze, over the lock screen, even if the phone rebooted or the user is offline. That's exact-alarm permissions, full-screen intents, boot receivers with a local cache fallback, and rescheduling logic that survives force-kills. A chat app can show 'offline' — an alarm can't."

**"Why not use a Cloud Function to auto-accept proposals?"**
> "For this scale, a client listener that commits an idempotent batch is free and near-real-time. The status update makes it race-safe. A function would add a deploy step and server billing for something the client handles deterministically."

**"What if two users veto at the exact same time?"**
> "The veto uses `FieldValue.increment(1)` and `arrayUnion(uid)` — both atomic server-side operations. No read-modify-write, so two simultaneous vetoes can't clobber each other, and a single veto flips status to 'vetoed' regardless."

**"How do you prevent one person waking up and syncing fake streak data?"**
> "The security rules gate every write: member docs are self-only, proposals are member-only, and `todayStatus == 'awake'` guards double-counting. The honest answer is the app trusts the client's single write per day — the social pressure is the anti-cheat."

**"Your tribe time is stored twice — tribe.alarmTime and user_alarms.followedAlarmTime. Isn't that redundant?"**
> "It's deliberate denormalization. `followedAlarmTime` is what this specific device schedules; it survives mid-transition states and tribe-doc changes. A listener keeps them in sync, and the redundancy is the source of resilience."

**"Why SharedPreferences instead of Room for the cache?"**
> "The cache stores exactly three tiny values that must be readable synchronously from a broadcast receiver with zero setup. Room would be over-engineering for that. If the cache grew to queries and relationships, I'd move to Room."

**"What happens if Firestore is down when someone opens the app?"**
> "Auth state comes from Firebase Auth first. If Firestore reads fail after retries, we trust the auth session, assume returning user, and let them in. HomeViewModel already rendered the cached alarm time and shows the offline banner. The worst case is redoing onboarding once — never a stuck spinner."

## When you're stumped

1. **Repeat the question** in your own words — buys time and confirms understanding.
2. **State what you know for sure** — the data flow, the file, the invariants.
3. **Reason out loud** about the tradeoff, even if you don't know the answer yet.
4. **"Here's how I'd find out"** — search the codebase, read the docs, write a test. Interviewers want the process, not just the answer.

## Self-check

1. Deliver the one-line pitch without notes.
2. Defend reschedule-on-fire in one paragraph.
3. Explain the redundant-alarm-time denormalization.
4. What's the honest anti-cheat answer for streaks?
5. Give the "alarm app isn't easy" rebuttal.
