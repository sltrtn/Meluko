# 🚀 MELUKO v1.0 RELEASE — MASTER PROMPT

> **Run this prompt at the start of every session.** It contains the complete roadmap, current state, and execution loop to ship v1.0 on Play Store.

---

## 🎯 THE GOAL

**Ship Meluko v1.0 to Google Play Store** — a social alarm clock where a tribe shares one alarm. If you sleep through it, your whole tribe knows. No snooze. No hiding.

**Definition of Done:**
- ✅ Core loop: alarm fires → tribe sees live status → swipe to wake → streak updates → alarm stops for *everyone* when tribe settles
- ✅ Reliability: exact alarms fire on Doze, survive force-kill, reschedule on reboot, work offline (cached)
- ✅ Auth: Google Sign-In (Credential Manager) + account deletion (Play policy)
- ✅ Tribe mgmt: create/join by code, leave, admin remove, delete tribe, invite code share
- ✅ Proposals: 2h veto window, auto-accept, one veto kills it
- ✅ Release build: signed AAB, ProGuard rules, no debug code, `google-services.json` rotated & gitignored
- ✅ Store assets: Privacy Policy URL, Terms URL, screenshots, feature graphic, store listing
- ✅ Firebase: Firestore rules deployed, Cloud Functions deployed (Blaze), release SHA-1/256 registered
- ✅ Tests: unit tests pass, lint clean, manual on-device smoke test (alarm → wake → tribe sees it)

---

## 📍 CURRENT STATE (as of 2026-11-02)

| Area | Status | Blockers |
|------|--------|----------|
| Core alarm logic | **95% ✅** | C1–C5 **FIXED** (alarm holds for tribe, no restart after wake, missed-check transactional) |
| Architecture | 100% | — |
| Auth + Account deletion | 100% | — |
| Tribe mgmt | 100% | — |
| Proposals/Vetoes | 100% | Cloud Functions written, **not deployed** (needs Blaze) |
| Offline/Cache | 90% | Wake recording fails offline (F1) |
| Release infra | 60% | **ProGuard missing, keys not rotated, Blaze not upgraded, Store assets missing** |
| Tests | 30% | Only 2 unit tests, no UI tests |

**Last commit:** `5331916` — "Hold tribe alarm until all members wake or miss"
**This session:** Fixed C1, C2, C3 (AlarmActivity), C4 (AlarmReceiver), C5 (MissedAlarmReceiver) — all builds green

---

## 🔴 P0 BLOCKERS — MUST FIX FIRST (Week 1)

### C1–C5: Alarm Race Conditions — **✅ ALL FIXED**

| Bug | Location | Fix | Status |
|-----|----------|-----|--------|
| **C1** Stale `uid` in tribe listener | `loadTribeSnapshot()` line 167-227 | Use `firebaseAuth.currentUser?.uid` inside listener | ✅ Done |
| **C2** Multiple overlapping rings | `loadTribeSnapshot()` line 205-224 | Track `isRinging` state, guard `startTone()`/`startVibration()` | ✅ Done |
| **C3** Alarm restarts after wake | `handleWake()` line 229-248 | Removed restart block; alarm stops only when tribe settles | ✅ Done |
| **C4** `resetOwnStatusForNewDay()` runs on every fire | `AlarmReceiver.kt` line 91-103 | Read status first, only reset if not awake | ✅ Done |
| **C5** Missed-check race | `MissedAlarmReceiver.kt` line 50-66 | Use **transaction** for atomic read+update | ✅ Done |

### B4: ProGuard Rules for Release Build — **✅ DONE**
`app/proguard-rules.pro` now covers Hilt, Firebase, Coil (v2+3), Haze, Material3 Expressive, androidx.graphics.shapes, Credential Manager, Google Identity. Verified by successful `./gradlew bundleRelease` (R8 minification + resource shrinking).

### B5: Firebase Key Rotation & Blaze Upgrade
- Rotate API key + OAuth client in Firebase Console
- Download fresh `google-services.json` → `app/google-services.json` (gitignored)
- Register new release SHA-1: `AD:33:B3:9B:9E:69:BF:AD:5E:5E:30:12:81:C6:02:34:A4:44:C1:8B` / SHA-256: `05:63:6D:87:7C:C7:F0:F0:12:13:FE:9E:79:17:8C:31:B5:F3:DA:E1:93:B8:4A:01:EF:2C:B7:96:75:87:0D:BD`
- Upgrade `meluko-routine` to Blaze plan
- Deploy Cloud Functions: `cd functions && npm install && firebase deploy --only functions`

---

## 🟠 P1 RELIABILITY (Week 1-2)

| Task | File | Action | Status |
|------|------|--------|--------|
| Fix `resetOwnStatusForNewDay()` | `AlarmReceiver.kt` | Check current status before resetting to sleeping | ✅ Done (C4) |
| Fix `MissedAlarmReceiver` race | `MissedAlarmReceiver.kt` | Use transaction for read+update | ✅ Done (C5) |
| Fix `BootReceiver` ghost alarms | `BootReceiver.kt` | Verify tribe membership before scheduling | 🔄 Next |
| Fix `AlarmScheduler` fallback | `AlarmScheduler.kt` | Show banner if exact alarm permission denied (already in HomeScreen) | ⏳ |
| Fix `HomeViewModel` duplicate alarms | `HomeViewModel.kt` | Cancel old alarm before scheduling new | ⏳ |
| Fix `ProposalViewModel` clock drift | `ProposalViewModel.kt` | Use `FieldValue.serverTimestamp()` for expiry check, not device `Date()` | ⏳ |

