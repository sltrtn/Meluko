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
- ✅ Missed status detection — 10-minute grace period after alarm
- ✅ Daily reset — own status reset to sleeping at fire time

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
- ✅ Emoji-free UI (AGENTS.md compliant)

### ✅ Social Features
- ✅ Tribe creation and join by invite code
- ✅ Real-time tribe member list with statuses
- ✅ Alarm proposal creation + veto system
- ✅ 2-hour auto-accept window for proposals
- ✅ Streak tracking (awake/best/total)
- ✅ Rank recomputation from live streaks
- ✅ Tribe navigation with Firestore doc IDs

### ✅ Polish & Reliability
- ✅ Tribe system — 13 critical/high fixes (navigation, race conditions, empty states, time format)
- ✅ Error handling — UiState, snackbar errors, error states in UI
- ✅ Notification permission — checked before requesting
- ✅ Auto-accept guarded against duplicate snapshot calls
- ✅ Firestore transaction in dismiss flow prevents double-count

### ✅ Infrastructure
- ✅ Firebase Crashlytics — crash reporting
- ✅ Firestore security rules — deployed and tightened
- ✅ Debug TEST button (BuildConfig.DEBUG-gated)
- ✅ Firebase Cloud Functions scaffold (auto-accept + join-by-code)
- ✅ Unit tests for TimeUtils and StreakCalculator
- ✅ Release signing config with env-var passwords
- ✅ Local secret rotation — new keystore, google-services.json untracked

---

## Current Milestone

### 📦 V1 Ship (production-ready foundation)
- All critical/high bugs fixed
- Alarm system reliable through Doze/reboot/force-kill
- Cloud Functions written, awaiting deployment
- Secrets rotated locally, awaiting Firebase Console updates

---

## Future Milestones

### 🔄 Must do before real users / Play Store
1. Deploy Cloud Functions (`cd functions && npm install && firebase deploy --only functions`)
2. Rotate Firebase API key + download fresh `google-services.json`
3. Register new release SHA-1/SHA-256 fingerprints in Firebase Console
4. Battery optimization whitelist prompt (OEMs kill exact alarms)
5. Privacy policy and terms of service links
6. Account deletion flow (required by Google Play policy)
7. Rebuild release APK with new keystore and re-upload to GitHub Releases

### ⏳ Should do for polished v1
- FCM push notifications (proposals, vetoes, acceptance, wake events)
- Invite code sharing (copy/share sheet)
- Leave tribe / remove member / delete tribe
- Wire `joinTribe` client flow to the Cloud Function
- Remove client-side auto-accept once Cloud Function is trusted

### ⏳ Nice-to-have features
- Notification deep links (tap notification → open specific screen)
- Weekly tribe stats (average wake time, best streak race)
- Custom alarm sounds per tribe (Spotify integration, sound packs)
- Timezone support for global tribes
- "Panic" alarm — escalate if someone is still "sleeping" past N minutes
- Widget (home screen alarm status)
- Wear OS companion app
- Multiple alarm support
- Snooze feature

### ⏳ Engineering quality
- Unit tests for ViewModels
- UI tests for critical flows (login, alarm, propose/veto)
- Firebase emulator support for local testing
- CI/CD with GitHub Actions (build + test on PR)
- Firebase Performance Monitoring
- Analytics (Firebase Analytics)

### ⏳ Production Readiness
- Play Store listing preparation
- App signing and release management
- Automated mapping upload verification
