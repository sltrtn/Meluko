# Lesson 7 — Reliability and Offline: The Alarm Survives Anything

Files: `alarm/BootReceiver.kt`, `data/AlarmCache.kt`, `util/ConnectivityObserver.kt`, `ui/HomeViewModel.kt`

## What this lesson covers

- Why an alarm app needs offline reliability (unlike a chat app)
- The local cache fallback chain in `BootReceiver`
- The reactive connectivity Flow and the offline banner
- Cache-first loading in `HomeViewModel`

## The problem

A chat app can degrade gracefully offline. An **alarm app cannot** — if the alarm doesn't fire, the product failed. But `AlarmReceiver` and `BootReceiver` run at the system level with no guarantee of network access:

- A phone rebooted at 3am may have no data connection yet.
- Firestore may be unreachable.
- The signed-in session may be cached-only.

So the alarm time must be **stored on-device**. That's `AlarmCache`.

## AlarmCache — the offline source of truth

```kotlin
object AlarmCache {
    private const val PREFS_NAME = "meluko_alarm_cache"
    // keys: alarm_time, tribe_id, alarm_tone_uri

    fun saveAlarmTime(context: Context, time: String)
    fun getAlarmTime(context: Context): String?
    fun saveTribeId(context: Context, tribeId: String)
    fun getTribeId(context: Context): String?
    fun saveAlarmToneUri(context: Context, uri: String)
    fun getAlarmToneUri(context: Context): String?
    fun clear(context: Context)
}
```

A `SharedPreferences` layer storing just the three facts the system layer needs. **Firestore is the source of truth; cache is the fallback** — not the other way around. The cache is written whenever the time changes (HomeViewModel listener) and read whenever a receiver needs the time fast.

## BootReceiver — the reboot fallback chain

On `ACTION_BOOT_COMPLETED`, Android delivers a broadcast so apps can restore alarms (AlarmManager alarms are lost on reboot). The real chain has **two layers of fallback** — one for auth not being restored yet, one for Firestore being unreachable:

```
1. FirebaseAuth.currentUser already non-null (rare right after boot, but possible)?
   └─ YES → go straight to step 3 with that uid
2. NO → register an AuthStateListener and wait (up to 5s) for Firebase Auth to
   restore its persisted session from disk
   ├─ Listener fires with a uid before timeout → go to step 3
   └─ 5s timeout elapses, still no user      → read AlarmCache directly, schedule
                                                 or give up, done
3. Try Firestore: user_alarms/{uid}.get()
   ├─ SUCCESS + has alarmTime  → schedule, refresh cache
   ├─ SUCCESS + no alarmTime   → fall back to AlarmCache
   └─ FAILURE (offline)        → fall back to AlarmCache
4. No cache at all → log and skip
```

```kotlin
val pendingResult = goAsync()      // receivers have ~10s; extend with goAsync
val auth = FirebaseAuth.getInstance()
val currentUser = auth.currentUser

if (currentUser != null) {
    loadAlarmAndSchedule(context, currentUser.uid, pendingResult)
    return
}

// FirebaseAuth may still be restoring the persisted user immediately after boot —
// wait briefly for an auth-state change instead of assuming "signed out".
var listener: FirebaseAuth.AuthStateListener? = null
val timeoutHandler = Handler(Looper.getMainLooper())

listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
    val uid = firebaseAuth.currentUser?.uid
    if (uid != null) {
        timeoutHandler.removeCallbacksAndMessages(null)
        listener?.let { auth.removeAuthStateListener(it) }
        loadAlarmAndSchedule(context, uid, pendingResult)
    }
}
auth.addAuthStateListener(listener)

timeoutHandler.postDelayed({
    auth.removeAuthStateListener(listener)
    val cachedTime = AlarmCache.getAlarmTime(context)   // auth never restored — cache is all we have
    if (cachedTime != null) scheduleFromCachedTime(context, cachedTime)
    pendingResult.finish()
}, AUTH_TIMEOUT_MS)   // 5_000L

// loadAlarmAndSchedule(context, uid, pendingResult) — step 3:
FirebaseFirestore.getInstance()
    .collection(USER_ALARMS).document(uid).get()
    .addOnSuccessListener { doc ->
        val alarmTime = doc.getString(FOLLOWED_ALARM_TIME)
        if (alarmTime != null) {
            scheduleFromCachedTime(context, alarmTime)
            AlarmCache.saveAlarmTime(context, alarmTime)   // refresh cache
        } else {
            val cachedTime = AlarmCache.getAlarmTime(context)
            if (cachedTime != null) scheduleFromCachedTime(context, cachedTime)
        }
        pendingResult.finish()
    }
    .addOnFailureListener {
        val cachedTime = AlarmCache.getAlarmTime(context)
        if (cachedTime != null) scheduleFromCachedTime(context, cachedTime)
        pendingResult.finish()
    }
```

**Why does the auth layer exist at all?** `FirebaseAuth.currentUser` reads a value that Firebase itself restores asynchronously from disk on cold start — right after a reboot, the very first broadcast delivered to the app can race Firebase's own session restore. Treating `currentUser == null` as "signed out" at that exact moment would silently skip straight to the cache every single reboot, even for a fully signed-in user with fresh Firestore data. The `AuthStateListener` + 5s timeout gives Firebase a real chance to restore the session before falling back — the cache is the last resort, not the first one.

Two important techniques:

### `goAsync()` — the receiver time budget
A `BroadcastReceiver` is killed ~10 seconds after `onReceive` returns. `goAsync()` tells Android "this is doing real work" and lets the code finish asynchronously. Between the up-to-5s auth wait and the Firestore round-trip, the total path can approach that ~10s budget, which is exactly why `goAsync()` is required here and why the auth timeout is tuned to 5s and not, say, 8-9s — it has to leave room for the Firestore call after it.

