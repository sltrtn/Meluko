package com.alarm.meluko.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
class OnboardingViewModel @Inject constructor() : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val uid get() = auth.currentUser?.uid

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    // Upload profile photo to Firebase Storage and return the download URL
    private suspend fun uploadProfilePhoto(uri: Uri): String? {
        val uid = uid ?: return null
        return try {
            val ref = storage.reference.child("profile_photos/$uid.jpg")
            ref.putFile(uri).await()
            ref.downloadUrl.await().toString()
        } catch (e: Exception) {
            null // Non-fatal — profile still saves without a photo
        }
    }

    fun updateProfile(
        username: String,
        bio: String,
        imageUri: Uri?,
        onSuccess: () -> Unit
    ) {
        val uid = uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Upload photo first (if one was picked)
                val photoUrl = if (imageUri != null) uploadProfilePhoto(imageUri) else null

                val updateMap = mutableMapOf<String, Any?>(
                    "username" to username,
                    "bio" to bio,
                )
                if (photoUrl != null) {
                    updateMap["photoUrl"] = photoUrl
                }

                db.collection("users").document(uid)
                    .update(updateMap as Map<String, Any?>)
                    .await()

                onSuccess()
            } catch (e: Exception) {
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
        val uid = uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Get user's displayName from users doc for denormalization
                val userDoc = db.collection("users").document(uid).get().await()
                val displayName = userDoc.getString("displayName") ?: "Unknown"
                val photoUrl = userDoc.getString("photoUrl")

                val tribeId = UUID.randomUUID().toString()
                // Generate a clean 6-char uppercase invite code
                val inviteCode = buildString {
                    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
                    repeat(6) { append(chars.random()) }
                }

                val defaultAlarmTime = "07:30"

                // 1. Create the tribe document
                db.collection("tribes").document(tribeId).set(
                    mapOf(
                        "name" to name,
                        "description" to description,
                        "inviteCode" to inviteCode,
                        "createdBy" to uid,
                        "alarmTime" to defaultAlarmTime,
                        "alarmDays" to listOf(1, 2, 3, 4, 5),
                        "memberCount" to 1,
                        "photoUrl" to null,
                        "toneUri" to null,
                        "toneSource" to null,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                // 2. Add creator as admin member
                db.collection("tribes").document(tribeId)
                    .collection("members").document(uid).set(
                        mapOf(
                            "displayName" to displayName,
                            "photoUrl" to photoUrl,
                            "role" to "admin",
                            "todayStatus" to "sleeping",
                            "todayWakeTime" to null,
                            "streakDays" to 0,
                            "bestStreak" to 0,
                            "totalWakes" to 0,
                            "rank" to 1,
                            "joinedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()

                // 3. Set user_alarms document so HomeScreen knows what time to show
                db.collection("user_alarms").document(uid).set(
                    mapOf(
                        "primaryTribeId" to tribeId,
                        "followedAlarmTime" to defaultAlarmTime,
                        "active" to true
                    ),
                    SetOptions.merge()
                ).await()

                onSuccess()
            } catch (e: Exception) {
                _error.value = "Failed to create tribe: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun joinTribe(inviteCode: String, onSuccess: () -> Unit) {
        val uid = uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Get user data for denormalization
                val userDoc = db.collection("users").document(uid).get().await()
                val displayName = userDoc.getString("displayName") ?: "Unknown"
                val photoUrl = userDoc.getString("photoUrl")

                // Find tribe by invite code
                val querySnapshot = db.collection("tribes")
                    .whereEqualTo("inviteCode", inviteCode.trim().uppercase())
                    .get().await()

                if (querySnapshot.isEmpty) {
                    _error.value = "Invalid invite code. Check with your tribe admin."
                    return@launch
                }

                val tribeDoc = querySnapshot.documents[0]
                val tribeId = tribeDoc.id
                val alarmTime = tribeDoc.getString("alarmTime") ?: "07:30"
                val currentCount = tribeDoc.getLong("memberCount") ?: 0

                // Check if user is already a member
                val existingMember = db.collection("tribes").document(tribeId)
                    .collection("members").document(uid).get().await()
                if (existingMember.exists()) {
                    _error.value = "You're already in this tribe!"
                    return@launch
                }

                // 1. Add user to tribe members
                db.collection("tribes").document(tribeId)
                    .collection("members").document(uid).set(
                        mapOf(
                            "displayName" to displayName,
                            "photoUrl" to photoUrl,
                            "role" to "member",
                            "todayStatus" to "sleeping",
                            "todayWakeTime" to null,
                            "streakDays" to 0,
                            "bestStreak" to 0,
                            "totalWakes" to 0,
                            "rank" to (currentCount + 1).toInt(),
                            "joinedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()

                // 2. Increment memberCount on tribe
                db.collection("tribes").document(tribeId)
                    .update("memberCount", currentCount + 1).await()

                // 3. Set user_alarms so HomeScreen shows the tribe's alarm time
                db.collection("user_alarms").document(uid).set(
                    mapOf(
                        "primaryTribeId" to tribeId,
                        "followedAlarmTime" to alarmTime,
                        "active" to true
                    ),
                    SetOptions.merge()
                ).await()

                onSuccess()
            } catch (e: Exception) {
                _error.value = "Failed to join tribe: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
