# Lesson 6 — Alarm Scheduling: The System-Level Core

Files: `alarm/AlarmScheduler.kt`, `alarm/AlarmReceiver.kt`, `alarm/AlarmActivity.kt`

## What this lesson covers

- How `AlarmManager` schedules an exact wake-up alarm
- The Android 12+ exact-alarm permission dance
- Doze mode and why `setExactAndAllowWhileIdle` is required
- The fire flow: receiver → reschedule → full-screen notification → ringing activity
- Streak updates on dismiss

## The chain

```
HomeViewModel listener (alarmTime changed)
        │  scheduleAlarm("07:30")
        ▼
AlarmScheduler.schedule(hour, minute)
        │  nextOccurrenceMillis() → today or tomorrow 07:30
        │  setExactAndAllowWhileIdle(RTC_WAKEUP, triggerAt, pendingIntent)
        ▼
[device sleeps, Doze mode, app killed — doesn't matter]
        ▼
Android wakes at the exact time → delivers broadcast to AlarmReceiver
        ▼
AlarmReceiver.onReceive:
    1. rescheduleTomorrow()  → next day's alarm set IMMEDIATELY
    2. post full-screen notification → launches AlarmActivity over the lock screen
        ▼
AlarmActivity: screen on, ringtone looping, vibration
     → user swipes → handleDismiss() → Firestore streak update
```

## AlarmScheduler — the wrapper

`AlarmScheduler` is a thin wrapper around `AlarmManager` with one job: schedule/cancel alarms.

### Next occurrence math
```kotlin
private fun nextOccurrenceMillis(hour: Int, minute: Int): Long {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply {
        set(HOUR_OF_DAY, hour); set(MINUTE, minute)
        set(SECOND, 0); set(MILLISECOND, 0)
    }
    if (target.before(now)) target.add(DAY_OF_MONTH, 1)  // already past → tomorrow
    return target.timeInMillis
}
```
If it's 8am and you set a 7:30 alarm, it fires tomorrow, not 23.5 hours in the past.

### The precision problem and Doze
- `set()` — inexact; Android batches it. Fine for reminders, useless for wake-up.
- `setExact()` — exact, but **deferred during Doze** (the phone's battery-saver mode). A wake-up alarm could arrive minutes late.
- `setExactAndAllowWhileIdle()` — exact AND fires during Doze. This is the one for alarms.

The code uses `setExactAndAllowWhileIdle` on all API levels (24+), with a graceful fallback.

### The Android 12+ permission dance
From API 31, exact alarms need a permission. The code checks first:

```kotlin
if (Build.VERSION.SDK_INT >= S) {
    if (alarmManager.canScheduleExactAlarms()) {
        alarmManager.setExactAndAllowWhileIdle(RTC_WAKEUP, triggerAtMillis, pendingIntent)
    } else {
        alarmManager.set(RTC_WAKEUP, triggerAtMillis, pendingIntent)  // fallback
    }
} else {
    alarmManager.setExactAndAllowWhileIdle(RTC_WAKEUP, triggerAtMillis, pendingIntent)
}
```

- Manifest declares `SCHEDULE_EXACT_ALARM` (API 31–32, user-grantable) and `USE_EXACT_ALARM` (API 33+, granted automatically for alarm-clock apps).
- If permission is missing, it falls back to inexact — still fires, just possibly a few minutes off. Better late than never.
- The pending intent uses `FLAG_IMMUTABLE` — the modern requirement (mutable intents are a security hole).

### The receiver intent
The intent carries `EXTRA_ALARM_TIME` (`"07:30"`) so the receiver and activity know what time fired. `REQUEST_CODE` (1001) keeps the pending intent unique.

## AlarmReceiver — fire time

Two responsibilities, in order:

### 1. Reschedule immediately
```kotlin
rescheduleTomorrow(context, alarmTime)   // schedules the next day's alarm
```
This runs **before** anything else. Why? If the app is force-killed, the phone reboots, or the user never dismisses, the next day's alarm is already set. Rescheduling at fire time is the guarantee; the UI never needs to be involved.

### 2. Post a full-screen notification
```kotlin
val channel = NotificationChannel("alarm_channel", "Meluko Alarms", IMPORTANCE_HIGH).apply {
    setBypassDnd(true)
    lockscreenVisibility = VISIBILITY_PUBLIC
}
```

The notification:
- **Priority HIGH** — heads-up at minimum.
- **`setBypassDnd(true)`** — rings even in Do Not Disturb. (A wake-up alarm that respects DND is useless.)
- **`VISIBILITY_PUBLIC`** — content visible on the lock screen.
- **`CATEGORY_ALARM`** — tells the system this is an alarm, not a random notification.
- **`setFullScreenIntent(activityIntent, true)`** — the critical piece: it launches `AlarmActivity` **over the lock screen**.
- **`setOngoing(true)`** — can't be swiped away (you must use the app's swipe).

## AlarmActivity — the moment of truth

Launched over the lock screen:

```kotlin
setShowWhenLocked(true)   // API 27+
setTurnScreenOn(true)     // screen was off — turn it on
window.addFlags(FLAG_KEEP_SCREEN_ON)
```

- Hidden system bars (immersive), black background.
- Default alarm ringtone starts immediately and **loops** (API 28+ `isLooping = true`).
- A custom tone is loaded async from `user_alarms/{uid}/alarmToneUri` and swapped in if present.
- Vibration runs an infinite repeating waveform `[0ms, 800ms on, 600ms off]`.
- The user swipes the `SwipeToWakeButton` → `handleDismiss(alarmTime)`.

