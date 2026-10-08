# Lesson 12 — Numbers Table: Facts to Say Cold

Recite these until they come out automatically. Interviewers love concrete numbers.

## The app

| Fact | Value |
|---|---|
| Name | Meluko |
| Package | `com.alarm.meluko` |
| Version | 1.0 (versionCode 1) |
| Release APK size | ~4.3 MB (after R8) |
| GitHub | `github.com/sltrtn/Meluko` |

## The stack

| Fact | Value |
|---|---|
| Language | Kotlin 2.2.10 |
| UI | Jetpack Compose, Material 3, BOM 2026.05.01 |
| Build | AGP 9.2.1, Gradle 9.4.1 |
| DI | Dagger Hilt 2.59.2 + KSP |
| Navigation | Compose Navigation 2.9.0 |
| Backend | Firebase BOM 33.15.0 (Auth, Firestore, Storage, Crashlytics) |
| Auth | Credential Manager 1.5.0 + Google ID |
| Images | Coil 2.7.0 |
| Min SDK / Target | 24 / 36 |

## Time and proposal math

| Fact | Value |
|---|---|
| Canonical alarm time format | `"HH:MM"` 24-hour string |
| Veto window | 2 hours (`expiresAt = createdAt + 2h`) |
| Vetoes to kill a proposal | 1 |
| Auth read timeout | 10,000 ms |
| Auth retries | 2, backoff 500ms / 1000ms |
| Sign-in exchange timeout | 15,000 ms |
| Streak rule | `lastWakeDate == yesterday` → +1, else reset to 1 |
| Alarm request code | 1001 (scheduler), 1002 (activity pending intent) |
| Notification ID | 101 |
| Notification channel | `alarm_channel`, IMPORTANCE_HIGH |

## Alarm precision

| Fact | Value |
|---|---|
| Scheduling call | `setExactAndAllowWhileIdle(RTC_WAKEUP, ...)` |
| Fallback when permission denied | `alarmManager.set(...)` (inexact) |
| Permissions | `SCHEDULE_EXACT_ALARM` (API 31+), `USE_EXACT_ALARM` (API 33+) |
| Vibration pattern | `[0, 800ms, 600ms]` repeat |
| Ringtone | Default alarm tone, `isLooping = true` on API 28+ |

## Firebase (registered)

| Fact | Value |
|---|---|
| OAuth web client ID | `1049841099865-q6ube9e1obc4b8vslu5dekqlb776k052.apps.googleusercontent.com` |
| Release SHA-1 | `D7:0D:CC:74:E2:19:0A:41:69:6A:98:BC:E1:06:27:24:DA:20:CC:C7` |
| Release SHA-256 | `29:7F:5D:CA:69:51:F9:59:E6:1F:35:A6:61:0C:E2:3D:B0:B2:4E:30:6A:F1:20:EF:95:4D:96:D4:F5:E9:46:CF` |
| Keystore alias | `meluko` |
| Keystore password | env `KEYSTORE_PASSWORD` / `KEY_PASSWORD` (fallback `meluko-secure-2026`) |

## Design

| Fact | Value |
|---|---|
| Seed color | Sunrise Amber `#FF8C00` |
| Dark base | `#0F0F12` |
| Font | Outfit (9 weights) |
| Card rounding | 20–24dp, 0dp elevation |
| Button height / rounding | 56dp / 20dp |
| Sheet top rounding | 32dp, no drag handle |
| pressScale target (default) | 0.94f (chip 0.92f, FAB 0.90f) |

## Data model

| Fact | Value |
|---|---|
| Top-level collections | `users`, `user_alarms`, `tribes` |
| Subcollections | `tribes/{id}/members`, `tribes/{id}/proposals` |
| Member statuses | `"awake"` / `"missed"` / `"sleeping"` |
| Proposal statuses | `"pending"` / `"accepted"` / `"vetoed"` |
| Member sort | `streakDays` descending |

## Typical answer patterns

- "It's a ~4MB release APK thanks to R8 minification + resource shrinking."
- "Auth checks two docs in parallel with a 10s timeout and 2 retries; if Firestore is unreachable, we trust the Firebase session and assume returning user."
- "The alarm uses `setExactAndAllowWhileIdle` so it fires through Doze; if the exact-alarm permission is missing on Android 12+, it falls back to inexact so it still fires."
- "A proposal lives for 2 hours; one veto kills it; otherwise the listener auto-accepts with an atomic batch that updates the proposal status, the tribe time, and your followed time together."
- "Streaks are consecutive-day wakes: yesterday's wake continues the streak, anything older resets it to 1."

## Self-check

1. How long is the veto window?
2. Which two permissions matter for exact alarms, and on which API levels?
3. What's the streak rule in one sentence?
4. What's the release APK size and how was it achieved?
5. What are the three statuses a member can have today?
