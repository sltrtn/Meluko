package com.alarm.meluko.ui

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alarm.meluko.data.FirestoreCollection
import com.alarm.meluko.data.FirestoreField
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
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
                val userDoc = db.collection(FirestoreCollection.USERS).document(currentUid).get().await()
                val displayName = userDoc.getString(FirestoreField.DISPLAY_NAME) ?: "Unknown"
                val photoUrl = userDoc.getString(FirestoreField.PHOTO_URL)

                val tribeId = UUID.randomUUID().toString()
                val inviteCode = buildString {
                    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
                    repeat(6) { append(chars.random()) }
                }

                val defaultAlarmTime = "07:30"

                val tribeRef = db.collection(FirestoreCollection.TRIBES).document(tribeId)
                val memberRef = tribeRef.collection(FirestoreCollection.MEMBERS).document(currentUid)
                val userAlarmRef = db.collection(FirestoreCollection.USER_ALARMS).document(currentUid)

                // Batch: all three docs must be created together; partial state is unusable.
                db.batch().apply {
                    set(
                        tribeRef,
                        mapOf(
                            FirestoreField.NAME to name,
                            FirestoreField.DESCRIPTION to description,
                            FirestoreField.INVITE_CODE to inviteCode,
                            FirestoreField.CREATED_BY to currentUid,
                            FirestoreField.ALARM_TIME to defaultAlarmTime,
                            FirestoreField.ALARM_DAYS to listOf(1, 2, 3, 4, 5),
                            FirestoreField.MEMBER_COUNT to 1,
                            FirestoreField.PHOTO_URL to null,
                            FirestoreField.TONE_URI to null,
                            FirestoreField.TONE_SOURCE to null,
                            FirestoreField.CREATED_AT to FieldValue.serverTimestamp()
                        )
                    )
                    set(
                        memberRef,
                        mapOf(
                            FirestoreField.DISPLAY_NAME to displayName,
                            FirestoreField.PHOTO_URL to photoUrl,
                            FirestoreField.ROLE to FirestoreField.ROLE_ADMIN,
                            FirestoreField.TODAY_STATUS to FirestoreField.STATUS_SLEEPING,
                            FirestoreField.TODAY_WAKE_TIME to null,
                            FirestoreField.STREAK_DAYS to 0,
                            FirestoreField.BEST_STREAK to 0,
                            FirestoreField.TOTAL_WAKES to 0,
                            FirestoreField.VETOES_USED to 0,
                            FirestoreField.RANK to 1,
                            FirestoreField.JOINED_AT to FieldValue.serverTimestamp()
                        )
                    )
                    set(
                        userAlarmRef,
                        mapOf(
                            FirestoreField.PRIMARY_TRIBE_ID to tribeId,
                            FirestoreField.FOLLOWED_ALARM_TIME to defaultAlarmTime,
                            FirestoreField.ACTIVE to true
                        ),
                        SetOptions.merge()
                    )
                }.commit().await()

                onSuccess()
            } catch (e: Exception) {
                Log.w("OnboardingVM", "Create tribe failed", e)
                _error.value = "Failed to create tribe: ${e.message}"
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
                val userDoc = db.collection(FirestoreCollection.USERS).document(currentUid).get().await()
                val displayName = userDoc.getString(FirestoreField.DISPLAY_NAME) ?: "Unknown"
                val photoUrl = userDoc.getString(FirestoreField.PHOTO_URL)

                val querySnapshot = db.collection(FirestoreCollection.TRIBES)
                    .whereEqualTo(FirestoreField.INVITE_CODE, inviteCode.trim().uppercase())
                    .get().await()

                if (querySnapshot.isEmpty) {
                    _error.value = "Invalid invite code. Check with your tribe admin."
                    return@launch
                }

                val tribeDoc = querySnapshot.documents[0]
                val tribeId = tribeDoc.id
                val alarmTime = tribeDoc.getString(FirestoreField.ALARM_TIME) ?: "07:30"

                val tribeRef = db.collection(FirestoreCollection.TRIBES).document(tribeId)
                val memberRef = tribeRef.collection(FirestoreCollection.MEMBERS).document(currentUid)

                // Atomic transaction: re-check membership, create member doc, increment count.
                db.runTransaction { transaction ->
                    val existing = transaction.get(memberRef)
                    if (existing.exists()) {
                        throw IllegalStateException("Already a member of this tribe")
                    }

                    transaction.set(
                        memberRef,
                        mapOf(
                            FirestoreField.DISPLAY_NAME to displayName,
                            FirestoreField.PHOTO_URL to photoUrl,
                            FirestoreField.ROLE to FirestoreField.ROLE_MEMBER,
                            FirestoreField.TODAY_STATUS to FirestoreField.STATUS_SLEEPING,
                            FirestoreField.TODAY_WAKE_TIME to null,
                            FirestoreField.STREAK_DAYS to 0,
                            FirestoreField.BEST_STREAK to 0,
                            FirestoreField.TOTAL_WAKES to 0,
                            FirestoreField.VETOES_USED to 0,
                            FirestoreField.RANK to 1,
                            FirestoreField.JOINED_AT to FieldValue.serverTimestamp()
                        )
                    )
                    transaction.update(tribeRef, FirestoreField.MEMBER_COUNT, FieldValue.increment(1))
                }.await()

                db.collection(FirestoreCollection.USER_ALARMS).document(currentUid).set(
                    mapOf(
                        FirestoreField.PRIMARY_TRIBE_ID to tribeId,
                        FirestoreField.FOLLOWED_ALARM_TIME to alarmTime,
                        FirestoreField.ACTIVE to true
                    ),
                    SetOptions.merge()
                ).await()

                onSuccess()
            } catch (e: Exception) {
                Log.w("OnboardingVM", "Join tribe failed", e)
                _error.value = "Failed to join tribe: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
