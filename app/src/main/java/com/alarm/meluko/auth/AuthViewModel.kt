package com.alarm.meluko.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Sealed class representing every possible auth state.
 *
 * Why sealed? Because when we `when(state)` in the UI, the compiler forces
 * us to handle every branch — no silent "else" fallthrough bugs.
 *
 * Space: O(1) — just a reference on the StateFlow
 */
sealed class AuthState {
    /** First launch, no auth operation in progress yet */
    data object Idle : AuthState()
    /** Sign-in sheet is open / Firebase credential exchange in flight */
    data object Loading : AuthState()
    /** User is authenticated — carry their display name forward */
    data class Success(val displayName: String?, val isNewUser: Boolean) : AuthState()
    /** Something went wrong — surface the message to the UI */
    data class Error(val message: String) : AuthState()
}

/**
 * @HiltViewModel → Hilt knows how to construct this and inject FirebaseAuth.
 * The @Inject constructor tells Hilt what to provide.
 *
 * Design decision: FirebaseAuth is injected (not created inline) so we can
 * swap a fake in unit tests without touching ViewModel code.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    // Web Client ID from google-services.json (client_type: 3)
    // This is the OAuth 2.0 client that identifies our app to Google's servers
    private val webClientId =
        "1049841099865-q6ube9e1obc4b8vslu5dekqlb776k052.apps.googleusercontent.com"

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)

    /**
     * Exposed as read-only StateFlow — the UI can collect it but never mutate it.
     * StateFlow always holds the latest value (unlike a Channel/Flow that can miss emissions).
     */
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    /**
     * Check on startup: if Firebase already has a session, skip the login screen.
     *
     * We do NOT trust Firebase Auth alone — a user's Firestore data may have been
     * deleted while their Auth session was still alive. So we check:
     *   1. `users/{uid}` document exists in Firestore
     *   2. `user_alarms/{uid}` has a `primaryTribeId` field
     * Only when BOTH conditions are true do we treat this as a returning user.
     * Otherwise we route them through onboarding.
     */
    fun checkExistingSession() {
        val currentUser = firebaseAuth.currentUser ?: return
        viewModelScope.launch {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val uid = currentUser.uid

                val userDoc = firestore.collection("users").document(uid).get().await()
                val alarmsDoc = firestore.collection("user_alarms").document(uid).get().await()

                // A fully-onboarded returning user must have both documents.
                // `primaryTribeId` being present (and non-null) signals completed onboarding.
                val isReturningUser = userDoc.exists() &&
                    alarmsDoc.getString("primaryTribeId") != null

                _authState.value = AuthState.Success(
                    displayName = currentUser.displayName,
                    isNewUser = !isReturningUser
                )
            } catch (e: Exception) {
                // Network failure, permissions error, etc. — fail safe to login screen.
                _authState.value = AuthState.Error("Session check failed: ${e.message}")
            }
        }
    }

    /**
     * Full Google Sign-In → Firebase credential exchange flow.
     *
     * Step 1: Credential Manager shows the Google account bottom sheet
     * Step 2: We extract the ID token from the selected account
     * Step 3: Exchange that token with Firebase Auth for a FirebaseUser
     *
     * Everything runs inside viewModelScope so it's automatically cancelled
     * if the user navigates away — no memory leaks.
     *
     * @param context  needed by CredentialManager to anchor the bottom sheet to the Activity
     */
    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            try {
                // Step 1: Configure what credential type we want
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                // Step 2: Launch the bottom sheet and wait for selection
                val credentialManager = CredentialManager.create(context)
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential

                // Step 3: Parse the Google ID token from the credential
                val googleIdToken = GoogleIdTokenCredential
                    .createFrom(credential.data)
                    .idToken

                // Step 4: Exchange with Firebase — .await() is the Kotlin coroutine
                // extension on Firebase Tasks so we don't need callbacks
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()
                
                val user = authResult.user
                if (user != null) {
                    val firestore = FirebaseFirestore.getInstance()
                    val uid = user.uid

                    // Persist/merge the user profile. `username` and `bio` are written as
                    // empty strings on first sign-in so downstream reads never see missing fields.
                    // SetOptions.merge() means we never overwrite data the user already filled in.
                    firestore.collection("users").document(uid)
                        .set(
                            mapOf(
                                "displayName" to user.displayName,
                                "email" to user.email,
                                "photoUrl" to null,
                                "username" to "",
                                "bio" to "",
                                "createdAt" to FieldValue.serverTimestamp()
                            ),
                            SetOptions.merge()
                        ).await()

                    // Determine new-vs-returning purely from Firestore data, NOT from
                    // additionalUserInfo.isNewUser — that flag is unreliable when Firestore
                    // records have been deleted while the Auth account still exists.
                    val alarmsDoc = firestore.collection("user_alarms").document(uid).get().await()
                    val userDoc = firestore.collection("users").document(uid).get().await()

                    val isReturningUser = userDoc.exists() &&
                        alarmsDoc.getString("primaryTribeId") != null

                    _authState.value = AuthState.Success(
                        displayName = user.displayName,
                        isNewUser = !isReturningUser
                    )
                }

            } catch (e: GetCredentialException) {
                // User cancelled the picker or no accounts on device
                _authState.value = AuthState.Error("Sign-in cancelled: ${e.message}")
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Sign-in failed: ${e.message}")
            }
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
        _authState.value = AuthState.Idle
    }
}
