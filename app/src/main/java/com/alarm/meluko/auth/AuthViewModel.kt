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
    data class Success(val displayName: String?) : AuthState()
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
     * Time: O(1) — local memory check, no network call.
     */
    fun checkExistingSession() {
        if (firebaseAuth.currentUser != null) {
            _authState.value = AuthState.Success(firebaseAuth.currentUser?.displayName)
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
                    .setFilterByAuthorizedAccounts(false) // show ALL Google accounts on device
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(false) // don't silently sign in — show picker
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

                _authState.value = AuthState.Success(
                    authResult.user?.displayName
                )

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
