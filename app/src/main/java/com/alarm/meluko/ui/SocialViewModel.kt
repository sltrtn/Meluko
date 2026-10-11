package com.alarm.meluko.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alarm.meluko.data.FirestoreCollection
import com.alarm.meluko.data.FirestoreField
import com.alarm.meluko.data.TribeActions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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

    private val _isTribeActionInProgress = MutableStateFlow(false)
    val isTribeActionInProgress = _isTribeActionInProgress.asStateFlow()

    private val _tribeActionError = MutableStateFlow<String?>(null)
    val tribeActionError = _tribeActionError.asStateFlow()

    fun clearTribeActionError() {
        _tribeActionError.value = null
    }

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
                    // No longer a member (removed by an admin, or the tribe was
                    // deleted) — Cloud Functions aren't deployed on this project
                    // to clear our pointer server-side, so self-heal here.
                    if (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        memberRegistration?.remove()
                        memberRegistration = null
                        currentMemberTribeId = null
                        clearStaleTribePointer(tribeId)
                    } else {
                        _uiState.value = SocialUiState.Error("Failed to load members: ${error.message}")
                    }
                    return@addSnapshotListener
                }
                if (snap != null) {
                    val list = snap.documents.mapNotNull { doc ->
                        TribeMember(
                            uid = doc.id,
                            name = doc.getString(FirestoreField.DISPLAY_NAME) ?: "Unknown",
                            photoUrl = doc.getString(FirestoreField.PHOTO_URL),
                            isYou = doc.id == uid,
                            isAdmin = doc.getString(FirestoreField.ROLE) == FirestoreField.ROLE_ADMIN,
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

    /**
     * Clears our own user_alarms.primaryTribeId when it points at a tribe we
     * can no longer read (removed by an admin) or that no longer exists
     * (deleted). Normally a Cloud Function trigger does this server-side,
     * but this project doesn't have Cloud Functions deployed (requires the
     * Blaze billing plan), so it's handled client-side as a self-heal.
     */
    private fun clearStaleTribePointer(tribeId: String) {
        val currentUid = uid.takeIf { it.isNotEmpty() } ?: return
        viewModelScope.launch {
            try {
                val userAlarmRef = db.collection(FirestoreCollection.USER_ALARMS).document(currentUid)
                val snap = userAlarmRef.get().await()
                if (snap.getString(FirestoreField.PRIMARY_TRIBE_ID) == tribeId) {
                    userAlarmRef.set(
                        mapOf(FirestoreField.PRIMARY_TRIBE_ID to null, FirestoreField.ACTIVE to false),
                        SetOptions.merge()
                    ).await()
                }
            } catch (e: Exception) {
                Log.w("SocialVM", "Failed to clear stale tribe pointer", e)
            }
            _tribes.value = emptyList()
            _members.value = emptyList()
            _uiState.value = SocialUiState.Success(members = emptyList(), tribes = emptyList())
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
                        } else if (tribeSnap != null && !tribeSnap.exists() && !tribeSnap.metadata.isFromCache) {
                            // Tribe was deleted (confirmed by the server, not a cold offline cache).
                            clearStaleTribePointer(primaryTribeId)
                        }
                    }

                loadMembersForTribe(primaryTribeId)
            }
    }

    /** Leave a tribe. Hands admin to the longest-standing member, or deletes the tribe if we're last. */
    fun leaveTribe(tribeId: String, onSuccess: () -> Unit) {
        val currentUid = uid.takeIf { it.isNotEmpty() } ?: return
        runTribeAction {
            TribeActions.leaveTribe(db, currentUid, tribeId)
            onSuccess()
        }
    }

    /** Admin-only: remove another member from the tribe. */
    fun removeMember(tribeId: String, memberUid: String, onSuccess: () -> Unit) {
        runTribeAction {
            TribeActions.removeMember(db, tribeId, memberUid)
            onSuccess()
        }
    }

    /** Admin-only: make another member an admin too. */
    fun makeAdmin(tribeId: String, memberUid: String) {
        runTribeAction { TribeActions.makeAdmin(db, tribeId, memberUid) }
    }

    /** Last member: permanently delete the tribe. */
    fun deleteTribe(tribeId: String, onSuccess: () -> Unit) {
        val currentUid = uid.takeIf { it.isNotEmpty() } ?: return
        runTribeAction {
            TribeActions.deleteTribe(db, currentUid, tribeId)
            onSuccess()
        }
    }

    /** Admins: make sure a legacy tribe's invite code is actually joinable. */
    fun ensureInvite(tribeId: String, inviteCode: String) {
        if (inviteCode.isBlank()) return
        viewModelScope.launch { TribeActions.ensureInviteDoc(db, tribeId, inviteCode) }
    }

    private fun runTribeAction(block: suspend () -> Unit) {
        viewModelScope.launch {
            _isTribeActionInProgress.value = true
            _tribeActionError.value = null
            try {
                block()
            } catch (e: Exception) {
                Log.w("SocialVM", "Tribe action failed", e)
                _tribeActionError.value = "Something went wrong: ${e.message}"
            } finally {
                _isTribeActionInProgress.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        userAlarmRegistration?.remove()
        tribeRegistration?.remove()
        memberRegistration?.remove()
    }
}
