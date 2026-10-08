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
- ✅ Firestore security rules — deployed and tightened (now live on meluko-routine, incl. admin/leave/delete-tribe rules)
- ✅ Debug TEST button (BuildConfig.DEBUG-gated)
- ✅ Firebase Cloud Functions scaffold (auto-accept + join-by-code + onMemberRemoved) — written, **not deployed** (project is on the free Spark plan; deploying requires upgrading to Blaze)
- ✅ Unit tests for TimeUtils and StreakCalculator
- ✅ Release signing config with env-var passwords
- ✅ Local secret rotation — new keystore, google-services.json untracked

### ✅ Beta-readiness pass (2026-10-08)
- ✅ Account deletion flow (Google re-auth on stale session, best-effort data cleanup)
- ✅ Battery-optimization whitelist banner (dismissible)
- ✅ Leave tribe / admin remove-member / admin delete-tribe (last member only)
- ✅ Invite-code share sheet + visible invite code on TribeDetailScreen
- ✅ Client-side self-heal for stale tribe pointers (stands in for the undeployed onMemberRemoved function)
- ✅ Fun-factor UI pass — icon badges replacing blank emoji-removal placeholders, streak celebration pulse, warmer empty-state copy (same layout/colors)

---

## Current Milestone

### 📦 V1 Ship (production-ready foundation)
- All critical/high bugs fixed
- Alarm system reliable through Doze/reboot/force-kill
- Firestore rules deployed; Cloud Functions written but not deployed (needs Blaze plan)
- Secrets rotated locally, awaiting Firebase Console updates

---

## Future Milestones

### 🔄 Must do before real users / Play Store
1. Upgrade meluko-routine to the Blaze plan, then deploy Cloud Functions (`cd functions && firebase deploy --only functions`)
2. Rotate Firebase API key + download fresh `google-services.json`
3. Register new release SHA-1/SHA-256 fingerprints in Firebase Console
4. Privacy policy and terms of service links
5. Rebuild release APK with new keystore and re-upload to GitHub Releases

### ⏳ Should do for polished v1
- FCM push notifications (proposals, vetoes, acceptance, wake events)
- Wire `joinTribe` client flow to the Cloud Function (once Blaze is enabled)
- Remove client-side auto-accept once Cloud Function is trusted (once Blaze is enabled)
- Replace the client-side stale-tribe-pointer self-heal with the onMemberRemoved trigger once functions are deployed

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
