package com.alarm.meluko.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alarm.meluko.data.FirestoreCollection
import com.alarm.meluko.data.FirestoreField
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import javax.inject.Inject

data class AlarmProposal(
    val id: String,
    val proposedBy: String,
    val proposedByName: String,
    val proposedTime: String,
    val status: String,
    val vetoedBy: List<String>,
    val vetoCount: Int,
    val createdAt: Date?,
    val expiresAt: Date?
)

@HiltViewModel
class ProposalViewModel @Inject constructor() : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val uid get() = auth.currentUser?.uid ?: ""

    private val _activeProposal = MutableStateFlow<AlarmProposal?>(null)
    val activeProposal: StateFlow<AlarmProposal?> = _activeProposal.asStateFlow()

    private val _vetoesUsed = MutableStateFlow(0)
    val vetoesUsed: StateFlow<Int> = _vetoesUsed.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var isAccepting = false

    fun clearError() { _error.value = null }

    private var proposalListener: ListenerRegistration? = null
    private var memberDocListener: ListenerRegistration? = null

    fun listenToTribeProposals(tribeId: String) {
        proposalListener?.remove()
        memberDocListener?.remove()

        proposalListener = db.collection(FirestoreCollection.TRIBES).document(tribeId)
            .collection(FirestoreCollection.PROPOSALS)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.w("ProposalVM", "Proposal listener error", error)
                    _error.value = "Failed to load proposals: ${error.message}"
                    return@addSnapshotListener
                }
                if (snap == null || snap.isEmpty) {
                    _activeProposal.value = null
                    return@addSnapshotListener
                }

                val pending = snap.documents.filter {
                    it.getString(FirestoreField.STATUS) == "pending"
                }
                if (pending.isEmpty()) {
                    _activeProposal.value = null
                    return@addSnapshotListener
                }

                val doc = pending.maxByOrNull {
                    it.getDate(FirestoreField.CREATED_AT)?.time ?: 0L
                } ?: return@addSnapshotListener

                val expiresAt = doc.getDate(FirestoreField.EXPIRES_AT)
                val now = Date()

                if (expiresAt != null && now.after(expiresAt)) {
                    if (!isAccepting) {
                        isAccepting = true
                        viewModelScope.launch {
                            acceptProposal(
                                tribeId = tribeId,
                                proposalId = doc.id,
                                proposedTime = doc.getString(FirestoreField.PROPOSED_TIME) ?: return@launch
                            )
                            isAccepting = false
                        }
                    }
                    return@addSnapshotListener
                }

                _activeProposal.value = AlarmProposal(
                    id = doc.id,
                    proposedBy = doc.getString(FirestoreField.PROPOSED_BY) ?: "",
                    proposedByName = doc.getString(FirestoreField.PROPOSED_BY_NAME) ?: "Someone",
                    proposedTime = doc.getString(FirestoreField.PROPOSED_TIME) ?: "--:--",
                    status = doc.getString(FirestoreField.STATUS) ?: "pending",
                    vetoedBy = (doc.get(FirestoreField.VETOED_BY) as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    vetoCount = doc.getLong(FirestoreField.VETO_COUNT)?.toInt() ?: 0,
                    createdAt = doc.getDate(FirestoreField.CREATED_AT),
                    expiresAt = expiresAt
                )
            }

        if (uid.isNotEmpty()) {
            memberDocListener = db.collection(FirestoreCollection.TRIBES).document(tribeId)
                .collection(FirestoreCollection.MEMBERS).document(uid)
                .addSnapshotListener { snap, error ->
                    if (error != null) {
                        Log.w("ProposalVM", "Member doc listener error", error)
                        return@addSnapshotListener
                    }
                    _vetoesUsed.value = snap?.getLong(FirestoreField.VETOES_USED)?.toInt() ?: 0
                }
        }
    }

    fun createProposal(tribeId: String, proposedTime: String, onSuccess: () -> Unit, onError: (() -> Unit)? = null) {
        if (uid.isEmpty()) {
            _error.value = "Not signed in"
            onError?.invoke()
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val userDoc = db.collection(FirestoreCollection.USERS).document(uid).get().await()
                val displayName = userDoc.getString(FirestoreField.DISPLAY_NAME) ?: "A tribe member"

                val proposalId = UUID.randomUUID().toString()
                val now = Date()
                val expiresAt = Date(now.time + (2 * 60 * 60 * 1000L))

                db.collection(FirestoreCollection.TRIBES).document(tribeId)
                    .collection(FirestoreCollection.PROPOSALS).document(proposalId)
                    .set(
                        mapOf(
                            FirestoreField.PROPOSED_BY to uid,
                            FirestoreField.PROPOSED_BY_NAME to displayName,
                            FirestoreField.PROPOSED_TIME to proposedTime,
                            FirestoreField.STATUS to "pending",
                            FirestoreField.VETOED_BY to emptyList<String>(),
                            FirestoreField.VETO_COUNT to 0,
                            FirestoreField.CREATED_AT to FieldValue.serverTimestamp(),
                            FirestoreField.EXPIRES_AT to expiresAt
                        )
                    ).await()

                onSuccess()
            } catch (e: Exception) {
                Log.w("ProposalVM", "Create proposal failed", e)
                _error.value = "Failed to propose: ${e.message}"
                onError?.invoke()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun castVeto(tribeId: String, proposalId: String, onSuccess: () -> Unit, onError: (() -> Unit)? = null) {
        if (uid.isEmpty()) {
            _error.value = "Not signed in"
            onError?.invoke()
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                db.collection(FirestoreCollection.TRIBES).document(tribeId)
                    .collection(FirestoreCollection.PROPOSALS).document(proposalId)
                    .update(
                        mapOf(
                            FirestoreField.STATUS to "vetoed",
                            FirestoreField.VETOED_BY to FieldValue.arrayUnion(uid),
                            FirestoreField.VETO_COUNT to FieldValue.increment(1)
                        )
                    ).await()

                db.collection(FirestoreCollection.TRIBES).document(tribeId)
                    .collection(FirestoreCollection.MEMBERS).document(uid)
                    .update(FirestoreField.VETOES_USED, FieldValue.increment(1)).await()

                _activeProposal.value = null
                onSuccess()
            } catch (e: Exception) {
                Log.w("ProposalVM", "Veto failed", e)
                _error.value = "Failed to veto: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun acceptProposal(tribeId: String, proposalId: String, proposedTime: String) {
        try {
            val batch = db.batch()

            batch.update(
                db.collection(FirestoreCollection.TRIBES).document(tribeId)
                    .collection(FirestoreCollection.PROPOSALS).document(proposalId),
                FirestoreField.STATUS, "accepted"
            )

            batch.update(
                db.collection(FirestoreCollection.TRIBES).document(tribeId),
                FirestoreField.ALARM_TIME, proposedTime
            )

            if (uid.isNotEmpty()) {
                batch.update(
                    db.collection(FirestoreCollection.USER_ALARMS).document(uid),
                    FirestoreField.FOLLOWED_ALARM_TIME, proposedTime
                )
            }

            batch.commit().await()
            _activeProposal.value = null
        } catch (e: Exception) {
            Log.w("ProposalVM", "Accept proposal failed", e)
            _error.value = "Failed to accept proposal: ${e.message}"
        }
    }

    override fun onCleared() {
        super.onCleared()
        proposalListener?.remove()
        memberDocListener?.remove()
    }
}
