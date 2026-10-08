package com.alarm.meluko.ui

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alarm.meluko.data.FirestoreCollection
import com.alarm.meluko.data.FirestoreField
import com.google.firebase.auth.FirebaseAuth
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

    private val _alarmToneUri = MutableStateFlow<String?>(null)
    val alarmToneUri = _alarmToneUri.asStateFlow()

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
                _alarmToneUri.value = userAlarmSnap?.getString(FirestoreField.ALARM_TONE_URI)
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

    fun updateAlarmTone(toneUri: String?) {
        if (uid.isEmpty()) return
        viewModelScope.launch {
            try {
                db.collection(FirestoreCollection.USER_ALARMS).document(uid)
                    .update(FirestoreField.ALARM_TONE_URI, toneUri).await()
                _alarmToneUri.value = toneUri
            } catch (e: Exception) {
                Log.w("AccountVM", "Alarm tone update failed", e)
                _error.value = "Failed to update alarm tone"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        userDocRegistration?.remove()
        userAlarmRegistration?.remove()
        memberRegistration?.remove()
    }
}
