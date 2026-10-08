# Lesson 1 — Mental Model: What is a social alarm clock and why does it exist?

## The problem Meluko solves

Waking up is a willpower problem. Most people snooze 2–5 times, and individual willpower fails reliably when it's 7am and the bed is warm.

The naive fix — a louder alarm, a puzzle alarm, an app that makes you scan a barcode — treats the symptom. It raises the cost of staying in bed, but the cost resets every morning.

Meluko's insight: **accountability scales better than willpower.** When your alarm failure is visible to your friend group, you're not fighting yourself — you're fighting your social identity. Failing is embarrassing, and embarrassment is a stronger motivator than a ringtone.

**The one-liner:** you can't snooze a tribe.

## The social accountability loop

1. **A tribe shares an alarm time.** Join via invite code; the tribe has one shared wake-up target.
2. **Anyone can propose a new time.** Democracy: propose, wait 2 hours, and if nobody vetoes, the alarm moves.
3. **Failure is public.** Your status (Awake / Sleeping / Missed) is visible to every member, live.
4. **Success compounds.** Consecutive wakes build a streak, ranked on a leaderboard.

The loop closes: propose → veto-or-accept → everyone follows → wake → visible status → streak → rank. Every screen in the app sits on one arrow of this loop.

## The core building blocks

### 1. Firebase Auth (Google Sign-In)
- Identity without building password infrastructure.
- Modern flow: Android Credential Manager shows a Google account sheet; you get an ID token; exchange it with Firebase for a signed-in user.
- Session persists across launches — `FirebaseAuth.currentUser` restores automatically.

### 2. Firestore (real-time database)
- A NoSQL document database with live listeners.
- When any user writes a document, every device listening on that document gets the update in real time.
- This is what makes "your tribe sees you miss the alarm" work without polling.
- Security is enforced server-side via `firestore.rules` — a compromised client still can't read other users' data.

### 3. AlarmManager (Android system service)
- The OS-level alarm clock. Can wake the device from Doze and fire a broadcast even when the app is dead.
- Needs exact-alarm permission on Android 12+.
- `setExactAndAllowWhileIdle` fires at the precise second — critical for a wake-up alarm.

### 4. BroadcastReceiver + full-screen intent
- The alarm fires a `BroadcastReceiver`, which posts a high-priority notification.
- The notification carries a full-screen intent that launches `AlarmActivity` over the lock screen — screen on, ringtone looping, vibration.

## The two phases

### SETUP (one time per user)

```
Sign in with Google
  → Check if existing session (Firestore data)
  → Onboarding → create profile
  → Create or join a tribe
  → Set the first alarm time
  → AlarmScheduler schedules it
```

### DAILY LOOP (every day)

```
Proposal in flight?
  → Veto or let it auto-accept (2h window)
Tribe alarm time updates
  → HomeViewModel listener → followedAlarmTime
  → AlarmScheduler schedules for next occurrence
Alarm fires
  → AlarmReceiver: reschedule tomorrow + full-screen notification
  → AlarmActivity: ring + vibrate over lock screen
  → User swipes to wake
  → Firestore: todayStatus=awake, streak+1, totalWakes+1
  → Tribe sees your status live
```

## Why this approach and not the alternatives?

### Why not just a regular alarm app?
A regular alarm is a personal device — you can ignore it privately. Meluko makes wake-up a public commitment. The visible failure is the product.

### Why not email/password auth?
Google auth means no password reset flows, no hash storage, no account compromise from reused passwords. Google handles identity; you focus on the app.

### Why Firestore and not a custom backend?
Real-time listeners out of the box, server-side security rules, zero infrastructure to run, free tier that covers a beta. A custom backend would need WebSockets, auth, and a database — all of which Firestore gives you with a client SDK call.

### Why AlarmManager and not WorkManager?
WorkManager is for deferrable background work and does **not** guarantee exact fire times. A wake-up alarm must fire at the exact minute — that's AlarmManager's job.

### Why cache the alarm locally?
Alarm receivers and boot receivers run with no guarantee of network access. The alarm time is saved to SharedPreferences so the alarm survives offline reboots.

## Your interview opener

> "Most alarm apps treat waking up as a personal willpower problem and make the alarm louder. I built Meluko to treat it as a social problem — you join a tribe, share an alarm, propose new times with a veto window, and your wake streak is visible to everyone. The hard part was system-level Android: exact alarms, boot receivers, full-screen lock-screen alarms, and making it all survive offline."

## Self-check

1. Explain Meluko to a 12-year-old in your own words.
2. Why is social accountability stronger than a louder alarm?
3. What makes Firestore's real-time listeners critical to this app?
4. Why can't WorkManager replace AlarmManager here?
5. Walk through the daily loop from "alarm fires" to "tribe sees you awake".

## Code map

| Concept | File |
|---|---|
| Auth | `auth/AuthViewModel.kt` |
| Real-time tribe data | `ui/SocialViewModel.kt` |
| Alarm scheduling | `alarm/AlarmScheduler.kt` |
| Alarm firing | `alarm/AlarmReceiver.kt` |
| Full-screen alarm | `alarm/AlarmActivity.kt` |
| Offline cache | `data/AlarmCache.kt` |
| Security rules | `firestore.rules` |
