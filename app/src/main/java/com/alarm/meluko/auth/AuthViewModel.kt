package com.alarm.meluko.auth

import android.content.Context
import com.alarm.meluko.alarm.AlarmScheduler
import com.alarm.meluko.alarm.MissedAlarmReceiver
import com.alarm.meluko.data.AlarmCache
import dagger.hilt.android.qualifiers.ApplicationContext
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alarm.meluko.data.FirestoreCollection
import com.alarm.meluko.data.FirestoreField
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

private const val TAG = "AuthViewModel"
private const val FIRESTORE_TIMEOUT_MS = 10_000L
private const val MAX_RETRIES = 2
private const val RETRY_DELAY_MS = 500L

sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data class Success(val displayName: String?, val isNewUser: Boolean) : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val webClientId = AuthConstants.WEB_CLIENT_ID

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val firestore get() = FirebaseFirestore.getInstance()

    // Screens get their own AuthViewModel instance from their nav back stack
    // entry, so a sign-out in one (e.g. Account) must be observed by all of
    // them — including the one AppNavGraph routes on.
    private val authListener = FirebaseAuth.AuthStateListener { auth ->
        if (auth.currentUser == null) {
            // Signed out (or account deleted): this phone must stop ringing for that
            // user, and the next user mustn't inherit their cached tribe/time/tone.
            clearDeviceAlarmState()
            if (_authState.value is AuthState.Success) _authState.value = AuthState.Idle
        }
    }

    private fun clearDeviceAlarmState() {
        AlarmScheduler(appContext).cancel()
        MissedAlarmReceiver.cancel(appContext)
        AlarmCache.clear(appContext)
    }

    init {
        firebaseAuth.addAuthStateListener(authListener)
    }

    override fun onCleared() {
        super.onCleared()
        firebaseAuth.removeAuthStateListener(authListener)
    }

    /**
     * Resilient session check: parallelizes reads, retries on failure,
     * and falls back to trusting Firebase Auth if Firestore is unreachable.
     *
     * Priority: Firebase Auth session is the source of truth for "is logged in".
     * Firestore data determines "is new vs returning" — but if Firestore is down,
     * we assume returning (worst case: user sees onboarding again next time).
     */
    fun checkExistingSession() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            Log.d(TAG, "No Firebase Auth session — showing login")
            _authState.value = AuthState.Idle
            return
        }

        Log.d(TAG, "Firebase Auth session found for ${currentUser.uid}")
        viewModelScope.launch {
            // Phase 1: Try to read Firestore with retries (parallel reads)
            val firestoreResult = readFirestoreWithRetry(currentUser.uid)

            if (firestoreResult != null) {
                // Firestore succeeded — use real data
                _authState.value = AuthState.Success(
                    displayName = currentUser.displayName,
                    isNewUser = !firestoreResult.isReturningUser
                )
                Log.d(TAG, "Session restored from Firestore: isNewUser=${firestoreResult.isReturningUser}")
            } else {
                // Firestore failed after retries — trust Firebase Auth
                // Assume returning user to avoid forcing onboarding on every session restore
                _authState.value = AuthState.Success(
                    displayName = currentUser.displayName,
                    isNewUser = false
                )
                Log.w(TAG, "Firestore unreachable — trusting Firebase Auth, assuming returning user")
            }
        }
    }

    private data class FirestoreResult(val isReturningUser: Boolean)

    private suspend fun readFirestoreWithRetry(uid: String): FirestoreResult? {
        repeat(MAX_RETRIES) { attempt ->
            try {
                val result = withTimeoutOrNull(FIRESTORE_TIMEOUT_MS) {
                    coroutineScope {
                        val userDocDeferred = async {
                            firestore.collection(FirestoreCollection.USERS)
                                .document(uid).get().await()
                        }
                        val alarmsDocDeferred = async {
                            firestore.collection(FirestoreCollection.USER_ALARMS)
                                .document(uid).get().await()
                        }
                        val userDoc = userDocDeferred.await()
                        val alarmsDoc = alarmsDocDeferred.await()

                        FirestoreResult(
                            isReturningUser = userDoc.exists() &&
                                alarmsDoc.getString(FirestoreField.PRIMARY_TRIBE_ID) != null
                        )
                    }
                }
                if (result != null) return result
                Log.w(TAG, "Firestore read timed out (attempt ${attempt + 1}/$MAX_RETRIES)")
            } catch (e: Exception) {
                Log.w(TAG, "Firestore read failed (attempt ${attempt + 1}/$MAX_RETRIES): ${e.message}")
            }
            if (attempt < MAX_RETRIES - 1) delay(RETRY_DELAY_MS * (attempt + 1))
        }
        return null
    }

    /**
     * Full Google Sign-In → Firebase credential exchange flow.
     *
     * After successful Firebase Auth, Firestore operations are fire-and-forget:
     * if they fail, the user is still authenticated and can use the app.
     */
    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val credentialManager = CredentialManager.create(context)
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential

                val googleIdToken = GoogleIdTokenCredential
                    .createFrom(credential.data)
                    .idToken

                // Step: Exchange with Firebase Auth — this is the critical path
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val authResult = withTimeoutOrNull(15_000L) {
                    firebaseAuth.signInWithCredential(firebaseCredential).await()
                }

                if (authResult == null) {
                    _authState.value = AuthState.Error("Sign-in timed out — check your connection")
                    return@launch
                }

                val user = authResult.user
                if (user == null) {
                    _authState.value = AuthState.Error("Sign-in failed: no user returned")
                    return@launch
                }

                Log.d(TAG, "Firebase Auth success for ${user.uid}")

                // Step: Firestore operations (non-blocking, best-effort)
                val isNewUser = syncFirestoreAfterAuth(user.uid, user.displayName, user.email)

                _authState.value = AuthState.Success(
                    displayName = user.displayName,
                    isNewUser = isNewUser
                )

            } catch (e: NoCredentialException) {
                Log.d(TAG, "No Google credentials available: ${e.message}")
                _authState.value = AuthState.Error("No Google account found on this device")
            } catch (e: GetCredentialException) {
                Log.d(TAG, "Credential picker cancelled: ${e.message}")
                _authState.value = AuthState.Error("Sign-in cancelled: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Sign-in failed: ${e.message}", e)
                _authState.value = AuthState.Error("Sign-in failed: ${e.message}")
            }
        }
    }

    /**
     * Sync user data to Firestore after successful auth.
     * Returns true if the user appears to be new (no existing Firestore data).
     * If Firestore is unreachable, defaults to new user.
     */
    private suspend fun syncFirestoreAfterAuth(
        uid: String,
        displayName: String?,
        email: String?
    ): Boolean {
        return try {
            withTimeoutOrNull(FIRESTORE_TIMEOUT_MS) {
                // 1. Create the user doc only if it doesn't exist yet. Writing defaults with
                //    merge on every sign-in would wipe the user's photo, username and bio.
                try {
                    val userRef = firestore.collection(FirestoreCollection.USERS).document(uid)
                    if (!userRef.get().await().exists()) {
                        userRef.set(
                            mapOf(
                                FirestoreField.DISPLAY_NAME to displayName,
                                FirestoreField.EMAIL to email,
                                FirestoreField.PHOTO_URL to null,
                                FirestoreField.USERNAME to "",
                                FirestoreField.BIO to "",
                                FirestoreField.CREATED_AT to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "User doc write failed (non-critical): ${e.message}")
                }

                // 2. Read both docs in parallel AFTER the write has completed
                coroutineScope {
                    val userDocDeferred = async {
                        firestore.collection(FirestoreCollection.USERS)
                            .document(uid).get().await()
                    }
                    val alarmsDocDeferred = async {
                        firestore.collection(FirestoreCollection.USER_ALARMS)
                            .document(uid).get().await()
                    }

                    val userDoc = userDocDeferred.await()
                    val alarmsDoc = alarmsDocDeferred.await()

                    val isReturningUser = userDoc.exists() &&
                        alarmsDoc.getString(FirestoreField.PRIMARY_TRIBE_ID) != null

                    !isReturningUser
                }
            } ?: true // timeout = assume new user
        } catch (e: Exception) {
            Log.w(TAG, "Firestore sync failed (non-critical): ${e.message}")
            true // default to new user
        }
    }

    /**
     * DEBUG-only: anonymous sign-in + a self-owned tribe/profile seeded with
     * realistic data, so the real app UI (Home/Social/Account/TribeDetail)
     * can be exercised on an emulator with no Google account and no manual
     * data entry. Never reachable from a release build — gated by
     * BuildConfig.DEBUG at the call site (LoginScreen).
     */
    fun debugPreviewSignIn() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val authResult = firebaseAuth.signInAnonymously().await()
                val uid = authResult.user?.uid
                if (uid == null) {
                    _authState.value = AuthState.Error("Preview sign-in failed: no user")
                    return@launch
                }

                firestore.collection(FirestoreCollection.USERS).document(uid).set(
                    mapOf(
                        FirestoreField.DISPLAY_NAME to "Preview User",
                        FirestoreField.USERNAME to "previewuser",
                        FirestoreField.BIO to "Just here to check the UI.",
                        FirestoreField.PHOTO_URL to null,
                        FirestoreField.NOTIFICATIONS_ENABLED to true
                    )
                ).await()
                // Same rule-checked path as a real user creating a tribe (unique invite code).
                val tribeId = com.alarm.meluko.data.TribeActions.createTribe(
                    firestore, uid, "Preview User", null, "Preview Tribe", "Debug preview data"
                )

                Log.d(TAG, "Debug preview seeded for uid=$uid, tribeId=$tribeId")
                _authState.value = AuthState.Success(displayName = "Preview User", isNewUser = false)
            } catch (e: Exception) {
                Log.e(TAG, "Preview sign-in failed: ${e.message}", e)
                _authState.value = AuthState.Error("Preview sign-in failed: ${e.message}")
            }
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
        _authState.value = AuthState.Idle
    }

    /** Allow user to retry after an error without killing the app */
    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Idle
        }
    }
}
