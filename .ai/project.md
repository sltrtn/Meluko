# Meluko — Project Overview

> Living documentation for the Meluko social alarm clock app.

---

## What It Is

Meluko is a **social accountability alarm clock** for Android. Instead of a solitary alarm that you can snooze, Meluko ties your wake-up to a tribe — miss your alarm, and everyone knows.

**Core thesis:** Waking up is hard alone. Meluko turns it into a shared commitment with real social consequences.

---

## Architecture

### App Architecture (Single Activity)

```
MelukoApplication (@HiltAndroidApp)
    └── MainActivity (@AndroidEntryPoint)
         └── MelukoTheme
              └── AppNavGraph (NavHost)
                   ├── SplashScreen
                   ├── LoginScreen
                   ├── OnboardingScreen (3 pages)
                   ├── CreateProfileScreen
                   ├── JoinOrCreateTribeScreen
                   ├── WelcomeBackScreen
                   └── MainScreen (3-tab shell)
                        ├── SocialScreen (tab)
                        ├── HomeScreen (tab)
                        └── AccountScreen (tab)
                             └── TribeDetailScreen
```

### Firebase Data Model

```
users/{userId}
  ├── username: String
  ├── bio: String
  ├── photoUrl: String
  ├── tribeId: String
  ├── awakeStreak: Int
  ├── bestStreak: Int
  ├── totalWakes: Int
  └── createdAt: Timestamp

tribes/{tribeId}
  ├── name: String
  ├── inviteCode: String
  ├── photoUrl: String
  ├── memberCount: Int
  ├── createdBy: String (userId)
  └── createdAt: Timestamp

tribe_members/{tribeId}/members/{userId}
  ├── status: String (awake/sleeping/missed)
  ├── awakeStreak: Int
  ├── bestStreak: Int
  ├── totalWakes: Int
  └── joinedAt: Timestamp

user_alarms/{userId}
  ├── alarmTime: String ("HH:mm")
  ├── tribeId: String
  └── lastUpdated: Timestamp

proposals/{proposalId}
  ├── tribeId: String
  ├── proposedBy: String (userId)
  ├── proposedTime: String ("HH:mm")
  ├── vetoes: Map<String, Boolean>
  ├── expiresAt: Timestamp
  ├── status: String (active/accepted/rejected)
  └── createdAt: Timestamp
```

### Alarm Flow

```
1. Tribe member proposes alarm time (ProposeAlarmBottomSheet)
2. Proposal written to Firestore → triggers notification to all tribe members
3. 2-hour veto window begins
4. Other members see proposal in HomeScreen → can veto (VetoBottomSheet)
5. If any veto: proposal rejected
6. If no veto after 2h: proposal accepted → updates user_alarms for all members
7. AlarmScheduler sets exact alarm via AlarmManager
8. At alarm time: AlarmReceiver fires → AlarmActivity opens (full screen, no dismiss)
9. User must swipe-to-wake (SwipeToWakeButton) to dismiss
10. Dismiss → Firestore status updated to "awake" → streak incremented
```

---

## Major Features

- **Google Sign-In** — Android Credential Manager + Firebase Auth
- **Tribe Management** — Create/join tribes by invite code
- **Shared Alarms** — Propose, veto, auto-accept with 2-hour window
- **Wake Tracking** — Real-time member statuses (awake/sleeping/missed)
- **Streaks** — Awake streaks, best streaks, total wakes
- **Swipe-to-Wake** — Custom swipe gesture on full-screen alarm
- **Expressive UI** — Scatter-letter splash, spring animations, glass-morphism nav bar
- **Dynamic Colors** — Android 12+ wallpaper-based color theming
- **Dark Mode** — True black dark mode (purely AMOLED)

---

## External Services

| Service | Purpose | Auth Method |
|---|---|---|
| Firebase Auth | User authentication | Google ID tokens via Credential Manager |
| Firestore | Real-time database (users, tribes, alarms, proposals) | Firebase Auth rules |
| Firebase Storage | Profile photos, tribe photos | Firebase Auth rules |
| Android AlarmManager | Exact alarm scheduling | System service + runtime permissions |

---

## Key Dependencies

- `androidx.compose.material3` — Material 3 theming
- `androidx.compose.material.icons.extended` — Extended icon set
- `firebase-auth-ktx` — Firebase Authentication
- `firebase-firestore-ktx` — Real-time NoSQL database
- `firebase-storage-ktx` — File uploads
- `hilt-navigation-compose` — Hilt ViewModel scoping
- `coil-compose` — Async image loading
- `androidx.credentials` — Credential Manager (Google Sign-In)
- `androidx.navigation.compose` — Compose Navigation
- `com.google.android.gms:play-services-auth` — Google Sign-In API
