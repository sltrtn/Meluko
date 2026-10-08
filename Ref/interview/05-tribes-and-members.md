# Lesson 5 — Tribes and Members: Real-Time Tribe Data

Files: `ui/SocialViewModel.kt`, `ui/SocialScreen.kt`

## What this lesson covers

- The `SocialUiState` sealed class
- The listener chain: user_alarms → tribe → members
- Live member statuses (Awake / Sleeping / Missed)
- Leaderboard sorting and the awake counter

## The UI state model

```kotlin
sealed class SocialUiState {
    data object Loading : SocialUiState()
    data class Success(
        val members: List<TribeMember>,
        val tribes: List<TribeProgress>
    ) : SocialUiState()
    data class Error(val message: String) : SocialUiState()
}
```

Three explicit states, nothing implicit. Loading is shown while the first snapshot hasn't arrived; Error carries a message for a banner; Success carries everything the screen renders.

## The listener chain — how tribe data flows

The Social screen shows your tribe card plus all members. That data comes from **three chained Firestore listeners**:

```
1. user_alarms/{uid}  → tells us WHICH tribe (primaryTribeId)
        │  (listener fires when the value changes)
        ▼
2. tribes/{tribeId}   → tells us the tribe's name, photo, memberCount
        ▼
3. tribes/{tribeId}/members → tells us every member's status/streak
```

### Step 1 — resolve the tribe
`loadPrimaryTribeMembers(uid)` listens on `user_alarms/{uid}`. When it gets a `primaryTribeId`, it starts the next two listeners. If there's no `primaryTribeId`, it emits an empty Success (user hasn't joined a tribe yet).

### Step 2 — tribe doc
Listens on `tribes/{primaryTribeId}` and builds `TribeProgress(primaryTribeId, name, photoUrl, awakeCount, memberCount)`.

### Step 3 — members
Listens on `tribes/{tribeId}/members`. Each member doc becomes a `TribeMember`, then the list is **sorted by streak descending** (highest streak first — the leaderboard):

```kotlin
val list = snap.documents.mapNotNull { doc ->
    TribeMember(
        name = doc.getString(DISPLAY_NAME) ?: "Unknown",
        photoUrl = doc.getString(PHOTO_URL),
        isYou = doc.id == uid,
        streakDays = doc.getLong(STREAK_DAYS)?.toInt() ?: 0,
        status = when (doc.getString(TODAY_STATUS)) {
            "awake" -> WakeStatus.Awake
            "missed" -> WakeStatus.Missed
            else -> WakeStatus.Sleeping
        },
        wakeTime = doc.getString(TODAY_WAKE_TIME)
    )
}.sortedByDescending { it.streakDays }
```

The `status` field becomes a sealed `WakeStatus` — the UI `when`s over it exhaustively.

### The awake counter
```kotlin
val awakeCount = list.count { it.status == WakeStatus.Awake }
_tribes.value = _tribes.value.map { it.copy(awakeCount = awakeCount) }
```
Count of members already awake today is stamped onto the tribe card — the "2/5 awake" stat that drives competition.

## Why listeners instead of one-shot reads?

- **Real-time tribe sync.** If another member dismisses their alarm, their `todayStatus` flips to `"awake"` and every phone's Social screen updates within ~a second — no polling, no pull-to-refresh.
- **One code path.** The same listener handles initial load and live updates. No separate "fetch" and "refresh" logic.
- **Cost control.** A listener uses the Firestore snapshot cache and only downloads changed docs.

## Listener lifecycle — the critical rule

Every listener is stored in a field and removed in `onCleared()`:

```kotlin
private var userAlarmRegistration: ListenerRegistration? = null
private var tribeRegistration: ListenerRegistration? = null
private var memberRegistration: ListenerRegistration? = null

override fun onCleared() {
    userAlarmRegistration?.remove()
    tribeRegistration?.remove()
    memberRegistration?.remove()
}
```

If you leak a listener, you leak an active connection — the ViewModel stays alive, downloads keep coming, and you can end up with duplicate updates after re-entering the screen.

Before re-registering, the old listener is also removed:

```kotlin
memberRegistration?.remove()
memberRegistration = db.collection(...).addSnapshotListener { ... }
```

This is what prevents stacking listeners when the tribe ID changes.

## Common trap

**"Why does `loadPrimaryTribeMembers` listen on `user_alarms` instead of just reading it once?"**

Strong answer: the primary tribe can change (leave/join). Listening means the chain automatically re-wires when `primaryTribeId` changes — the tribe and member listeners re-register with the new ID. A one-shot read would freeze the screen on the stale tribe.

## Self-check

1. What are the three states of `SocialUiState`, and when does each appear?
2. Walk through the three-listener chain.
3. How does the leaderboard order members?
4. Why is `onCleared()` so important with listeners?
5. How does another member's wake-up reach your phone?

## Code map

| Concept | Location |
|---|---|
| UI state | `ui/SocialViewModel.kt` lines 15–22 |
| Member listener | lines 49–80 |
| Tribe chain | lines 82–120 |
| onCleared | lines 122–127 |
| WakeStatus UI | `ui/SocialScreen.kt` |
