# Handoff — Session End

> **Last updated:** 2026-08-27
> **Last action:** Full production audit + fixes (security, reliability, architecture)

---

## Current Goal

Move Meluko from "v1.0 demo" to "production-shippable foundation". The core product is complete; remaining work is deployment, monitoring, and scaling infrastructure.

---

## Completed Work (this session)

### Critical functional fixes
1. **`SetFirstAlarmScreen` 12h→24h conversion** — first alarm after onboarding now fires correctly.
2. **Exact-alarm permission flow** — HomeScreen shows a settings banner on Android 12+ when `canScheduleExactAlarms()` is false.
3. **Auth race fix** — `AuthViewModel.syncFirestoreAfterAuth` now writes the user doc before reading, avoiding misclassification of returning users as new.
4. **Member count race fix** — `OnboardingViewModel.joinTribe()` uses a Firestore transaction + atomic `FieldValue.increment(1)`.
5. **Create tribe atomicity** — `createTribe()` uses a batch for tribe/member/user_alarms.
6. **Missed status + daily reset** — `AlarmReceiver` resets own status to sleeping at fire time; `MissedAlarmReceiver` marks sleeping users as missed after a 10-minute grace period.
7. **Double-dismiss race fix** — `AlarmActivity.handleDismiss()` uses a Firestore transaction.
8. **Boot receiver auth restore** — waits for `FirebaseAuth` state with a 5s timeout before falling back to cache.

### Security & release hygiene
9. **Firestore rules tightened** — proposal status transitions validated, proposer matching, member create self-only with exists guard.
10. **Secret rotation (local)** —
    - `app/google-services.json` removed from git tracking (still in history; rotate Firebase API key in Console).
    - New `app/release-key.jks` generated with password `meluko-secure-2026`.
    - `.gitignore` fixed for `google-services.json` and `*.jks`.
    - `build.gradle.kts` no longer commits fallback passwords; env vars required for real releases.
11. **New release fingerprints** (register in Firebase Console):
    - SHA-1: `AD:33:B3:9B:9E:69:BF:AD:5E:5E:30:12:81:C6:02:34:A4:44:C1:8B`
    - SHA-256: `05:63:6D:87:7C:C7:F0:F0:12:13:FE:9E:79:17:8C:31:B5:F3:DA:E1:93:B8:4A:01:EF:2C:B7:96:75:87:0D:BD`

### Architecture & code quality
12. **Hilt DI consistency** — all ViewModels now inject `FirebaseAuth`, `FirebaseFirestore`, `FirebaseStorage` instead of calling `getInstance()`.
13. **Firestore string literals moved to constants** — added `ROLE`, `ACTIVE`, `DESCRIPTION`, `ALARM_DAYS`, `LAST_WAKE_DATE`, `NOTIFICATIONS_ENABLED`, status enums, role enums.
14. **Rank recomputation** — `SocialViewModel` recomputes and persists rank from sorted streaks.
15. **TribeDetail listener clobber fixed** — `loadMembersForTribe` skips re-attaching if already listening to the same tribe.
16. **Dead code removed** — `SafeNavigation.kt`, `UiState.kt`, `HomeViewModel.dismissAlarm/resetAlarm`, `HomeScreen.isAlarmRinging`, `AccountScreen.showToneMissing`.
17. **Emojis removed from UI** — complies with `AGENTS.md` no-emoji rule.
18. **Theme parent changed** to dark Material theme.
19. **Lint cleanup** — redundant activity label removed, `NoCredentialException` handled.

### Server-side reliability
20. **Firebase Cloud Functions added** under `functions/`:
    - `onProposalCreated` / `onProposalUpdated` — server-side auto-accept of expired proposals, updating **all** tribe members' `followedAlarmTime`.
    - `joinTribeByInviteCode` — server-side invite-code validation and member creation.
    - Deploy with `firebase deploy --only functions`.

