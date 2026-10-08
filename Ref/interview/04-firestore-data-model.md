# Lesson 4 — Firestore Data Model and Security Rules

Files: `data/FirestoreConstants.kt`, `firestore.rules`

## What this lesson covers

- The four top-level collections and the one subcollection
- The purpose of each document and its key fields
- The security rules — who can read/write what, and why
- The "following alarm" model (why two alarm times exist)

## The collections

```
users/{uid}                      → the user's profile
user_alarms/{uid}                → the user's alarm state (one doc per user)
tribes/{tribeId}                 → a tribe
tribes/{tribeId}/members/{uid}   → one member doc per user per tribe
tribes/{tribeId}/proposals/{id}  → alarm time proposals (audit trail, never deleted)
```

All collection/field names are centralized in `FirestoreConstants.kt` — no magic strings scattered through the app.

### users/{uid}
Profile data, document ID = Firebase Auth UID.

| Field | Type | Notes |
|---|---|---|
| `displayName` | string | From Google account |
| `username` | string | Set during onboarding |
| `bio` | string | Optional |
| `email` | string | From Google account |
| `photoUrl` | string/null | Profile photo (Firebase Storage URL) |
| `createdAt` | timestamp | Server timestamp |

### user_alarms/{uid}
One document per user holding everything about their alarm.

| Field | Type | Notes |
|---|---|---|
| `primaryTribeId` | string | The tribe this user follows |
| `followedAlarmTime` | string | 24-hour `"HH:MM"` — the alarm this user actually follows |
| `alarmToneUri` | string | Custom ringtone URI (nullable, default alarm tone) |

**Why `followedAlarmTime` and not just the tribe's `alarmTime`?** The tribe alarm is the shared target; `followedAlarmTime` is what YOUR device actually schedules. They're kept in sync by a listener (see Lesson 5/8), but separating them means a user's alarm survives even if they're mid-transition or the tribe doc changes.

### tribes/{tribeId}
| Field | Type | Notes |
|---|---|---|
| `name` | string | Tribe name |
| `inviteCode` | string | Join-by-code |
| `memberCount` | number | Displayed on tribe card |
| `alarmTime` | string | The tribe's current shared target |
| `createdBy` | string | Founder uid |
| `photoUrl` | string/null | Tribe photo |

### tribes/{tribeId}/members/{uid}
| Field | Type | Notes |
|---|---|---|
| `displayName` | string | |
| `photoUrl` | string/null | |
| `streakDays` | number | Current streak |
| `bestStreak` | number | All-time best |
| `totalWakes` | number | Lifetime count |
| `todayStatus` | string | `"awake"` / `"missed"` / `"sleeping"` |
| `todayWakeTime` | string | e.g. `"7:31 AM"` |
| `rank` | number | Leaderboard rank |
| `vetoesUsed` | number | Total vetoes cast |
| `lastWakeDate` | string | `"yyyy-MM-dd"` — used for streak math |
| `joinedAt` | timestamp | |

### tribes/{tribeId}/proposals/{id}
| Field | Type | Notes |
|---|---|---|
| `proposedBy` | string | Proposer uid |
| `proposedByName` | string | Denormalized for display |
| `proposedTime` | string | `"HH:MM"` being proposed |
| `status` | string | `"pending"` / `"accepted"` / `"vetoed"` |
| `vetoedBy` | array | UIDs who vetoed |
| `vetoCount` | number | |
| `createdAt` | timestamp | |
| `expiresAt` | timestamp | `createdAt + 2h` |

## The security rules

Rules are evaluated **server-side** — a malicious client can't bypass them.

### users and user_alarms — strictest
```js
match /users/{uid} {
  allow read, write: if request.auth != null && request.auth.uid == uid;
}
```
You can only touch **your own** document. No user can read another user's profile.