---

## 🟡 P2 POLISH (Week 2)

| Task | File | Action |
|------|------|--------|
| ProGuard rules | `app/proguard-rules.pro` | Write comprehensive rules |
| Privacy Policy / Terms | `docs/privacy.md`, `docs/terms.md` | Simple markdown, host on Git Pages or Firebase Hosting |
| Play Store assets | `play-store/` | Screenshots (phone + tablet), feature graphic (1024x500), icon (512x512) |
| Manual device test | — | Install release AAB on physical device, test full tribe flow |
| Lint clean | — | `./gradlew lintDebug` → 0 errors |
| Unit tests pass | — | `./gradlew test` → all green |

---

## 🟢 P3 NICE TO HAVE (Post-v1.0)

- Offline wake recording (Firestore cache + sync)
- FCM push notifications
- Widget + Wear OS tile
- Goodnight Protocol (evening check-in)
- Habit tribes (generic commitments)

---

## ⚙️ EXECUTION LOOP — RUN EVERY SESSION

```bash
# 1. Sync & build
./gradlew assembleDebug
./gradlew test
./gradlew lintDebug

# 2. Fix the NEXT unchecked item from P0 → P1 → P2
#    (Edit code, re-run above until green)

# 3. Commit & push
git add -A
git commit -m "v1.0: <specific fix>"
git push origin master

# 4. When ALL P0+P1+P2 done:
#    - Build release AAB: KEYSTORE_PASSWORD=... KEY_PASSWORD=... ./gradlew bundleRelease
#    - Upload to Play Console Internal Testing
#    - Test on real device with real tribe
#    - Promote to Production
```

---

## 📋 SESSION CHECKLIST (Copy to top of each session)

```
[ ] Read this PROMPT_V1_RELEASE.md
[ ] Run ./gradlew assembleDebug test lintDebug
[ ] Pick ONE unchecked P0/P1/P2 item
[ ] Fix it completely (code + verify build green)
[ ] Commit with conventional message
[ ] Update this file: check off completed item
[ ] Push to origin/master
```

---

## 🔑 KEY FILES TO TOUCH

| Priority | File | Purpose | Status |
|----------|------|---------|--------|
| P0 | `app/src/main/java/com/alarm/meluko/alarm/AlarmActivity.kt` | Fix C1, C2, C3 | ✅ Done |
| P0 | `app/src/main/java/com/alarm/meluko/alarm/AlarmReceiver.kt` | Fix C4 | ✅ Done |
| P0 | `app/src/main/java/com/alarm/meluko/alarm/MissedAlarmReceiver.kt` | Fix C5 | ✅ Done |
| P0 | `app/proguard-rules.pro` | **Create** — Release ProGuard rules | ✅ Done |
| P0 | `functions/index.js` | Deploy to Firebase (Blaze) | ⏳ |
| P1 | `app/src/main/java/com/alarm/meluko/alarm/BootReceiver.kt` | Fix ghost alarms | ⏳ |
| P1 | `app/src/main/java/com/alarm/meluko/alarm/AlarmScheduler.kt` | Permission fallback UX | ⏳ |
| P1 | `app/src/main/java/com/alarm/meluko/ui/HomeViewModel.kt` | Cancel old alarm | ⏳ |
| P1 | `app/src/main/java/com/alarm/meluko/ui/ProposalViewModel.kt` | Server-time expiry | ⏳ |
| P2 | `docs/privacy.md`, `docs/terms.md` | **Create** — Store policies | ⏳ |
| P2 | `play-store/` | **Create** — Store assets | ⏳ |

---

## 🚫 DO NOT DO (Until v1.0 Shipped)

- ❌ Add new features (Goodnight, habits, chat, widgets)
- ❌ Refactor working code for "cleanliness"
- ❌ Write UI tests (unit tests only for critical logic)
- ❌ Optimize performance
- ❌ Change UI/design system
- ❌ Add analytics, crashlytics config (already in, just verify)

---

## 🎯 SUCCESS CRITERIA FOR THIS PROMPT

When you can run this and **every check passes**:

```bash
./gradlew assembleDebug test lintDebug        # All green
./gradlew bundleRelease                       # Signed AAB builds
# Manual test: alarm fires → tribe sees live → swipe → streak → all stop
# Play Console: Internal Testing → Production
```

**Then v1.0 is shipped.** 🎉

---

## 📝 NOTES FOR THE AGENT

- **Read this file first** every session.
- **Update the checkboxes** in this file as you complete items.
- **One item at a time** — finish completely before moving on.
- **Commit early, commit often** — conventional messages: `v1.0: fix C3 alarm restart after wake`
- **Push to origin/master** after every commit.
- **No new files** unless explicitly listed above.
- **Ask the user** only for: Blaze upgrade confirmation, Play Console access, keystore passwords (if not in env).

---

*Generated 2026-11-02 | Meluko v1.0 Release Tracker*