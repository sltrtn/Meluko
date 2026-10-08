# Meluko — Interview Prep Roadmap

> Study guide series. Read in order. Each file is one lesson. Every concept is mapped to your actual code.

## The goal

You can explain Meluko from first principles, trace a request through the app, and defend every architectural decision in an interview.

## Learning formats

| Format | Where |
|---|---|
| Written lessons | These `.md` files |
| Interactive Q&A | Live chat sessions |
| Flashcards | `13-flashcards.md` + `flashcard_quiz.py` (recite until automatic) |
| Mock interviews | Live chat rounds |

## The sequence

- [ ] **00 — Roadmap** (this file)
- [ ] **01 — Mental model** — the social accountability loop, tribes, proposals/veto, streaks
- [ ] **02 — Config and entry** — `MainActivity`, `AppNavGraph`, routes, Hilt, theme
- [ ] **03 — Auth** — Google Sign-In via Credential Manager, session check, resilience
- [ ] **04 — Firestore data model** — collections, fields, security rules
- [ ] **05 — Tribes and members** — `SocialViewModel`, live statuses, awakeCount
- [ ] **06 — Alarm scheduling** — `AlarmScheduler`, `AlarmReceiver`, exact alarms, Doze
- [ ] **07 — Reliability and offline** — `BootReceiver`, `AlarmCache`, `ConnectivityObserver`
- [ ] **08 — Proposals and veto** — `ProposalViewModel`, wheel picker, auto-accept, batches
- [ ] **09 — UI and animations** — Material 3, design system, `MotionSpec`, `pressScale`
- [ ] **10 — Build and release** — signing, R8, Crashlytics, SHA registration
- [ ] **11 — Judgment layer** — decisions, traps, AI framing, the one-line pitch
- [ ] **12 — Numbers table** — quick-reference facts to say cold
- [ ] **13 — Flashcards** — Q&A drills

## How to use this

1. Read one file.
2. Close it and explain the concept out loud (to yourself or voice memo).
3. Open the referenced code file and trace the actual lines.
4. Try the self-check questions without looking.
5. Move on only when you can answer in plain English.

## The one rule

Every answer must connect to **your code**. Abstract definitions alone won't survive an interview follow-up.
