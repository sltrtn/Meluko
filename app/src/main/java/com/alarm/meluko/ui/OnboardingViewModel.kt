package com.alarm.meluko.ui

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alarm.meluko.data.FirestoreCollection
import com.alarm.meluko.data.FirestoreField
import com.alarm.meluko.data.TribeActions
import com.alarm.meluko.data.TribeException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val storage: FirebaseStorage
) : ViewModel() {

    private val uid get() = auth.currentUser?.uid

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private suspend fun uploadProfilePhoto(uri: Uri): String? {
        val currentUid = uid ?: return null
        return try {
            val ref = storage.reference.child("profile_photos/$currentUid.jpg")
            ref.putFile(uri).await()
            ref.downloadUrl.await().toString()
        } catch (e: Exception) {
            Log.w("OnboardingVM", "Photo upload failed", e)
            _error.value = "Photo upload failed"
            null
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun updateProfile(
        username: String,
        bio: String,
        imageUri: Uri?,
        onSuccess: () -> Unit
    ) {
        val currentUid = uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val photoUrl = if (imageUri != null) uploadProfilePhoto(imageUri) else null

                val updateMap = mutableMapOf<String, Any?>(
                    FirestoreField.USERNAME to username,
                    FirestoreField.BIO to bio,
                )
                if (photoUrl != null) {
                    updateMap[FirestoreField.PHOTO_URL] = photoUrl
                }

                db.collection(FirestoreCollection.USERS).document(currentUid)
                    .update(updateMap as Map<String, Any?>)
                    .await()

                onSuccess()
            } catch (e: Exception) {
                Log.w("OnboardingVM", "Profile update failed", e)
                _error.value = "Failed to update profile: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun myNameAndPhoto(uid: String): Pair<String, String?> {
        val userDoc = db.collection(FirestoreCollection.USERS).document(uid).get().await()
        return (userDoc.getString(FirestoreField.DISPLAY_NAME) ?: "Unknown") to userDoc.getString(FirestoreField.PHOTO_URL)
    }

    fun createTribe(
        name: String,
        description: String,
        onSuccess: () -> Unit
    ) {
        val currentUid = uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val (displayName, photoUrl) = myNameAndPhoto(currentUid)
                TribeActions.createTribe(db, currentUid, displayName, photoUrl, name.trim(), description.trim())
                onSuccess()
            } catch (e: TribeException) {
                _error.value = e.message
            } catch (e: Exception) {
                Log.w("OnboardingVM", "Create tribe failed", e)
                _error.value = "Couldn't create the tribe. Please try again."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun joinTribe(inviteCode: String, onSuccess: () -> Unit) {
        val currentUid = uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val (displayName, photoUrl) = myNameAndPhoto(currentUid)
                TribeActions.joinTribe(db, currentUid, displayName, photoUrl, inviteCode)
                onSuccess()
            } catch (e: TribeException) {
                _error.value = e.message
            } catch (e: Exception) {
                Log.w("OnboardingVM", "Join tribe failed", e)
                _error.value = "Couldn't join the tribe. Please try again."
            } finally {
                _isLoading.value = false
            }
        }
    }
}
