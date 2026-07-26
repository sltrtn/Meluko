# Meluko — Architectural Decisions

> Every important decision with date, reason, alternatives, and consequences.

---

## 2026-07-27 — Alarm Reschedule on Fire (Not on Dismiss)

**Decision:** Reschedule the next-day alarm in `AlarmReceiver.onReceive()` when the alarm fires, not in `AlarmActivity.handleDismiss()` when the user dismisses.

**Reason:** If reschedule happens on dismiss, the next alarm is lost if the user force-kills the app, the dismiss Firestore write fails, or the user walks away from the phone. Rescheduling on fire guarantees the next alarm is always set.

**Alternatives Considered:**
- Reschedule on dismiss (previous approach) — fragile, depends on user interaction.
- `setRepeating` AlarmManager API — doesn't support exact alarms on Doze mode.
- WorkManager daily task — too imprecise for a wake-up alarm.

**Consequences:**
- Alarm is always rescheduled, even if user never interacts.
- `AlarmActivity.handleDismiss()` no longer calls `rescheduleAlarmForTomorrow()` — cleaner separation.
- Same time next day is scheduled; if user changes tribe alarm time, the next fire will use the new time.

---

## 2026-07-27 — System Ringtone Picker for Alarm Tone

**Decision:** Use Android's built-in `RingtoneManager.ACTION_RINGTONE_PICKER` for alarm tone selection, storing the URI in Firestore.

**Reason:** Zero dependencies, works on all Android versions, user already knows the system picker, covers all system tones. Differentiation via Spotify/sound packs can come in v2.

**Alternatives Considered:**
- Spotify Web API previews — requires OAuth flow, network dependency, 30-sec clips only. Deferred to v2.
- Bundled sound packs — adds APK size, limited selection. Deferred to v2.
- In-app custom picker — reinvents the wheel, system picker is more familiar.

**Consequences:**
- Tone URI stored at `user_alarms/{uid}.alarmToneUri` in Firestore.
- AlarmActivity plays default immediately, then async loads custom tone (no delay on alarm start).
- AccountScreen shows "Custom tone set" or "Default alarm sound" subtitle.

---

## 2026-07-27 — 24h Time Format for Proposals

**Decision:** Normalize proposal time to 24h format (`"HH:MM"`) before Firestore write, converting from the picker's 12h AM/PM format.

**Reason:** `AlarmScheduler.schedule(hour, minute)` and `BootReceiver` both expect 24h format. `computeCountdown` parses hours as `HOUR_OF_DAY`. Storing 12h format caused silent bugs (AM/PM ignored, wrong alarm times).

**Alternatives Considered:**
- Store both 12h display and 24h computed — adds complexity, two fields to maintain.
- Change all consumers to parse 12h — more touch points, more fragile.

**Consequences:**
- All times in Firestore are now `"HH:MM"` (24h).
- `ProposeAlarmBottomSheet` converts at write time: `"07:30 AM"` → `"07:30"`, `"07:30 PM"` → `"19:30"`.
- `computeCountdown` works correctly for all times.

---

## 2026-07-27 — TribeInfo ID Field

**Decision:** Add `id: String` to `TribeInfo` data class and pass Firestore document ID through the tribe info chain.

**Reason:** Tribe navigation (`TribeDetailScreen`) expects a Firestore document ID as the route argument. Without `id` in `TribeInfo`, the tribe name was being passed as the ID, causing Firestore lookups to fail.

**Alternatives Considered:**
- Store tribe ID in a separate map — adds indirection, harder to maintain.
- Look up tribe by name in Firestore — requires additional query, unreliable if names aren't unique.

**Consequences:**
- `TribeInfo` now carries `id`, `name`, `photoUrl`, `memberCount`.
- All `TribeInfo` construction sites updated to pass `it.id`.
- `onTribeClick(tribe.id)` instead of `onTribeClick(tribe.name)`.

---

## 2026-07-02 — Single Activity Architecture

**Decision:** Use a single Activity (`MainActivity`) with Compose Navigation for all screens.

**Reason:** Simplifies lifecycle management, enables smooth animated transitions between screens, and is the recommended modern Android architecture.

**Alternatives Considered:**
- Multi-Activity — rejected for complexity and transition overhead.
- Fragment-based navigation — rejected in favor of Compose-first approach.

**Consequences:**
- All navigation state lives in the NavHost.
- Splash → Login/Home routing is handled by `if (authState != null)` in the navigation graph.
- Alarm screen is a separate Activity (`AlarmActivity`) because it needs to appear on top of the lock screen.

---

## 2026-07-02 — Dagger Hilt for DI

**Decision:** Use Dagger Hilt with KSP for dependency injection.

**Reason:** Industry standard for Android DI. First-party Firebase support. KSP is faster than kapt.

**Alternatives Considered:**
- Koin — easier setup but runtime DI means errors surface later.
- Manual DI — too verbose for a project of this size.
- Anvil — less mainstream, harder to find help.

**Consequences:**
- All ViewModels use `@HiltViewModel` + `@Inject constructor`.
- Firebase dependencies provided via `FirebaseModule` Hilt module.
- DI graph is compile-time validated.

---

## 2026-07-02 — Firebase as Backend (No Custom API)

**Decision:** Use Firebase (Auth, Firestore, Storage) as the entire backend.

**Reason:** Zero server management, real-time Firestore listeners eliminate polling, Google Sign-In is seamless, and Firebase Auth handles token refresh. Ideal for a startup/portfolio project.

**Alternatives Considered:**
- Custom FastAPI backend — overkill for real-time sync use case; Firestore's real-time listeners are hard to replicate with REST.
- Supabase — good alternative but Firebase's Google Sign-In integration is smoother on Android.

**Consequences:**
- All business logic lives in Android ViewModels (no backend code to maintain).
- Firestore security rules are the only access control — must be carefully configured.
- Heavy reliance on Firebase free tier (Firestore 1GB storage, 50K reads/day).

---

## 2026-07-02 — Sunrise Amber (#FF8C00) Seed Color

**Decision:** Use Sunrise Amber (#FF8C00) as the Material 3 seed color.

**Reason:** Evokes sunrise, energy, warmth — thematically perfect for a wake-up app. Amber is distinctive and memorable.

**Alternatives Considered:**
- Blue — too generic, feels like a utility app.
- Red — too aggressive/alarming.
- Green — too calm, doesn't convey urgency.

**Consequences:**
- Material 3 generates a full tonal palette from the seed color.
- Dynamic color (wallpaper-based) is supported as an opt-in — users who prefer custom theming can override.
- True black dark mode is enforced regardless of seed color (pure #000000 background).

---

## 2026-07-02 — 2-Hour Veto Window

**Decision:** Proposals auto-accept after 2 hours unless vetoed by any member.

**Reason:** Balances responsiveness with fairness — tribe members have a reasonable window to object, but the proposal doesn't hang indefinitely.

**Alternatives Considered:**
- Majority vote — more democratic but adds complexity; veto-only is simpler and ensures any single objection is respected.
- No veto window — proposal always requires explicit confirmation from all members — too slow, alarm never gets set.
- 1-hour window — too short for someone who doesn't check their phone immediately.

**Consequences:**
- `proposals/{proposalId}.expiresAt` is set to `createdAt + 2 hours` on creation.
- Auto-accept is a Firestore scheduled function (or client-side check on query).
- Veto is final — once vetoed, the proposal cannot be re-activated.
