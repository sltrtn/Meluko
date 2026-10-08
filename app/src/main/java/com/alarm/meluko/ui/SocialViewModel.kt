package com.alarm.meluko.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import com.alarm.meluko.data.FirestoreCollection
import com.alarm.meluko.data.FirestoreField
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

sealed class SocialUiState {
    data object Loading : SocialUiState()
    data class Success(
        val members: List<TribeMember>,
        val tribes: List<TribeProgress>
    ) : SocialUiState()
    data class Error(val message: String) : SocialUiState()
}

@HiltViewModel
class SocialViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : ViewModel() {

    private val uid get() = auth.currentUser?.uid ?: ""

    private val _uiState = MutableStateFlow<SocialUiState>(SocialUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _members = MutableStateFlow<List<TribeMember>>(emptyList())
    val members = _members.asStateFlow()

    private val _tribes = MutableStateFlow<List<TribeProgress>>(emptyList())
    val tribes = _tribes.asStateFlow()

    private var userAlarmRegistration: ListenerRegistration? = null
    private var tribeRegistration: ListenerRegistration? = null
    private var memberRegistration: ListenerRegistration? = null
    private var currentMemberTribeId: String? = null

    init {
        val currentUid = auth.currentUser?.uid
        if (!currentUid.isNullOrEmpty()) loadPrimaryTribeMembers(currentUid)
    }

    fun loadMembersForTribe(tribeId: String) {
        // Avoid replacing the listener if we're already listening to this tribe.
        // This prevents TribeDetailScreen from clobbering the Social tab listener.
        if (currentMemberTribeId == tribeId && memberRegistration != null) return

        memberRegistration?.remove()
        currentMemberTribeId = tribeId
        memberRegistration = db.collection(FirestoreCollection.TRIBES).document(tribeId)
            .collection(FirestoreCollection.MEMBERS)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.w("SocialVM", "Members listener error", error)
                    _uiState.value = SocialUiState.Error("Failed to load members: ${error.message}")
                    return@addSnapshotListener
                }
                if (snap != null) {
                    val list = snap.documents.mapNotNull { doc ->
                        TribeMember(
                            name = doc.getString(FirestoreField.DISPLAY_NAME) ?: "Unknown",
                            photoUrl = doc.getString(FirestoreField.PHOTO_URL),
                            isYou = doc.id == uid,
                            streakDays = doc.getLong(FirestoreField.STREAK_DAYS)?.toInt() ?: 0,
                            status = when (doc.getString(FirestoreField.TODAY_STATUS)) {
                                FirestoreField.STATUS_AWAKE -> WakeStatus.Awake
                                FirestoreField.STATUS_MISSED -> WakeStatus.Missed
                                else -> WakeStatus.Sleeping
                            },
                            wakeTime = doc.getString(FirestoreField.TODAY_WAKE_TIME)
                        )
                    }.sortedByDescending { it.streakDays }
                    _members.value = list
                    val awakeCount = list.count { it.status == WakeStatus.Awake }
                    _tribes.value = _tribes.value.map { it.copy(awakeCount = awakeCount) }

                    // Recompute and persist current user's rank based on sorted position.
                    recomputeOwnRankIfNeeded(tribeId, list)
                }
                _uiState.value = SocialUiState.Success(members = _members.value, tribes = _tribes.value)
            }
    }

    private fun recomputeOwnRankIfNeeded(tribeId: String, sortedMembers: List<TribeMember>) {
        val currentUid = uid.takeIf { it.isNotEmpty() } ?: return
        val index = sortedMembers.indexOfFirst { it.isYou }
        if (index == -1) return
        val rank = index + 1

        val memberRef = db.collection(FirestoreCollection.TRIBES).document(tribeId)
            .collection(FirestoreCollection.MEMBERS).document(currentUid)
        memberRef.get().addOnSuccessListener { doc ->
            val currentRank = doc.getLong(FirestoreField.RANK)?.toInt()
            if (currentRank != rank) {
                memberRef.update(FirestoreField.RANK, rank)
                    .addOnFailureListener { Log.w("SocialVM", "Failed to update rank", it) }
            }
        }
    }

    private fun loadPrimaryTribeMembers(currentUid: String) {
        userAlarmRegistration?.remove()
        userAlarmRegistration = db.collection(FirestoreCollection.USER_ALARMS).document(currentUid)
            .addSnapshotListener { userAlarmSnap, error ->
                if (error != null) {
                    Log.w("SocialVM", "User alarm listener error", error)
                    _uiState.value = SocialUiState.Error("Failed to load tribe data")
                    return@addSnapshotListener
                }
                val primaryTribeId = userAlarmSnap?.getString(FirestoreField.PRIMARY_TRIBE_ID)
                if (primaryTribeId == null) {
                    _tribes.value = emptyList()
                    _members.value = emptyList()
                    _uiState.value = SocialUiState.Success(members = emptyList(), tribes = emptyList())
                    return@addSnapshotListener
                }

                tribeRegistration?.remove()
                tribeRegistration = db.collection(FirestoreCollection.TRIBES).document(primaryTribeId)
                    .addSnapshotListener { tribeSnap, tribeError ->
                        if (tribeError != null) {
                            Log.w("SocialVM", "Tribe listener error", tribeError)
                            _uiState.value = SocialUiState.Error("Failed to load tribe info")
                            return@addSnapshotListener
                        }
                        if (tribeSnap != null && tribeSnap.exists()) {
                            val name = tribeSnap.getString(FirestoreField.NAME) ?: "Your Tribe"
                            val memberCount = tribeSnap.getLong(FirestoreField.MEMBER_COUNT)?.toInt() ?: 1
                            val photoUrl = tribeSnap.getString(FirestoreField.PHOTO_URL)
                            val awakeCount = _members.value.count { it.status == WakeStatus.Awake }
                            _tribes.value = listOf(
                                TribeProgress(primaryTribeId, name, photoUrl, awakeCount, memberCount)
                            )
                        }
                    }

                loadMembersForTribe(primaryTribeId)
            }
    }

    override fun onCleared() {
        super.onCleared()
        userAlarmRegistration?.remove()
        tribeRegistration?.remove()
        memberRegistration?.remove()
    }
}