### Greedy parsing with defaults
```kotlin
val hour = parts.getOrElse(0) { "7" }.toIntOrNull() ?: 7
val minute = parts.getOrElse(1) { "30" }.toIntOrNull() ?: 30
```
A malformed string can't crash the boot path — it degrades to 07:30. The boot path must never throw.

## ConnectivityObserver — reactive network status

```kotlin
@Singleton
class ConnectivityObserver @Inject constructor(@ApplicationContext context: Context) {
    val isOnline: Flow<Boolean> = callbackFlow {
        val callback = object : NetworkCallback() {
            override fun onAvailable(network) = trySend(true)
            override fun onLost(network) = trySend(false)
            override fun onCapabilitiesChanged(network, capabilities) {
                trySend(capabilities.hasCapability(NET_CAPABILITY_INTERNET))
            }
        }
        connectivityManager.registerNetworkCallback(request, callback)

        // emit initial state immediately
        trySend(currentCapabilities?.hasCapability(NET_CAPABILITY_INTERNET) == true)

        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }.conflate()
}
```

- Wraps `ConnectivityManager.NetworkCallback` in a coroutines `callbackFlow` — **push-based**, no polling.
- `onCapabilitiesChanged` is the important callback: WiFi connected ≠ internet (no-captive-portal network). It re-checks `NET_CAPABILITY_INTERNET`.
- Emits an **initial state** on subscribe so consumers don't wait for the first change.
- `.conflate()` drops intermediate values — you only care about the latest online/offline state, not every transition.
- Hilt `@Singleton` + `@ApplicationContext` so one instance is shared app-wide.

## HomeViewModel — cache-first, then Firestore

The home screen is instant even before Firestore answers:

```kotlin
init {
    // 1. Cache-first: show something immediately
    AlarmCache.getAlarmTime(context)?.let { _nextAlarmTime.value = it }
    AlarmCache.getTribeId(context)?.let { _primaryTribeId.value = it }

    // 2. Connectivity monitoring
    viewModelScope.launch {
        connectivityObserver.isOnline.collect { _isOnline.value = it }
    }

    // 3. Live listener
    userAlarmRegistration = db.collection(USER_ALARMS).document(uid)
        .addSnapshotListener { snapshot, error ->
            ...
            _nextAlarmTime.value = time
            AlarmCache.saveAlarmTime(context, time)   // cache on every update
            scheduleAlarm(time)
        }
}
```

The flow: cached value renders instantly → live listener replaces it with the real value → the cache is refreshed → the alarm is scheduled. Offline, you still see your alarm time and the offline banner. When the network returns, `_isOnline` flips and the banner dismisses — Firestore's snapshot cache makes the listener reconnect automatically.

### The offline banner
```kotlin
if (!isOnline) {
    // AnimatedVisibility + errorContainer card: "You're offline. Alarm still set."
}
```
The banner tells the user the alarm is still armed — which is true, because of AlarmCache + BootReceiver. That's the whole point of this lesson.

## Why this architecture

| Concern | Solution |
|---|---|
| Alarm must survive reboot | BootReceiver + AlarmCache |
| Alarm must survive force-kill | Reschedule-on-fire (Lesson 6) |
| Alarm must survive offline reboot | Firestore first, cache fallback |
| Alarm must survive a reboot mid-auth-restore | AuthStateListener + 5s timeout before trusting "signed out" |
| A user who never dismisses shouldn't keep their streak | MissedAlarmReceiver, +10min grace check |
| User must see state instantly | Cache-first loading |
| User must know network status | ConnectivityObserver Flow |
| No polling | callbackFlow + NetworkCallback |

## Common trap

**"What's the difference between the on-device cache and Firestore's built-in snapshot cache?"**

Strong answer: Firestore's cache holds **query results** for listeners — great for reads while offline, but you can't reliably schedule an alarm by reading a snapshot cache from a broadcast receiver before any listener has run. `AlarmCache` is a deliberate, tiny, dependency-free store of exactly the three values the system layer needs, readable synchronously in any process. Firestore remains the source of truth; the prefs cache is the last-resort fallback.

## Self-check

1. Why can't an alarm app be gracefully degraded offline like a chat app?
2. Walk the BootReceiver fallback chain for: (a) signed out, (b) offline, (c) Firestore returns no doc.
3. What is `goAsync()` protecting, exactly?
4. Why `onCapabilitiesChanged` instead of just `onAvailable`?
5. What does the HomeViewModel show the user while offline, and why is it truthful?
6. Why can't BootReceiver treat `FirebaseAuth.currentUser == null` as "signed out" the instant it fires, and how does the 5s `AuthStateListener` timeout fix that?
7. What does MissedAlarmReceiver add on top of reschedule-on-fire, and why is reschedule-on-fire alone not enough to enforce "no snooze"?

## Code map

| Concept | Location |
|---|---|
| Cache layer | `data/AlarmCache.kt` |
| Reboot chain | `alarm/BootReceiver.kt` lines 22–60 |
| AuthStateListener + timeout | lines 36–59 |
| Firestore-then-cache load | `loadAlarmAndSchedule`, lines 62–94 |
| goAsync + defaults | line 25, lines 96–101 |
| Missed-alarm grace check | `alarm/MissedAlarmReceiver.kt` |
| Connectivity Flow | `util/ConnectivityObserver.kt` |
| Cache-first init | `ui/HomeViewModel.kt` lines 63–79 |
| Offline banner | `ui/HomeScreen.kt` |
