# Current Task

> Always represents exactly what is currently being worked on.

---

## Objective

Move Meluko from v1.0 demo to a beta-ready app. Core product is complete; this pass added the remaining Play-Store-policy features (account deletion), reliability/social polish (battery prompt, leave/remove/delete tribe, invite sharing), and a UI personality pass.

---

## Status

✅ Full production audit completed and committed (was sitting uncommitted for ~6 weeks — recovered and verified before building on it).
✅ All critical/high issues from audit fixed.
✅ Beta-readiness feature set added (see roadmap "Beta-readiness pass").
✅ Firestore rules deployed to meluko-routine.
⚠️ Cloud Functions **not deployed** — meluko-routine is on the free Spark plan; deploying requires upgrading to Blaze (user's call, needs billing).
✅ Debug build + unit tests + lint pass. No adb/emulator available in this environment, so no live on-device smoke test was done this session — verify manually before a real release.

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
- [x] Account deletion flow
- [x] Battery-optimization whitelist banner
- [x] Leave tribe / admin remove-member / admin delete-tribe
- [x] Invite-code share sheet
- [x] Deploy Firestore rules to meluko-routine
- [x] Fun-factor UI pass (icon badges, streak celebration, copy)

---

## Next Immediate Step

1. Decide whether to upgrade meluko-routine to Blaze; if yes, deploy Cloud Functions: `cd functions && firebase deploy --only functions` (dependencies already installed).
2. Rotate Firebase API key / OAuth client in Firebase Console and download fresh `google-services.json`.
3. Register new release SHA fingerprints in Firebase Console.
4. Privacy policy / terms of service links.
5. Set real `KEYSTORE_PASSWORD` / `KEY_PASSWORD` env vars and build release.
