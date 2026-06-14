package com.alarm.meluko.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor() : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val uid get() = auth.currentUser?.uid ?: ""

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

    init {
        if (uid.isNotEmpty()) loadUserData()
    }

    private fun loadUserData() {
        db.collection("users").document(uid)
            .addSnapshotListener { snap, _ ->
                if (snap != null && snap.exists()) {
                    _displayName.value = snap.getString("displayName") ?: ""
                    _username.value = snap.getString("username") ?: ""
                    _bio.value = snap.getString("bio") ?: ""
                    _photoUrl.value = snap.getString("photoUrl")
                    _notificationsEnabled.value = snap.getBoolean("notificationsEnabled") ?: true
                }
            }

        db.collection("user_alarms").document(uid).addSnapshotListener { userAlarmSnap, _ ->
            val primaryTribeId = userAlarmSnap?.getString("primaryTribeId")
            if (primaryTribeId != null) {
                db.collection("tribes").document(primaryTribeId).collection("members").document(uid)
                    .addSnapshotListener { snap, _ ->
                        if (snap != null && snap.exists()) {
                            _streakDays.value = snap.getLong("streakDays")?.toInt() ?: 0
                            _bestStreak.value = snap.getLong("bestStreak")?.toInt() ?: 0
                            _totalWakes.value = snap.getLong("totalWakes")?.toInt() ?: 0
                        }
                    }
            }
        }
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
                    "displayName" to newDisplayName.trim(),
                    "bio" to newBio.trim()
                )
                if (uploadedPhotoUrl != null) updateMap["photoUrl"] = uploadedPhotoUrl

                db.collection("users").document(uid)
                    .set(updateMap, SetOptions.merge()).await()

                // Denormalize displayName and photoUrl into tribe member doc
                val tribeSnap = db.collection("user_alarms").document(uid).get().await()
                val primaryTribeId = tribeSnap.getString("primaryTribeId")
                if (primaryTribeId != null) {
                    val memberUpdate = mutableMapOf<String, Any?>("displayName" to newDisplayName.trim())
                    if (uploadedPhotoUrl != null) memberUpdate["photoUrl"] = uploadedPhotoUrl
                    db.collection("tribes").document(primaryTribeId)
                        .collection("members").document(uid)
                        .update(memberUpdate as Map<String, Any?>).await()
                }

                onSuccess()
            } catch (e: Exception) {
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
                db.collection("users").document(uid)
                    .update("notificationsEnabled", enabled).await()
                _notificationsEnabled.value = enabled
            } catch (e: Exception) {
                // fail silently — not critical
            }
        }
    }
}