### Testing
21. **Unit tests added**:
    - `TimeUtilsTest` — 12h→24h conversion.
    - `StreakCalculatorTest` — streak math.
22. **Shared utilities extracted**:
    - `TimeUtils.to24HourTime()`
    - `StreakCalculator.computeNewStreak()`

### Documentation
23. **Full `Ref/` interview-prep package** created mirroring ContextIQ format.
24. **TCS Prime quick-sheet** added and pushed.

---

## Build status

- `./gradlew assembleDebug` ✅
- `./gradlew assembleRelease` ✅ (with `KEYSTORE_PASSWORD` / `KEY_PASSWORD` env vars)
- `./gradlew test` ✅
- `./gradlew lintDebug` ✅ (warnings remain for outdated AGP/Gradle/targetSdk versions)

---

## Important manual follow-ups required

1. **Firebase Console:**
   - Rotate the exposed API key / OAuth client from the committed `google-services.json`.
   - Download a fresh `google-services.json` and place it at `app/google-services.json` (do not commit).
   - Register the new release SHA-1/SHA-256 fingerprints.
2. **Deploy Cloud Functions:** `cd functions && npm install && firebase deploy --only functions`.
3. **Wire client to Cloud Function join** (optional but recommended): replace `OnboardingViewModel.joinTribe()` client-side write with a callable to `joinTribeByInviteCode`.
4. **Real release:** set `KEYSTORE_PASSWORD` / `KEY_PASSWORD` env vars and build with a new, secret keystore password.
5. **Consider:** FCM push notifications, account deletion, leave tribe, Play Store privacy policy.

---

## Known remaining gaps (acceptable for v1 demo, not blockers)

- Outdated dependency versions (AGP 9.2.1, Gradle 9.4.1, targetSdk 36 — lint warns).
- `BootReceiver` still uses `FirebaseFirestore.getInstance()` directly (BroadcastReceiver).
- Cloud Function join path not yet used by the Android client.
- No runtime battery-optimization whitelist prompt.
- No FCM push notifications.
- No account deletion flow.
- No automated CI/CD.

---

## Files changed (high-level)

- `app/build.gradle.kts`
- `app/.gitignore`, `.gitignore`
- `app/src/main/AndroidManifest.xml`
- `firestore.rules`
- `app/src/main/java/com/alarm/meluko/auth/AuthViewModel.kt`
- `app/src/main/java/com/alarm/meluko/alarm/{AlarmScheduler,AlarmReceiver,AlarmActivity,BootReceiver,MissedAlarmReceiver}.kt`
- `app/src/main/java/com/alarm/meluko/data/{FirestoreConstants,AlarmCache}.kt`
- `app/src/main/java/com/alarm/meluko/di/FirebaseModule.kt`
- `app/src/main/java/com/alarm/meluko/ui/{HomeViewModel,HomeScreen,SocialViewModel,OnboardingViewModel,ProposalViewModel,AccountViewModel,SetFirstAlarmScreen,ProposeAlarmBottomSheet,AccountScreen,SocialScreen,TribeDetailScreen,OnboardingScreen}.kt`
- `app/src/main/java/com/alarm/meluko/util/TimeUtils.kt`
- `app/src/main/java/com/alarm/meluko/alarm/StreakCalculator.kt`
- `app/src/main/res/values/themes.xml`
- New: `functions/package.json`, `functions/index.js`, `functions/.gitignore`
- New tests: `app/src/test/java/com/alarm/meluko/util/TimeUtilsTest.kt`, `app/src/test/java/com/alarm/meluko/alarm/StreakCalculatorTest.kt`
- Deleted: `app/src/main/java/com/alarm/meluko/navigation/SafeNavigation.kt`, `app/src/main/java/com/alarm/meluko/ui/UiState.kt`
- Docs: `Ref/Meluko_Master.md`, `Ref/interview/*.md`, `Ref/interview/quick-sheet-tcs.md`
