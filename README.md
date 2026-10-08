# Meluko

**Sleep together. (Not literally.) Wake up together.**

Meluko is a social alarm clock for Android. You join a "tribe" — your hostel flatmates, gym crew, whoever — and you all wake up to the same alarm. If you sleep through it, your whole tribe knows. No snooze. No lying.

## Features

- **Tribes** — Create or join a group with a simple invite code
- **Shared Alarms** — Propose alarm times, tribe members can veto within 2 hours
- **Wake Tracking** — Streaks, ranks, and awake/sleeping/missed statuses visible to your tribe
- **Swipe to Wake** — Custom swipe gesture to dismiss your alarm (no accidental snoozes)
- **Material 3 Design** — Warm sunrise amber palette, buttery spring animations, dark mode
- **Offline Support** — Alarm fires even without internet; cached data for reliability
- **Boot Resilient** — Alarm reschedules automatically after device restart
- **Missed Detection** — Users still sleeping after the grace period are marked missed

## Tech Stack

- **Language:** Kotlin 2.2.10
- **UI:** Jetpack Compose + Material 3
- **Backend:** Firebase Auth, Firestore, Storage, Cloud Functions
- **Auth:** Android Credential Manager (Google Sign-In)
- **DI:** Dagger Hilt
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 36

## Download

Grab the latest release APK from [Releases](../../releases). Install it on your Android device (enable "Install unknown apps" if prompted).

## Building from Source

### Prerequisites

1. Download `google-services.json` from your Firebase Console project and place it at `app/google-services.json`. This file is gitignored and must not be committed.
2. For release builds, generate a release keystore or use the local one and set environment variables.

### Debug build

```bash
./gradlew assembleDebug
```

### Release build

```bash
export KEYSTORE_PASSWORD=your_keystore_password
export KEY_PASSWORD=your_key_password
./gradlew assembleRelease
```

The release APK will be at `app/build/outputs/apk/release/app-release.apk`.

> **Security note:** The original `release-key.jks` and `google-services.json` that were previously committed have been rotated out. For any real release, generate a fresh keystore with a strong password and register its SHA-1/SHA-256 fingerprints in Firebase Console.

## Firebase Cloud Functions

Server-side logic lives in `functions/`:

- **Auto-accept proposals** — expires pending proposals and updates every member's alarm time.
- **Join by invite code** — validates invite codes server-side.

Deploy:

```bash
cd functions
npm install
firebase deploy --only functions
```

## Permissions

| Permission | Why |
|---|---|
| `SCHEDULE_EXACT_ALARM` | Fire alarm at exact time |
| `USE_EXACT_ALARM` | Auto-granted exact alarm on Android 13+ |
| `RECEIVE_BOOT_COMPLETED` | Reschedule alarm after restart |
| `WAKE_LOCK` | Keep CPU alive when alarm fires |
| `POST_NOTIFICATIONS` | Show alarm notifications |
| `INTERNET` | Sync tribe data via Firestore |
| `ACCESS_NETWORK_STATE` | Observe connectivity for offline banner |

## Testing

```bash
# Run unit tests
./gradlew test

# Run lint
./gradlew lintDebug
```

## License

Private — all rights reserved.
