# Current Task

> Always represents exactly what is currently being worked on.

---

## Objective

Move Meluko from v1.0 demo to production-shippable foundation. Core product is complete; focus is now deployment, secret rotation, Cloud Functions, and monitoring.

---

## Status

✅ Full production audit completed.
✅ All critical/high issues from audit fixed.
✅ Debug + release builds pass; unit tests pass; lint passes.

---

## Steps

- [x] Add Firebase Crashlytics
- [x] Create BootReceiver for alarm reschedule on reboot
- [x] Strip alarm screen to system alarm aesthetic
- [x] Fix SwipeToWakeButton gesture
- [x] Add shimmer loading states
- [x] Migrate fonts to Outfit
- [x] Centralize MotionSpec
- [x] Add error handling (UiState, snackbar errors)
- [x] Add alarm tone picker (system ringtone)
- [x] Alarm reliability (reschedule on fire, notification channel)
- [x] Tribe system polish — 13 fixes
- [x] Fix `SetFirstAlarmScreen` 12h→24h bug
- [x] Add exact-alarm permission settings banner
- [x] Fix AuthViewModel read-before-write race
- [x] Fix memberCount race with transaction + increment
- [x] Tighten Firestore security rules
- [x] Rotate secrets (new keystore, remove google-services.json from tracking)
- [x] Implement missed status + daily reset
- [x] Implement rank recomputation
- [x] Create Firebase Cloud Functions (auto-accept + join-by-code)
- [x] Migrate all ViewModels to Hilt-injected Firebase
- [x] Move string literals to FirestoreConstants
- [x] Fix BootReceiver auth restore race
- [x] Fix AlarmActivity double-dismiss race with transaction
- [x] Fix TribeDetail replacing Social listener
- [x] Remove dead code (SafeNavigation, UiState, unused states)
- [x] Remove emojis from UI
- [x] Add unit tests (TimeUtils, StreakCalculator)
- [x] Fix lint issues

---

## Next Immediate Step

1. Rotate Firebase API key / OAuth client in Firebase Console and download fresh `google-services.json`.
2. Register new release SHA fingerprints in Firebase Console.
3. Deploy Cloud Functions: `cd functions && npm install && firebase deploy --only functions`.
4. Set real `KEYSTORE_PASSWORD` / `KEY_PASSWORD` env vars and build release.
