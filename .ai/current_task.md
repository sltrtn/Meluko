# Current Task

> Always represents exactly what is currently being worked on.

---

## Objective

**Polish Meluko's onboarding, alarm UX, and data wiring** — fix alarm screen flash, complete onboarding with SetFirstAlarmScreen, wire propose flow to Firestore, replace mock/hardcoded data with real values.

---

## Status

🔄 Completed — SHA-1/SHA-256 fingerprints added to Firebase Console. Google Sign-In now works. Remaining: diagnose crashes on tribe screen and elsewhere.

---

## Steps

- [x] Fix alarm screen flash (dark theme, @AndroidEntryPoint, black window bg)
- [x] Wire ProposeAlarmBottomSheet → ProposalViewModel.createProposal()
- [x] Add SetFirstAlarmScreen to onboarding after tribe join/create
- [x] Replace hardcoded HomeScreen stats with Firestore-driven values
- [x] Replace WeeklyConsistencyGraph mock data with real ConsistencyCard
- [x] Use WriteBatch in ProposalViewModel.acceptProposal()
- [x] Fix pre-existing build errors (ExpressiveUtils, SwipeToWakeButton, AlarmScreen)
- [x] Update .ai/ project memory for handoff

---

## Next Immediate Step

Diagnose crashes on tribe screen and elsewhere via Logcat:
```bash
adb logcat --pid=$(adb shell pidof -s com.alarm.meluko) *:E
```
