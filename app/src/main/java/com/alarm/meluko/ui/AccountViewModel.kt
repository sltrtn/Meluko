package com.alarm.meluko.ui

import android.content.Context
import com.alarm.meluko.data.AlarmCache
import dagger.hilt.android.qualifiers.ApplicationContext
import android.net.Uri
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alarm.meluko.auth.AuthConstants
import com.alarm.meluko.data.FirestoreCollection
import com.alarm.meluko.data.FirestoreField
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val storage: FirebaseStorage
) : ViewModel() {

    private val uid get() = auth.currentUser?.uid ?: ""

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded = _isLoaded.asStateFlow()

    private val _displayName = MutableStateFlow("")
    val displayName = _displayName.asStateFlow()

    private val _username = MutableStateFlow("")
    val username = _username.asStateFlow()

    private val _bio = MutableStateFlow("")
    val bio = _bio.asStateFlow()

    private val _photoUrl = MutableStateFlow<String?>(null)
    val photoUrl = _photoUrl.asStateFlow()

    private val _streakDays = MutableStateFlow(0)
    val streakDays = _streakDays.asStateFlow()

    private val _bestStreak = MutableStateFlow(0)
    val bestStreak = _bestStreak.asStateFlow()

    private val _totalWakes = MutableStateFlow(0)
    val totalWakes = _totalWakes.asStateFlow()

    private val _isUpdating = MutableStateFlow(false)
    val isUpdating = _isUpdating.asStateFlow()

    private val _updateError = MutableStateFlow<String?>(null)
    val updateError = _updateError.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled = _notificationsEnabled.asStateFlow()

    private val _alarmToneUri = MutableStateFlow(AlarmCache.getAlarmToneUri(appContext))
    val alarmToneUri = _alarmToneUri.asStateFlow()

    private val _alarmToneTitle = MutableStateFlow(AlarmCache.getAlarmToneTitle(appContext))
    val alarmToneTitle = _alarmToneTitle.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private var userDocRegistration: ListenerRegistration? = null
    private var userAlarmRegistration: ListenerRegistration? = null
    private var memberRegistration: ListenerRegistration? = null

    init {
        if (uid.isNotEmpty()) loadUserData()
    }

    private fun loadUserData() {
        userDocRegistration?.remove()
        userDocRegistration = db.collection(FirestoreCollection.USERS).document(uid)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.w("AccountVM", "User doc listener error", error)
                    _error.value = "Failed to load profile"
                    _isLoaded.value = true
                    return@addSnapshotListener
                }
                if (snap != null && snap.exists()) {
                    _isLoaded.value = true
                    _displayName.value = snap.getString(FirestoreField.DISPLAY_NAME) ?: ""
                    _username.value = snap.getString(FirestoreField.USERNAME) ?: ""
                    _bio.value = snap.getString(FirestoreField.BIO) ?: ""
                    _photoUrl.value = snap.getString(FirestoreField.PHOTO_URL)
                    _notificationsEnabled.value = snap.getBoolean(FirestoreField.NOTIFICATIONS_ENABLED) ?: true
                }
                if (snap != null && !snap.exists()) {
                    _isLoaded.value = true
                }
            }

        userAlarmRegistration?.remove()
        userAlarmRegistration = db.collection(FirestoreCollection.USER_ALARMS).document(uid)
            .addSnapshotListener { userAlarmSnap, error ->
                if (error != null) {
                    Log.w("AccountVM", "User alarm listener error", error)
                    _error.value = "Failed to load alarm data"
                    return@addSnapshotListener
                }
                val primaryTribeId = userAlarmSnap?.getString(FirestoreField.PRIMARY_TRIBE_ID)
                // Legacy: tones used to be stored in Firestore. Migrate once into the local cache.
                val legacyTone = userAlarmSnap?.getString(FirestoreField.ALARM_TONE_URI)
                if (legacyTone != null && !AlarmCache.hasChosenAlarmTone(appContext)) {
                    updateAlarmTone(legacyTone, null)
                }
                if (primaryTribeId != null) {
                    memberRegistration?.remove()
                    memberRegistration = db.collection(FirestoreCollection.TRIBES).document(primaryTribeId)
                        .collection(FirestoreCollection.MEMBERS).document(uid)
                        .addSnapshotListener { snap, memberError ->
                            if (memberError != null) {
                                Log.w("AccountVM", "Member listener error", memberError)
                                _error.value = "Failed to load stats"
                                return@addSnapshotListener
                            }
                            if (snap != null && snap.exists()) {
                                _streakDays.value = snap.getLong(FirestoreField.STREAK_DAYS)?.toInt() ?: 0
                                _bestStreak.value = snap.getLong(FirestoreField.BEST_STREAK)?.toInt() ?: 0
                                _totalWakes.value = snap.getLong(FirestoreField.TOTAL_WAKES)?.toInt() ?: 0
                            }
                        }
                }
            }
    }

    fun clearError() {
        _error.value = null
    }

    fun updateProfile(
        newDisplayName: String,
        newBio: String,
        imageUri: Uri?,
        onSuccess: () -> Unit
    ) {
        if (uid.isEmpty()) return
        viewModelScope.launch {
            _isUpdating.value = true
            _updateError.value = null
            try {
                var uploadedPhotoUrl: String? = null
                if (imageUri != null) {
                    val ref = storage.reference.child("profile_photos/$uid.jpg")
                    ref.putFile(imageUri).await()
                    uploadedPhotoUrl = ref.downloadUrl.await().toString()
                }

                val updateMap = mutableMapOf<String, Any?>(
                    FirestoreField.DISPLAY_NAME to newDisplayName.trim(),
                    FirestoreField.BIO to newBio.trim()
                )
                if (uploadedPhotoUrl != null) updateMap[FirestoreField.PHOTO_URL] = uploadedPhotoUrl

                db.collection(FirestoreCollection.USERS).document(uid)
                    .set(updateMap, SetOptions.merge()).await()

                val tribeSnap = db.collection(FirestoreCollection.USER_ALARMS).document(uid).get().await()
                val primaryTribeId = tribeSnap.getString(FirestoreField.PRIMARY_TRIBE_ID)
                if (primaryTribeId != null) {
                    val memberUpdate = mutableMapOf<String, Any?>(FirestoreField.DISPLAY_NAME to newDisplayName.trim())
                    if (uploadedPhotoUrl != null) memberUpdate[FirestoreField.PHOTO_URL] = uploadedPhotoUrl
                    db.collection(FirestoreCollection.TRIBES).document(primaryTribeId)
                        .collection(FirestoreCollection.MEMBERS).document(uid)
                        .update(memberUpdate as Map<String, Any?>).await()
                }

                onSuccess()
            } catch (e: Exception) {
                Log.w("AccountVM", "Profile update failed", e)
                _updateError.value = "Update failed: ${e.message}"
            } finally {
                _isUpdating.value = false
            }
        }
    }

    fun updateNotificationPrefs(enabled: Boolean) {
        if (uid.isEmpty()) return
        viewModelScope.launch {
            try {
                db.collection(FirestoreCollection.USERS).document(uid)
                    .update(FirestoreField.NOTIFICATIONS_ENABLED, enabled).await()
                _notificationsEnabled.value = enabled
            } catch (e: Exception) {
                Log.w("AccountVM", "Notification prefs update failed", e)
                _error.value = "Failed to update notification settings"
            }
        }
    }

    /** Tone is device-local, so it's stored on-device only. [title] null = unknown/default. */
    fun updateAlarmTone(toneUri: String?, title: String?) {
        AlarmCache.saveAlarmTone(appContext, toneUri, title)
        _alarmToneUri.value = toneUri
        _alarmToneTitle.value = title
    }

    /**
     * Permanently delete the account: best-effort tribe cleanup, Firestore
     * docs, profile photo, then the Firebase Auth user itself. Required by
     * Google Play policy before any real release.
     *
     * Firebase requires a *recent* login to delete the auth user — if the
     * cached session is stale, this re-prompts Google Sign-In and retries.
     */
    fun deleteAccount(context: Context, onSuccess: () -> Unit) {
        val currentUid = uid.takeIf { it.isNotEmpty() } ?: return
        viewModelScope.launch {
            _isUpdating.value = true
            _error.value = null
            try {
                leaveTribeBestEffort(currentUid)

                try {
                    storage.reference.child("profile_photos/$currentUid.jpg").delete().await()
                } catch (e: Exception) {
                    Log.w("AccountVM", "Profile photo delete failed (non-critical)", e)
                }

                try {
                    db.collection(FirestoreCollection.USERS).document(currentUid).delete().await()
                } catch (e: Exception) {
                    Log.w("AccountVM", "User doc delete failed (non-critical)", e)
                }
                try {
                    db.collection(FirestoreCollection.USER_ALARMS).document(currentUid).delete().await()
                } catch (e: Exception) {
                    Log.w("AccountVM", "User alarm doc delete failed (non-critical)", e)
                }

                try {
                    auth.currentUser?.delete()?.await()
                } catch (e: FirebaseAuthRecentLoginRequiredException) {
                    reauthenticateWithGoogle(context)
                    auth.currentUser?.delete()?.await()
                }

                onSuccess()
            } catch (e: Exception) {
                Log.w("AccountVM", "Account deletion failed", e)
                _error.value = "Failed to delete account: ${e.message}"
            } finally {
                _isUpdating.value = false
            }
        }
    }

    private suspend fun leaveTribeBestEffort(currentUid: String) {
        try {
            val tribeId = db.collection(FirestoreCollection.USER_ALARMS).document(currentUid).get().await()
                .getString(FirestoreField.PRIMARY_TRIBE_ID) ?: return
            com.alarm.meluko.data.TribeActions.leaveTribe(db, currentUid, tribeId)
        } catch (e: Exception) {
            Log.w("AccountVM", "Leaving tribe during account deletion failed (non-critical)", e)
        }
    }

    private suspend fun reauthenticateWithGoogle(context: Context) {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(AuthConstants.WEB_CLIENT_ID)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
        val credentialManager = CredentialManager.create(context)
        val result = credentialManager.getCredential(context, request)
        val googleIdToken = GoogleIdTokenCredential.createFrom(result.credential.data).idToken
        val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        auth.currentUser?.reauthenticate(firebaseCredential)?.await()
    }

    override fun onCleared() {
        super.onCleared()
        userDocRegistration?.remove()
        userAlarmRegistration?.remove()
        memberRegistration?.remove()
    }
}
