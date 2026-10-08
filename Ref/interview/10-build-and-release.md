# Lesson 10 — Build and Release: Shipping a Signed APK

Files: `app/build.gradle.kts`, `app/proguard-rules.pro`, `app/release-key.jks`

## What this lesson covers

- The version catalog and plugin setup
- Release signing — keystore, alias, password strategy
- R8 minification + resource shrinking + ProGuard rules
- Crashlytics mapping upload
- Firebase app registration and the SHA fingerprint gotcha

## The build configuration

`app/build.gradle.kts`:

| Setting | Value | Why |
|---|---|---|
| `compileSdk` | 36 (minorApiLevel 1) | Latest platform |
| `minSdk` | 24 | Android 7.0+ coverage |
| `targetSdk` | 36 | Latest behavior rules |
| `applicationId` | `com.alarm.meluko` | Unique app identity |
| `versionCode` | 1, `versionName` | "1.0" | Release versioning |
| Namespace | `com.alarm.meluko` | Kotlin DSL namespacing |

Plugins: Android application, Kotlin Compose, KSP (for Hilt), Hilt, Google Services (Firebase), Crashlytics.

## Release signing — the part you must never lose

```kotlin
signingConfigs {
    create("release") {
        storeFile = file("release-key.jks")
        storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "meluko-secure-2026"
        keyAlias = "meluko"
        keyPassword = System.getenv("KEY_PASSWORD") ?: "meluko-secure-2026"
    }
}

buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        signingConfig = signingConfigs.getByName("release")
    }
}
```

### Why signing matters
An APK is signed with a cryptographic key. Android uses that signature to verify the app is genuine and that updates come from the same author. The **keystore is the one thing you can never lose** — lose it and you can never update the app under the same identity (you'd have to publish a fresh app with a new package name).

### Password strategy
- `storeFile` — `release-key.jks` (gitignored via `*.jks`).
- Passwords default to `meluko-secure-2026` **only as a fallback** — the real convention is environment variables `KEYSTORE_PASSWORD` / `KEY_PASSWORD`. That keeps secrets out of the repo while still building locally.
- Aliases identify keys inside the keystore; there's exactly one key here, `meluko`.

### The Firebase gotcha — SHA fingerprints
Firebase Auth's Google Sign-In validates the app by its **SHA-1/SHA-256 certificate fingerprint**. Debug and release builds have different signatures, so a debug-signed APK can sign in while a release APK fails with "This app is not authorized."

The release fingerprints are registered in Firebase Console → Project settings → Your apps → add fingerprint:

- SHA-1: `D7:0D:CC:74:E2:19:0A:41:69:6A:98:BC:E1:06:27:24:DA:20:CC:C7`
- SHA-256: `29:7F:5D:CA:69:51:F9:59:E6:1F:35:A6:61:0C:E2:3D:B0:B2:4E:30:6A:F1:20:EF:95:4D:96:D4:F5:E9:46:CF`

## R8 and ProGuard — the production-size APK

`isMinifyEnabled = true` turns on R8, which does three jobs:

1. **Shrinking** — removes unused code and classes.
2. **Obfuscation** — renames classes/methods to short names (harder to reverse-engineer).
3. **Optimization** — inlines and removes dead branches.

`isShrinkResources = true` additionally strips unused resources. Together they take the debug APK down to a ~4.3 MB release APK.

### Why keep rules are needed
Obfuscation is dangerous for reflection-based libraries: Firebase, Hilt, and Coil look up classes by name at runtime. If R8 renames them, the app crashes. `app/proguard-rules.pro` lists keep rules:

```proguard
# Firebase — keep all classes referenced by reflection
-keep class com.google.firebase.** { *; }

# Hilt — generated components
-keep class dagger.hilt.** { *; }

# Kotlin coroutines
-keep class kotlinx.coroutines.** { *; }

# Coil
-keep class coil.** { *; }
```

Plus the two rules that keep crash traces readable:

```proguard
-keepattributes SourceFile, LineNumberTable
```

Line numbers mean a stack trace points at the exact line of the crash — without them, the trace is just class names and the `-renameSourceFileAttribute` even lets you hide the file names.

## Crashlytics mapping upload

The Crashlytics Gradle plugin reads the mapping file R8 produces and uploads it on release builds (`uploadCrashlyticsMappingFileRelease`). Crashlytics de-obfuscates stack traces against it, so you see `HomeViewModel.kt:87` instead of `com.alarm.meluko.a.b.c`. Without the upload, release crashes are nearly unreadable.

The plugin also stamps a `google-services.json` config so the crash reports route to the right Firebase project.

## The release commands

```bash
./gradlew assembleDebug     # dev builds, no minify
./gradlew assembleRelease   # signed, minified, mapped
./gradlew bundleRelease     # Play Store AAB (if you ever publish)
```

Output:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

## Common trap

**"Why does Google Sign-In work on a debug build but fail on the release build?"**

Strong answer: the debug and release keystores produce different certificate fingerprints, and Firebase only trusts the fingerprints you register. `google-services.json` contains the debug SHA; the release SHA must be added in the Firebase Console. This is the single most common "works in dev, broken in release" bug — and why the release fingerprints are registered (Lesson's key fact).

## Self-check

1. Why can't you lose the release keystore?
2. What do shrinking, obfuscation, and optimization each do?
3. Why would R8 break Firebase without keep rules?
4. What does the mapping file upload do for a crash trace?
5. Why does release Google Sign-In need a separate SHA fingerprint?

## Code map

| Concept | Location |
|---|---|
| Signing + R8 config | `app/build.gradle.kts` lines 28–50 |
| Keep rules | `app/proguard-rules.pro` |
| Firebase config | `app/google-services.json` (don't modify) |
| Release SHA-1/SHA-256 | Firebase Console → app settings |