### tribes — read-open, write-gated
```js
allow read:  if request.auth != null;
allow create: if request.auth != null;
allow update: if request.auth != null && exists(
  /databases/$(database)/documents/tribes/$(tribeId)/members/$(request.auth.uid)
);
allow delete: if false;
```
- **Read is open to any signed-in user** — this is deliberate: to join by invite code you must read tribe docs you're not a member of yet.
- **Create is open** — anyone can start a tribe.
- **Update requires membership** — proven by `exists()` on the member subcollection.
- **Delete is impossible** — `if false` is an explicit hard no.

### members — self-only writes, member reads, no re-joining
```js
allow read: if isTribeMember(tribeId);   // members only
allow create: if isOwner(memberUid)
  && !exists(/databases/$(database)/documents/tribes/$(tribeId)/members/$(memberUid));
allow update: if isOwner(memberUid);
allow delete: if false;
```
The membership check (`isTribeMember`, which wraps `exists()`) blocks the "create a fake member doc to join" attack: to create your member doc you only need to be authenticated, but to see other members' docs you need a real membership that already exists. The `!exists(...)` guard on `create` additionally means a member can't re-run "join" to reset their own doc (streak, `vetoesUsed`, etc.) back to defaults — once you've joined, `create` is closed and only `update` (self-only) applies.

### proposals — member-only, state-machine enforced
```js
function isValidProposalStatusTransition(oldStatus, newStatus) {
  return oldStatus == 'pending' && (newStatus == 'vetoed' || newStatus == 'accepted');
}

allow read: if isTribeMember(tribeId);
allow create: if isTribeMember(tribeId)
  && request.resource.data.proposedBy == request.auth.uid
  && request.resource.data.status == 'pending';
allow update: if isTribeMember(tribeId)
  && isValidProposalStatusTransition(resource.data.status, request.resource.data.status)
  && request.resource.data.proposedBy == resource.data.proposedBy
  && request.resource.data.proposedTime == resource.data.proposedTime;
allow delete: if false;
```
Only actual members can read or propose. Three invariants are enforced **server-side**, not just trusted from the client:
- **`create`** — you can only create a proposal attributed to yourself (`proposedBy == request.auth.uid`), and it must start `pending` (can't be born already `accepted`).
- **`update`** — `isValidProposalStatusTransition` only allows `pending → vetoed` or `pending → accepted`. A vetoed proposal can never be un-vetoed, and an accepted one can't be reopened, purely from the rules — the client-side `isAccepting` guard (Lesson 8) is a *convenience* against double-writes, not the actual security boundary.
- **`update`** also requires `proposedBy` and `proposedTime` to stay unchanged — a member can veto or accept a proposal, but can't rewrite what was proposed or by whom.

Expired proposals are **never deleted** — they're an audit trail (see Lesson 8 for the auto-accept).

## Security model summary

| Resource | Read | Write |
|---|---|---|
| users/{uid} | self only | self only |
| user_alarms/{uid} | self only | self only |
| tribes/{id} | any auth | create: any auth; update: member; delete: no |
| tribes/{id}/members | members | self only |
| tribes/{id}/proposals | members | members (delete: no) |

## Common trap

**"Why is tribe read open to all signed-in users? Isn't that insecure?"**

Strong answer: it's required by the join-by-code flow — a prospective member must read the tribe doc before they have a member doc. Nothing sensitive lives on the tribe doc (no emails, no private data); all personal data is in `users` and `user_alarms`, which are strictly self-only.

## Self-check

1. List the four resilience fields on a member doc and what they mean.
2. What does `allow delete: if false` accomplish and why keep it?
3. Why does a tribe need to be readable by non-members?
4. What's the difference between `alarmTime` and `followedAlarmTime`?
5. Where does `exists(...)` get used and what attack does it block?

## Code map

| Concept | Location |
|---|---|
| Collection/field names | `data/FirestoreConstants.kt` |
| Rules | `firestore.rules` |
| Server timestamp | `AuthViewModel.syncFirestoreAfterAuth` (FieldValue.serverTimestamp) |