### handleDismiss — the streak write is a transaction, not a plain update
The real implementation (`AlarmActivity.handleDismiss`) wraps the write in `firebaseFirestore.runTransaction { ... }`, and delegates the actual streak math to a separate pure function, `StreakCalculator.computeNewStreak`:

```kotlin
// alarm/StreakCalculator.kt — no Android/Firebase deps, trivially unit-testable
object StreakCalculator {
    fun computeNewStreak(
        lastWakeDate: String?, today: String, yesterday: String, currentStreak: Int
    ): Pair<Int, Boolean> {
        if (lastWakeDate == today) return currentStreak to false   // already counted today
        val newStreak = if (lastWakeDate == yesterday) currentStreak + 1 else 1
        return newStreak to true
    }
}

// alarm/AlarmActivity.kt — handleDismiss
firebaseFirestore.runTransaction { transaction ->
    val memberDoc = transaction.get(memberRef)
    val currentStatus = memberDoc.getString(TODAY_STATUS) ?: "sleeping"
    if (currentStatus == "awake") {
        null // already counted today
    } else {
        val (newStreak, shouldCount) = StreakCalculator.computeNewStreak(
            lastWakeDate, today, yesterday, currentStreak
        )
        if (!shouldCount) null else {
            transaction.update(memberRef, mapOf(
                TODAY_STATUS to "awake", TODAY_WAKE_TIME to wakeTime,
                STREAK_DAYS to newStreak, BEST_STREAK to max(newStreak, currentBestStreak),
                TOTAL_WAKES to currentTotalWakes + 1, LAST_WAKE_DATE to today
            ))
            newTotalWakes
        }
    }
}
```

Streak logic: if the last wake was **yesterday**, increment; otherwise reset to 1 (you broke the chain). Two separate guards against double-counting: `computeNewStreak` returns `shouldCount = false` if `lastWakeDate == today`, and `handleDismiss` also short-circuits on `currentStatus == "awake"`.

**Why a transaction and not a plain `update`?** `runTransaction` reads the member doc and writes back inside one atomic server-side operation — if the alarm activity is re-triggered (e.g. a rapid double-tap on swipe, or the activity is recreated) and two dismiss calls race, Firestore retries the transaction so only one of them sees the pre-increment state and commits, guaranteeing a single streak increment. A plain `update` has no such read-then-write atomicity — two concurrent `update` calls could both read `streakDays = 5` and both write `6`, silently losing a day of streak. Pulling the math into `StreakCalculator` (a plain Kotlin object, no Firestore/Android types) also means the streak rule itself is unit-testable without an emulator.

Note: this write is still best-effort at the outer level — a transaction failure just logs and finishes, because the next day's alarm is already scheduled regardless (Lesson 6's core guarantee doesn't depend on this write succeeding).

### MissedAlarmReceiver — the other half of the streak story
`AlarmReceiver.onReceive` also calls `MissedAlarmReceiver.schedule(context, alarmTime)`, which arms a second, inexact-tolerant alarm 10 minutes after the main one (`GRACE_PERIOD_MINUTES = 10`). When it fires, it reads the member doc; if `todayStatus` is still `"sleeping"` (the user never swiped), it sets `todayStatus = "missed"` and **resets `streakDays` to 0**. `AlarmActivity.onDestroy` calls `MissedAlarmReceiver.cancel()` so a normal dismiss cancels the pending missed-check. This is the piece that actually enforces "no snooze" — without it, a user who never opens the app would just stay `"sleeping"` forever with their streak intact.

`onDestroy` also stops the ringtone, cancels vibration, cancels the notification, and clears `KEEP_SCREEN_ON`.

## Common trap

**"Why reschedule in the receiver instead of when the user dismisses the alarm?"**

Strong answer: dismissing requires a live user in the app. Force-kills, reboots, and missed swipes would silently kill tomorrow's alarm. Rescheduling at fire time happens in a `BroadcastReceiver` that the system launches regardless of app state — it's the only reschedule point that can't be skipped.

## Self-check

1. What's the difference between `set`, `setExact`, and `setExactAndAllowWhileIdle`?
2. What happens if exact-alarm permission is denied on Android 12+?
3. Name every attribute of the notification that makes it alarm-like (bypass DND, public visibility, full screen, category, ongoing).
4. Walk the streak logic: wake on Tuesday after waking Monday → what's the streak? What if last wake was Sunday?
5. What does `FLAG_IMMUTABLE` protect against?
6. Why is the streak write a Firestore transaction instead of a plain `update`, and why is the math pulled into `StreakCalculator`?
7. What does `MissedAlarmReceiver` do, and why does the app need it in addition to reschedule-on-fire?

## Code map

| Concept | Location |
|---|---|
| Scheduling + Doze + permissions | `alarm/AlarmScheduler.kt` lines 33–72 |
| Next-occurrence math | lines 120–133 |
| Reschedule-on-fire | `alarm/AlarmReceiver.kt` lines 32, 83–91 |
| Full-screen notification | lines 49–73 |
| Screen-on + looping ringtone | `alarm/AlarmActivity.kt` lines 52–83 |
| Streak transaction (handleDismiss) | lines 140–213 |
| Streak math (pure) | `alarm/StreakCalculator.kt` |
| Missed-alarm grace check | `alarm/MissedAlarmReceiver.kt` |
