# Meluko — Changelog

> Human-readable summary of major repository changes. Not every commit — only meaningful milestones.

---

## 2026-07-02 — Polish Pass

**Fixed:**
- Alarm screen white flash — added `@AndroidEntryPoint`, dark theme, black window background
- `ProposeAlarmBottomSheet` now actually calls `ProposalViewModel.createProposal()` instead of doing nothing
- HomeScreen stats (streak, wake-ups, rank) now sourced from Firestore instead of hardcoded values
- Removed mock `WeeklyConsistencyGraph` — replaced with real `ConsistencyCard`
- `acceptProposal()` uses `WriteBatch` for atomic updates

**Added:**
- `SetFirstAlarmScreen` — new onboarding step after tribe join/create with time picker
- `Theme.Meluko.Alarm` — dark window background theme for AlarmActivity
- `primaryTribeId`, `streakDays`, `totalWakes`, `rank` to `HomeViewModel`
- `id` field to `TribeProgress` data class

**Changed:**
- `FirebaseModule` now provides `FirebaseFirestore`
- `AppNavGraph` routes onboarding → `SetFirstAlarmScreen` → Home (instead of directly to Home)

**Fixed pre-existing build errors:**
- `ExpressiveUtils.kt` — launch in PressInteractionScope
- `SwipeToWakeButton.kt` — suspend calls in drag handlers wrapped in scope.launch
- `AlarmScreen.kt` — missing graphicsLayer import

---

## 2026-07-02 — Project Memory System

**Created:** `.ai/` directory with full project memory: project.md, roadmap.md, current_task.md, progress.md, decisions.md, handoff.md, changelog.md.
**Created:** `AGENTS.md` with permanent project instructions.

---

## [Prior Milestones — App Development]

- **Initial commit** — Project scaffold with AGP, Compose, Hilt, Navigation
- **Google Sign-In** — Credential Manager + Firebase Auth integration
- **Onboarding flow** — 3-page pager, profile creation, tribe join/create
- **Main app shell** — 3-tab layout with animated switching
- **Alarm system** — AlarmManager, AlarmReceiver, AlarmActivity, SwipeToWakeButton
- **Social features** — Tribe member list, real-time status, proposals, veto
- **Design system** — Material 3 Sunrise Amber theme, Clash Display fonts, spring animations, scatter-letter splash
