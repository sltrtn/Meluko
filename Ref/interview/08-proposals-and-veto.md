# Lesson 8 — Proposals and Veto: The Democracy Loop

Files: `ui/ProposalViewModel.kt`, `ui/ProposeAlarmBottomSheet.kt`, `ui/VetoBottomSheet.kt`

## What this lesson covers

- The proposal lifecycle: pending → accepted | vetoed
- The 2-hour veto window and auto-accept
- The `AlarmProposal` model
- Atomic writes with Firestore batches
- The `isAccepting` guard against duplicate batches

## The rule

> Any tribe member can propose a new alarm time. Members have **2 hours** to veto. **One veto kills the proposal** forever. If nobody vetoes, the proposal auto-accepts and becomes the tribe's new alarm.

This is the social engine of the app. The proposal is a subcollection document: `tribes/{tribeId}/proposals/{proposalId}`.

## The model

```kotlin
data class AlarmProposal(
    val id: String,
    val proposedBy: String,
    val proposedByName: String,
    val proposedTime: String,
    val status: String,          // pending / accepted / vetoed
    val vetoedBy: List<String>,
    val vetoCount: Int,
    val createdAt: Date?,
    val expiresAt: Date?
)
```

## The proposal listener — includes the auto-accept logic

`listenToTribeProposals(tribeId)` listens on the whole `proposals` subcollection:

```kotlin
addSnapshotListener { snap, error ->
    ...
    // 1. Keep only pending proposals
    val pending = snap.documents.filter { it.getString(STATUS) == "pending" }
    if (pending.isEmpty()) { _activeProposal.value = null; return }

    // 2. Pick the newest
    val doc = pending.maxByOrNull { it.getDate(CREATED_AT)?.time ?: 0L }

    // 3. Expired? → auto-accept (once)
    if (expiresAt != null && now.after(expiresAt)) {
        if (!isAccepting) {
            isAccepting = true
            viewModelScope.launch {
                acceptProposal(tribeId, doc.id, proposedTime)
                isAccepting = false
            }
        }
        return
    }

    // 4. Otherwise surface it to the UI
    _activeProposal.value = AlarmProposal(...)
}
```

Key details:
- **"Newest pending" wins.** Multiple pending proposals can exist; the UI only ever shows the latest.
- **Auto-accept is triggered by the listener itself.** When a proposal passes `expiresAt`, this client writes the acceptance. This works without a server because the listener fires whenever the snapshot changes (the expiry timestamp is what changes).
- **`isAccepting` is a re-entrancy guard.** The snapshot listener can fire repeatedly (each snapshot that crosses the deadline) — without the guard, `acceptProposal` could run twice. The guard plus the fact that `acceptProposal` sets status to `accepted` means the second fire finds no pending doc.

## acceptProposal — the atomic batch

Acceptance touches **three documents** that must change together — that's what a Firestore batch is for:

```kotlin
val batch = db.batch()

batch.update(proposalDoc, STATUS, "accepted")                      // 1. close the proposal
batch.update(tribeDoc, ALARM_TIME, proposedTime)                    // 2. update tribe target
if (uid.isNotEmpty()) {
    batch.update(userAlarmsDoc(uid), FOLLOWED_ALARM_TIME, proposedTime)  // 3. this user follows it
}

batch.commit().await()
```

**Why a batch and not three separate updates?** A batch is atomic — either all three writes succeed or none do. If the tribe time updated but the proposal stayed pending, the UI would show a contradiction. The batch also cuts round-trips from three to one.

Note the *client-side* atomicity: each user's `followedAlarmTime` is updated by whichever device detects the expiry. That's fine for this scale — the listener chain (Lesson 5) re-syncs everyone. And `HomeViewModel`'s tribe listener does the same for members not on the expiry path:

```kotlin
if (tribeAlarm != null && tribeAlarm != time) {
    db.collection(USER_ALARMS).document(currentUid)
        .update(FOLLOWED_ALARM_TIME, tribeAlarm)   // self-healing sync
}
```

## castVeto — the one-kill

```kotlin
db.collection(TRIBES).document(tribeId)
    .collection(PROPOSALS).document(proposalId)
    .update(
        STATUS to "vetoed",
        VETOED_BY to FieldValue.arrayUnion(uid),     // append uid
        VETO_COUNT to FieldValue.increment(1)        // atomic increment
    ).await()

db.collection(TRIBES).document(tribeId)
    .collection(MEMBERS).document(uid)
    .update(VETOES_USED, FieldValue.increment(1)).await()
```

- **`arrayUnion`** appends the uid — no need to read-modify-write the list (and no race where two vetos overwrite each other).
- **`increment`** is atomic — two simultaneous vetoes can't both read "0" and write "1".
- Once `status` is `"vetoed"`, the proposal listener's `pending` filter drops it → the veto sheet closes.

## createProposal

```kotlin
val proposalId = UUID.randomUUID().toString()
val expiresAt = Date(now.time + (2 * 60 * 60 * 1000L))   // now + 2h

db.collection(TRIBES).document(tribeId)
    .collection(PROPOSALS).document(proposalId)
    .set(mapOf(
        PROPOSED_BY to uid,
        PROPOSED_BY_NAME to displayName,      // denormalized for display
        PROPOSED_TIME to proposedTime,
        STATUS to "pending",
        VETOED_BY to emptyList<String>(),
        VETO_COUNT to 0,
        CREATED_AT to FieldValue.serverTimestamp(),   // server clock, not device clock
        EXPIRES_AT to expiresAt
    )).await()
```

- `createdAt` uses a **server timestamp** — the server's clock is authoritative, so expiry math isn't corrupted by a device with the wrong time.
- The user's `displayName` is denormalized into the proposal so the UI can show "X proposed 7:30" without a second read.
- Empty-uid guard at the top: if not signed in, surface an error instead of throwing.

## The time picker

`ProposeAlarmBottomSheet` uses a `WheelPicker` (hour + minute) — the built-in Android time picker replaced because the custom wheel matches the design system. It validates before writing (hours 0–23, minutes 0–59) and renders the selection as a `"HH:MM"` string — the canonical 24-hour format everywhere in the data layer.

## Common trap

**"Why is auto-accept done client-side instead of with a Firestore Cloud Function or a scheduled trigger?"**

Strong answer: a client listener is free and near-real-time, and for a social alarm at this scale, whichever member's app happens to be running crosses the deadline and commits the batch. It's deterministic enough because the `status` update makes the batch idempotent. A Cloud Function would add a deploy step, a billing dependency, and server code for behavior the client already handles correctly with the `isAccepting` guard.

## Self-check

1. Walk the full lifecycle of a proposal, from creation to acceptance.
2. What exactly does `isAccepting` prevent, and why can the listener fire repeatedly?
3. Why is the batch atomic and what are its three writes?
4. How do `arrayUnion` and `increment` prevent race conditions in vetoes?
5. Why is `createdAt` a server timestamp but `expiresAt` computed client-side?

## Code map

| Concept | Location |
|---|---|
| Model | `ui/ProposalViewModel.kt` lines 22–32 |
| Listener + auto-accept | lines 60–131 |
| createProposal | lines 133–174 |
| castVeto | lines 176–209 |
| acceptProposal batch | lines 211–239 |
| Time picker | `ui/ProposeAlarmBottomSheet.kt` |
