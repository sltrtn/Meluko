# Meluko — Roadmap

---

## Legend
- ✅ Completed
- 🔄 In Progress
- ⏳ Planned
- ❌ Blocked

---

## Completed Milestones

### ✅ Initial App Scaffold
- ✅ Google Sign-In with Credential Manager
- ✅ Firebase Auth + Firestore + Storage integration
- ✅ Hilt DI setup
- ✅ Navigation graph with all routes

### ✅ Core Alarm System
- ✅ Alarm scheduling via AlarmManager
- ✅ AlarmReceiver with notification + full-screen intent
- ✅ AlarmActivity with swipe-to-wake dismiss
- ✅ Runtime permission handling (SCHEDULE_EXACT_ALARM, USE_EXACT_ALARM)
- ✅ BootReceiver — alarm reschedules after reboot
- ✅ Alarm screen — pure black, centered clock, system alarm aesthetic
- ✅ Daily repeat — reschedule on fire (not on dismiss), survives force-kill
- ✅ Custom alarm tone — system ringtone picker, stored in Firestore

### ✅ UI & Design
- ✅ Material 3 theme with Sunrise Amber (#FF8C00)
- ✅ Outfit font family (9 weights, static TTF)
- ✅ Spring-based pressScale animations
- ✅ Background depth (#0F0F12 with 3-tier surface hierarchy)
- ✅ Scatter-letter splash screen
- ✅ Glass-morphism navigation bar
- ✅ SwipeToWakeButton custom gesture widget
- ✅ Propose/Veto bottom sheets (no drag handle)
- ✅ Onboarding flow (3 pages)
- ✅ Profile creation with photo picker
- ✅ Shimmer loading states (4 screens)
- ✅ MotionSpec centralization (6 named specs)

### ✅ Social Features
- ✅ Tribe creation and join by invite code
- ✅ Real-time tribe member list with statuses
- ✅ Alarm proposal creation + veto system
- ✅ 2-hour auto-accept window for proposals
- ✅ Streak tracking (awake/best/total)
- ✅ Tribe navigation with Firestore doc IDs

### ✅ Polish & Reliability
- ✅ Tribe system — 13 critical/high fixes (navigation, race conditions, empty states, time format)
- ✅ Error handling — UiState, snackbar errors, error states in UI
- ✅ Notification permission — checked before requesting
- ✅ Auto-accept guarded against duplicate snapshot calls

### ✅ Infrastructure
- ✅ Firebase Crashlytics — crash reporting
- ✅ Firestore security rules — deployed
- ✅ Debug TEST button (BuildConfig.DEBUG-gated)

---

## Current Milestone

### 📦 V1 Ship
- Tribe system polished and reliable
- Alarm system with daily repeat + custom tones
- All critical bugs fixed

---

## Future Milestones

### ⏳ Testing & Quality
- Write unit tests for ViewModels
- Write UI tests for critical flows (login, alarm, propose/veto)
- Add Firebase emulator support for local testing

### ⏳ Features
- Notification deep links (tap notification → open specific screen)
- Weekly tribe stats (average wake time, best streak race)
- Custom alarm sounds per tribe (Spotify integration, sound packs)
- Timezone support for global tribes
- "Panic" alarm — escalate if someone is still "sleeping" past N minutes
- Widget (home screen alarm status)
- Wear OS companion app
- Multiple alarm support
- Snooze feature

### ⏳ Infrastructure
- CI/CD with GitHub Actions
- Automated build + test on PR
- Firebase Performance Monitoring
- Analytics (Firebase Analytics)

### ⏳ Production Readiness
- Battery optimization prompt (OEM-specific)
- Release signing config
- Privacy policy and terms of service
- Play Store listing preparation
- App signing and release management

---

## Technical Debt

- `google-services.json` committed — should use CI secret injection for production builds
- No offline-first support (Firestore default cache only)
- No unit tests or UI tests
- `local.properties` includes SDK path — ensure `.gitignore` keeps it excluded
- Alarm permission flow on Android 14+ needs edge-case testing (permission revoked, battery optimization)
