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

### ✅ UI & Design
- ✅ Material 3 theme with Sunrise Amber (#FF8C00)
- ✅ Clash Display fonts (6 weights) + Inter fallback
- ✅ Spring-based pressScale animations
- ✅ True black dark mode
- ✅ Scatter-letter splash screen
- ✅ Glass-morphism navigation bar
- ✅ SwipeToWakeButton custom gesture widget
- ✅ Propose/Veto bottom sheets (no drag handle)
- ✅ Onboarding flow (3 pages)
- ✅ Profile creation with photo picker

### ✅ Social Features
- ✅ Tribe creation and join by invite code
- ✅ Real-time tribe member list with statuses
- ✅ Alarm proposal creation + veto system
- ✅ 2-hour auto-accept window for proposals
- ✅ Streak tracking (awake/best/total)

---

## Current Milestone

### 🔄 Documentation & Project Memory
- Setting up `.ai/` project memory system
- Creating AGENTS.md with permanent instructions
- Documenting architecture and decisions

---

## Future Milestones

### ⏳ Testing & Quality
- Write unit tests for ViewModels
- Write UI tests for critical flows (login, alarm, propose/veto)
- Add Firebase emulator support for local testing

### ⏳ Features & Polish
- Notification deep links (tap notification → open specific screen)
- Weekly tribe stats (average wake time, best streak race)
- Tribe leaderboard
- Custom alarm sounds per tribe
- Timezone support for global tribes
- "Panic" alarm — escalate if someone is still "sleeping" past N minutes
- Widget (home screen alarm status)
- Wear OS companion app

### ⏳ Infrastructure
- CI/CD with GitHub Actions
- Automated build + test on PR
- Firebase Performance Monitoring
- Crashlytics integration

### ⏳ Production Readiness
- Privacy policy and terms of service
- Play Store listing preparation
- App signing and release management
- Analytics (Firebase Analytics)

---

## Technical Debt

- `google-services.json` committed — should use CI secret injection for production builds
- Firestore security rules not documented — need to extract and version in repo
- No offline-first support (Firestore default cache only)
- Error states in UI could be more descriptive (mostly generic snackbar)
- No loading skeleton/shimmer — uses circular progress indicator
- `local.properties` includes SDK path — ensure `.gitignore` keeps it excluded
- Alarm permission flow on Android 14+ needs edge-case testing (permission revoked, battery optimization)

---

## Planned Improvements

- Migrate to Jetpack Compose BOM aligned with latest Compose compiler
- Add proper instrumentation tests with Compose Test
- Extract Firestore collection/field names into constants
- Add ViewModel unit tests with Fake repositories
- Implement proper error handling with sealed class UiState (Loading/Success/Error)
