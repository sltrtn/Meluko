# Handoff — Session End

> **Last updated:** 2026-07-27
> **Last action:** Tribe system polish — 13 critical/high fixes + alarm reliability

---

## Current Goal

Ship Meluko v1 to production. Tribe system and alarm reliability are the core focus.

---

## Completed Work (this session)

### Alarm Tone Picker:
- System ringtone picker (`TYPE_ALARM`) in AccountScreen settings
- Selected tone URI saved to Firestore at `user_alarms/{uid}.alarmToneUri`
- AlarmActivity plays default immediately, then async loads custom tone from Firestore
- AccountViewModel: `alarmToneUri` state + `updateAlarmTone()` function
- FirestoreConstants: added `ALARM_TONE_URI`, `TONE_URI`, `TONE_SOURCE` fields
- SettingsRow: added optional `subtitle` parameter

### Alarm Reliability (Full-Screen Notification + Daily Repeat):
- AlarmReceiver now reschedules alarm for next day **immediately when it fires** — no longer depends on user dismissing
- Notification channel: `IMPORTANCE_HIGH`, bypass DND, lockscreen public visibility
- Notification: `setOngoing(true)` (can't swipe away), full-screen intent
- Removed duplicate `rescheduleAlarmForTomorrow()` from AlarmActivity (now redundant)
- Fixed duplicate `TODAY_WAKE_TIME` key in Firestore update map

### Tribe System Polish (13 fixes):

**CRITICAL:**
1. `TribeInfo` now has `id` field — tribe navigation uses Firestore doc ID, not name
2. `activeProposal!!` → `activeProposal?.let` — eliminated NPE race condition
3. `awakeCount` computed from live member statuses (was hardcoded to 0)
4. VetoBottomSheet waits for async Firestore write before showing success
5. ProposeAlarmBottomSheet resets to Idle on error (was stuck in loading)
6. Proposal time normalized to 24h format (`"07:30 AM"` → `"19:30"`) before Firestore write

**HIGH:**
7. Stale error cleared on Join/Create tab switch in JoinOrCreateTribeScreen
8. TribeDetailScreen shows error state + retry when Firestore listener fails
9. SocialScreen shows empty states ("Join or create a tribe" / "No members yet")
10. Notification permission checked before requesting (not every composition)
11. Propose button hidden when user has no tribe
12. Auto-accept guarded against duplicate snapshot calls
13. Tribe name limited to 30 characters

### Previously completed (prior sessions)
- Full app architecture: Single Activity, Compose Navigation, Hilt DI
- Google Sign-In with Credential Manager + Firebase Auth
- Onboarding flow (3 pages) + profile creation + tribe join/create
- 3-tab main shell (Social/Home/Account) with animated switching
- Alarm system: AlarmManager, BroadcastReceiver, full-screen AlarmActivity, SwipeToWakeButton
- Social features: Tribe member list, real-time status, proposals/veto (2-hour window), streaks
- Full Material 3 design system: Sunrise Amber, spring animations, dark mode
- Scatter-letter splash screen, glass-morphism nav bar, expressive bottom sheets
- Shimmer loading states, font migration (Outfit), MotionSpec centralization
- Crashlytics, BootReceiver, Firestore rules, debug TEST button

---

## Files Modified/Created (this session)

| File | Action |
|---|---|
| `app/.../ui/AccountScreen.kt` | Modified — alarm tone picker row, SettingsRow subtitle, TribeInfo id |
| `app/.../ui/AccountViewModel.kt` | Modified — alarmToneUri state, updateAlarmTone(), error on listeners |
| `app/.../ui/HomeScreen.kt` | Modified — TribeInfo id field, hasTribe prop, notification permission fix |
| `app/.../ui/SocialViewModel.kt` | Modified — awakeCount from live data, null tribeId handling |
| `app/.../ui/SocialScreen.kt` | Modified — empty states for tribes and members |
| `app/.../ui/TribeDetailScreen.kt` | Modified — activeProposal?.let, error state, VetoBottomSheet callback |
| `app/.../ui/ProposeAlarmBottomSheet.kt` | Modified — 24h time normalization, onError callback |
| `app/.../ui/VetoBottomSheet.kt` | Modified — async success callback, onConfirmVeto signature |
| `app/.../ui/ProposalViewModel.kt` | Modified — onError callback, isAccepting guard |
| `app/.../ui/JoinOrCreateTribeScreen.kt` | Modified — clearError on tab switch, tribe name max 30 chars |
| `app/.../alarm/AlarmReceiver.kt` | Modified — reschedule on fire, notification channel improvements |
| `app/.../alarm/AlarmActivity.kt` | Modified — custom tone loading, removed reschedule, fixed dup key |
| `app/.../data/FirestoreConstants.kt` | Modified — added ALARM_TONE_URI, TONE_URI, TONE_SOURCE |

---

## Remaining Work

### Next:
- [ ] Fix remaining crashes (Crashlytics active — wait for data)
- [ ] Daily alarm repeat verification (test across reboot, force-kill)
- [ ] Alarm volume control (`STREAM_ALARM` audio focus)

### Then:
- [ ] Battery optimization prompt (OEM-specific)
- [ ] Release signing config
- [ ] Multiple alarm support
- [ ] Snooze feature

### Later:
- [ ] Unit tests for ViewModels
- [ ] Compose UI tests for critical flows
- [ ] Weekly tribe stats
- [ ] Home screen widget
- [ ] Play Store preparation

---

## Known Bugs

1. **Crashes on tribe screen** — Crashlytics now active, waiting for data. TribeDetailScreen had `activeProposal!!` NPE (fixed this session).
2. **VetoBottomSheet hardcoded green color** — `Color(0xFF2E7D32)` instead of theme color (cosmetic, low priority).

---

## Important Context

- **Repo:** `https://github.com/sltrtn/Meluko` (origin: sltrtn/Meluko, branch: master)
- **Local path:** `/home/mad/StudioProjects/Meluko`
- **Package:** `com.alarm.meluko`
- **Min SDK:** 24, **Target SDK:** 36
- **Build:** `JAVA_HOME=/home/mad/android-studio/jbr ./gradlew assembleDebug`
- **Firebase project:** `meluko-routine`
- **Auth:** Google Sign-In only (no email/password)
- **DI:** Dagger Hilt with KSP
- **Debug keystore SHA-1:** `90:20:E1:20:94:73:5E:6A:01:EA:4E:C8:50:F5:28:A3:CD:61:E4:3D`
- **Debug keystore SHA-256:** `07:17:99:44:AE:2F:B0:AB:D6:AC:10:1A:D9:73:82:72:3A:96:AE:75:5F:92:53:71:92:E2:38:95:77:8C:17:B7`
