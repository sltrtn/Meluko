# Lesson 3 — Auth: Google Sign-In with Credential Manager

File: `auth/AuthViewModel.kt`, `LoginScreen.kt`

## What this lesson covers

- The modern Google Sign-In flow (Credential Manager — not the legacy Google Sign-In SDK)
- The `AuthState` sealed class and what each state means
- The resilient session check (parallel reads, retries, timeouts, fallback)
- Why Firestore is read in parallel and what "new vs returning" really means

## The modern sign-in flow

```kotlin
val googleIdOption = GetGoogleIdOption.Builder()
    .setFilterByAuthorizedAccounts(false)   // show all Google accounts, not just signed-in ones
    .setServerClientId(webClientId)          // our OAuth client ID
    .setAutoSelectEnabled(false)
    .build()

val request = GetCredentialRequest.Builder()
    .addCredentialOption(googleIdOption)
    .build()

val credentialManager = CredentialManager.create(context)
val result = credentialManager.getCredential(context, request)
val googleIdToken = GoogleIdTokenCredential.createFrom(result.credential.data).idToken

// Exchange the Google ID token for Firebase Auth
val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
firebaseAuth.signInWithCredential(firebaseCredential).await()
```

Step by step:
1. Build a `GetGoogleIdOption` with the app's OAuth **web client ID** (from `google-services.json`).
2. Hand it to the system **Credential Manager** — Android shows the Google account sheet.
3. User picks an account → you get a Google **ID token** (a JWT proving their identity).
4. Send that token to Firebase Auth via `GoogleAuthProvider` → Firebase verifies it and returns a Firebase user.

**Why Credential Manager and not the old `GoogleSignInClient`?** Credential Manager is the modern, unified API Google recommends — one flow for passwords, passkeys, and Google accounts, with no `onActivityResult` callbacks. The old SDK is deprecated.

## The AuthState sealed class

```kotlin
sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data class Success(val displayName: String?, val isNewUser: Boolean) : AuthState()
    data class Error(val message: String) : AuthState()
}
```

Each state maps to a screen decision:
- **Idle** → LoginScreen
- **Loading** → keep showing current screen
- **Success** → onboarding (new user) or welcome-back → home (returning)
- **Error** → LoginScreen with an error banner + RETRY button

A sealed class forces you to handle every state — no silent fall-through.

## The resilient session check

When the app opens, `checkExistingSession()` runs:

```kotlin
if (firebaseAuth.currentUser == null) {
    _authState.value = AuthState.Idle   // no session → login
    return
}
// else: session exists → figure out new vs returning
```

**The critical insight:** Firebase Auth says "is someone signed in," but it does NOT say "is this user new to Meluko." That question lives in Firestore data. The logic:

> A user is **returning** if they have both a `users` doc AND a `primaryTribeId` in their `user_alarms` doc. Otherwise they're **new**.

Why both? A signed-in user whose Firestore data was lost (e.g. Firestore wiped) should be treated as returning only if they actually completed setup (picked a tribe). If the read fails, assume returning — because the worst case is the user sees onboarding once more.

### The three resilience techniques

1. **Parallel reads** — `coroutineScope` + two `async` blocks read `users` and `user_alarms` at the same time instead of sequentially:

```kotlin
coroutineScope {
    val userDocDeferred = async { usersDoc.get().await() }
    val alarmsDocDeferred = async { alarmsDoc.get().await() }
    val userDoc = userDocDeferred.await()
    val alarmsDoc = alarmsDocDeferred.await()
}
```

2. **Retries with backoff** — up to 2 retries, waiting 500ms then 1000ms. Network blips are common on app start.

3. **Timeout** — `withTimeoutOrNull(10_000)` prevents the check hanging forever on a dead connection.

4. **Fallback** — if Firestore is completely unreachable after retries, **trust Firebase Auth** and assume returning user. The user gets in; worst case they redo onboarding once.

### The sign-in timeout

The Google credential exchange (the critical path) gets its own `withTimeoutOrNull(15_000)`. If the network stalls there, the user sees "Sign-in timed out — check your connection" instead of a spinner forever.

## Best-effort Firestore sync

After auth succeeds, `syncFirestoreAfterAuth` writes the `users` doc and re-reads to decide new-vs-returning. Crucially it's wrapped so **auth is never blocked by Firestore**:

```kotlin
try {
    withTimeoutOrNull(FIRESTORE_TIMEOUT_MS) { ... } ?: true  // timeout → assume new
} catch (e: Exception) { true }
```

The user is authenticated and can use the app even if every Firestore write fails.

## Error handling + retry

- `GetCredentialException` → "Sign-in cancelled" (user backed out of the sheet — not a real error).
- Other exceptions → the message is surfaced in an error banner.
- `clearError()` resets `Error` → `Idle` so RETRY works without a restart.
- `signOut()` signs out of Firebase Auth and flips state back to Idle.

## Common trap

**"What happens if the user signs in but Firestore rules block the write?"**

Strong answer: the write is best-effort — a failed write logs a warning and continues. Auth isn't held hostage by Firestore. The user is marked `isNewUser = true`, routed to onboarding, and the write retries there.

## Self-check

1. What is a Google ID token and why is it exchanged for Firebase Auth?
2. What does `isNewUser` actually mean, and what data decides it?
3. Name the four resilience techniques in `checkExistingSession`.
4. Why is the credential exchange wrapped in a 15s timeout but Firestore sync isn't blocking?
5. What does `clearError()` enable in the UI?

## Code map

| Concept | Location |
|---|---|
| Auth state | `auth/AuthViewModel.kt` lines 36–41 |
| Session check | lines 64–94 |
| Parallel reads + retry | lines 98–128 |
| Google sign-in | lines 136–194 |
| Firestore sync | lines 201–253 |
| Error banner + retry | `ui/LoginScreen.kt` |
