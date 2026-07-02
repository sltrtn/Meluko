# Meluko — Architectural Decisions

> Every important decision with date, reason, alternatives, and consequences.

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

---

## 2026-07-02 — Clash Display Fonts Bundled in APK

**Decision:** Bundle Clash Display OTF fonts (6 weights) and Inter TTF (2 weights) in the APK resources rather than downloading them.

**Reason:** Instant availability on first launch. No network dependency for rendering core UI text. Fonts are small (~2MB total).

**Alternatives Considered:**
- Google Fonts download — requires network, adds latency on first render.
- System fonts only — too limited, Clash Display is central to the brand identity.

**Consequences:**
- APK size increases by ~2MB.
- Font files are in `res/font/` and defined in `Type.kt` via `FontFamily`.
- All typography roles use Clash Display with Inter as fallback.
