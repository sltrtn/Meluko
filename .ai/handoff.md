# Handoff — Session End

> **Last updated:** 2026-10-11
> **Last action:** Alarm screen + swipe-to-wake + tone picker overhaul; fixed Home stats-always-0 bug (see changelog 2026-10-11)

---

## Current Goal

Move Meluko from "v1.0 demo" toward a beta-ready app other people can actually use. Core product and production-audit hardening (2026-08-27) are done; this session closed most of the remaining "must do before real users" and "should do for polished v1" gaps.

---

## Important: recovered a 6-week-old uncommitted backlog

The entire 2026-08-27 production audit session's work (security rules, race-condition fixes, Hilt DI cleanup, Cloud Functions scaffold, dead code removal, unit tests) was sitting **uncommitted** in the working tree since that session — it was never committed. This session verified it still built and passed tests, then committed it (`d61ebd4`) before building anything new on top. If you're reading this expecting that work to already be on GitHub: it's committed locally, but **check whether this repo's commits have been pushed to origin** — pushing was not done automatically this session (see "Push status" below).

---

## Completed Work (this session)

1. **Account deletion flow** — `AccountViewModel.deleteAccount(context, onSuccess)`: best-effort leave-tribe + delete user/user_alarms docs + delete Storage photo, then `FirebaseAuth` user delete, re-prompting Google Sign-In via Credential Manager if the session is stale. Required by Google Play policy before any real release. UI: "Delete Account" row in a danger zone on AccountScreen with a confirmation dialog.
2. **Battery-optimization banner** — dismissible banner on HomeScreen using `PowerManager.isIgnoringBatteryOptimizations` + `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` intent. Dismiss state persisted in SharedPreferences (`meluko_prefs`).
3. **Leave tribe / remove member / delete tribe** — new functions in `SocialViewModel`: `leaveTribe`, `removeMember` (admin-only), `deleteTribe` (admin-only, last member only). Firestore rules updated with an `isTribeAdmin()` helper and delete permissions on `tribes/{id}` and `tribes/{id}/members/{uid}`. **Write ordering inside each transaction matters** — e.g. the tribe's `memberCount` update must be written *before* the member doc is deleted in the same transaction, because the tribe-update rule checks `isTribeMember()` which depends on the member doc still existing at evaluation time. Mirrors the existing `joinTribe` pattern (member-write-then-tribe-update), which is how this ordering requirement was discovered to already be relied upon.
4. **Invite-code share sheet** — TribeDetailScreen now shows the invite code (it was generated at tribe creation but never displayed anywhere since) and has a share action (`Intent.ACTION_SEND`) in the top bar.
5. **`onMemberRemoved` Cloud Function** — written (Firestore-trigger on member-doc delete, clears the removed/left user's `primaryTribeId` server-side) but **not deployed** — see below.
6. **Client-side self-heal fallback** — because Cloud Functions aren't deployed, `SocialViewModel` now detects `PERMISSION_DENIED` on the members listener (removed from tribe) or a non-existent tribe doc (tribe deleted), and clears its own `primaryTribeId` client-side instead of getting stuck in an error loop. Replace this with the real `onMemberRemoved` trigger once Cloud Functions are deployed (it's more robust — handles the case where the user never reopens the app).
7. **Fun-factor UI pass** (same layout and colors, per explicit instruction): replaced the blank `Text(emoji, ...)` placeholders left over from the earlier no-emoji pass (Home stat cards, Account stat cards, Consistency metrics) with tinted icon badges (flame/sun/trophy); added a one-shot springy "celebration pulse" (`celebrateBurst`/`rememberIncreaseTrigger` in `ExpressiveUtils`) on the streak stat when it increases; warmed up two of the flattest empty-state copy lines in SocialScreen.
8. **`AuthConstants.WEB_CLIENT_ID`** — deduplicated the Google OAuth web client ID out of `AuthViewModel` so `AccountViewModel` can reuse it for re-auth.

---

## Deployment status

