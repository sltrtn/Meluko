# Meluko

**Sleep together. (Not literally.) Wake up together.**

Meluko is a social alarm clock for Android. You join a "tribe" — your hostel flatmates, gym crew, whoever — and you all wake up to the same alarm. If you sleep through it, your whole tribe knows. No snooze. No lying.

## Features

- **Tribes** — Create or join a group with a simple invite code
- **Shared Alarms** — Propose alarm times, tribe members can veto within 2 hours
- **Wake Tracking** — Streaks, ranks, and awake/sleeping statuses visible to your tribe
- **Swipe to Wake** — Custom swipe gesture to dismiss your alarm (no accidental snoozes)
- **Material 3 Design** — Warm sunrise amber palette, buttery spring animations, dark mode
- **Offline Support** — Alarm fires even without internet; cached data for reliability
- **Boot Resilient** — Alarm reschedules automatically after device restart

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Backend:** Firebase Auth, Firestore, Storage
- **Auth:** Android Credential Manager (Google Sign-In)
- **DI:** Dagger Hilt
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 36

## Download

Grab the latest release APK from [Releases](../../releases). Install it on your Android device (enable "Install unknown apps" if prompted).

## Building from Source

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires release-key.jks)
./gradlew assembleRelease
```

The release APK will be at `app/build/outputs/apk/release/app-release.apk`.

## Permissions

| Permission | Why |
|---|---|
| `SCHEDULE_EXACT_ALARM` | Fire alarm at exact time |
| `USE_EXACT_ALARM` | Auto-granted exact alarm on Android 13+ |
| `RECEIVE_BOOT_COMPLETED` | Reschedule alarm after restart |
| `WAKE_LOCK` | Keep CPU alive when alarm fires |
| `POST_NOTIFICATIONS` | Show alarm notifications |
| `INTERNET` | Sync tribe data via Firestore |

## License

Private — all rights reserved.