- **Firestore rules**: deployed to `meluko-routine` (includes the new `isTribeAdmin()` helper and leave/remove/delete-tribe permissions).
- **Cloud Functions**: written and `npm install`'d in `functions/`, but **not deployed**. `meluko-routine` is on the free Spark plan; deploying Cloud Functions requires upgrading to Blaze (pay-as-you-go, still free at this app's scale, but it's a billing decision only the account owner can make). The user explicitly chose to skip this for now. To deploy later: upgrade the project to Blaze in the Firebase Console, then `cd functions && firebase deploy --only functions`.
- **Push status**: new commits exist locally on `master` (`d61ebd4`, `d12077c`, `3fb4063`, `4a6e264`, plus doc updates) but were **not pushed to `origin/master`** automatically — check with the user and push explicitly if they want GitHub updated.

---

## Build status

- `./gradlew assembleDebug` ✅
- `./gradlew test` ✅
- `./gradlew lintDebug` ✅ (no new errors; pre-existing warnings for outdated AGP/Gradle/targetSdk versions remain)
- **No live on-device/emulator test was done this session** — no `adb`/emulator available in this environment. The account-deletion re-auth flow, tribe leave/remove/delete flows, and battery/share intents should be manually verified on a device before any real release.

---

## Important manual follow-ups required

1. **Decide on Blaze**: upgrade `meluko-routine` to Blaze if you want Cloud Functions (auto-accept proposals, server-side join, member-removal cleanup) actually running server-side. Until then, the app's client-side fallbacks (already in place) cover the same ground, just less robustly.
2. **Firebase Console**: rotate the exposed API key / OAuth client from the old committed `google-services.json`, download a fresh one to `app/google-services.json` (gitignored, already present locally), register new release SHA-1/SHA-256 fingerprints.
3. **Push to GitHub**: this session's commits are local-only so far.
4. Privacy policy / terms of service links — still needed before Play Store submission.
5. Rebuild release APK with the rotated keystore and re-upload to GitHub Releases once the above is done.

---

## Known remaining gaps (acceptable for beta, not blockers)

- **Offline wakes aren't recorded** (AlarmActivity uses a Firestore transaction, which needs network) and MissedAlarmReceiver then marks the user missed. Highest-priority follow-up.
- Debug preview: Anonymous auth is enabled in Firebase Console for the LoginScreen "Skip Login (Debug Preview)" button (DEBUG builds only).

- Cloud Functions not deployed (see above) — client-side fallbacks cover auto-accept, join, and member-removal cleanup, just with slightly weaker guarantees (e.g. requires the affected client to be online to self-heal).
- No FCM push notifications — proposals/vetoes/wake events still require the app to be open or a Firestore listener active.
- No account-deletion confirmation beyond a single dialog (no "type DELETE to confirm" friction) — acceptable for beta scale.
- Outdated dependency versions (AGP 9.2.1, Gradle 9.4.1, targetSdk 36 — lint warns).
- No automated CI/CD, no UI tests.

---

## Files changed (high-level, this session)

- `AndroidManifest.xml` — `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` permission.
- `auth/AuthConstants.kt` (new), `auth/AuthViewModel.kt` — shared web client ID.
- `ui/AccountScreen.kt`, `ui/AccountViewModel.kt` — delete account UI + flow, icon badges.
- `ui/HomeScreen.kt` — battery banner, icon badges, streak celebration.
- `ui/SocialScreen.kt`, `ui/SocialViewModel.kt` — `TribeMember.uid`/`isAdmin`, leave/remove/delete-tribe, stale-pointer self-heal, empty-state copy.
- `ui/TribeDetailScreen.kt` — invite code display + share, manage-tribe section (leave/delete), per-member remove action.
- `ui/ExpressiveUtils.kt` — `celebrateBurst()`, `rememberIncreaseTrigger()`.
- `firestore.rules` — `isTribeAdmin()`, member/tribe delete permissions (deployed).
- `functions/index.js` — `onMemberRemoved` trigger (not deployed).
- `.ai/*.md` — this session's updates.
