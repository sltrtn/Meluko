package com.alarm.meluko.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val status: String, // "pending", "accepted", "vetoed"
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

    private var proposalListener: ListenerRegistration? = null

    /**
     * Start listening to the active proposal for a given tribe.
     * Only shows pending proposals. Expired non-vetoed proposals are auto-accepted.
     */
    fun listenToTribeProposals(tribeId: String) {
        proposalListener?.remove()
        proposalListener = db.collection("tribes").document(tribeId)
            .collection("proposals")
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snap, _ ->
                if (snap == null || snap.isEmpty) {
                    _activeProposal.value = null
                    return@addSnapshotListener
                }
                // Take the most recent pending proposal
                val doc = snap.documents.maxByOrNull {
                    it.getDate("createdAt")?.time ?: 0L
                } ?: return@addSnapshotListener

                val expiresAt = doc.getDate("expiresAt")
                val now = Date()

                // If the proposal has expired and wasn't vetoed, auto-accept it
                if (expiresAt != null && now.after(expiresAt)) {
                    viewModelScope.launch {
                        acceptProposal(
                            tribeId = tribeId,
                            proposalId = doc.id,
                            proposedTime = doc.getString("proposedTime") ?: return@launch
                        )
                    }
                    return@addSnapshotListener
                }

                _activeProposal.value = AlarmProposal(
                    id = doc.id,
                    proposedBy = doc.getString("proposedBy") ?: "",
                    proposedByName = doc.getString("proposedByName") ?: "Someone",
                    proposedTime = doc.getString("proposedTime") ?: "--:--",
                    status = doc.getString("status") ?: "pending",
                    vetoedBy = (doc.get("vetoedBy") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    vetoCount = doc.getLong("vetoCount")?.toInt() ?: 0,
                    createdAt = doc.getDate("createdAt"),
                    expiresAt = expiresAt
                )
            }

        // Also load the user's veto count for this tribe
        db.collection("tribes").document(tribeId).collection("members").document(uid)
            .addSnapshotListener { snap, _ ->
                _vetoesUsed.value = snap?.getLong("vetoesUsed")?.toInt() ?: 0
            }
    }

    /**
     * Create a new proposal to change the tribe's alarm time.
     * Creates a proposals sub-document with a 2-hour veto window.
     */
    fun createProposal(tribeId: String, proposedTime: String, onSuccess: () -> Unit) {
        if (uid.isEmpty()) return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val userDoc = db.collection("users").document(uid).get().await()
                val displayName = userDoc.getString("displayName") ?: "A tribe member"

                val proposalId = UUID.randomUUID().toString()
                val now = Date()
                // Veto window: 2 hours
                val expiresAt = Date(now.time + (2 * 60 * 60 * 1000L))

                db.collection("tribes").document(tribeId)
                    .collection("proposals").document(proposalId)
                    .set(
                        mapOf(
                            "proposedBy" to uid,
                            "proposedByName" to displayName,
                            "proposedTime" to proposedTime,
                            "status" to "pending",
                            "vetoedBy" to emptyList<String>(),
                            "vetoCount" to 0,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "expiresAt" to expiresAt
                        )
                    ).await()

                onSuccess()
            } catch (e: Exception) {
                _error.value = "Failed to propose: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Cast a veto against the active proposal.
     * - Adds uid to vetoedBy list
     * - Increments the user's vetoesUsed count in the member doc
     * - Sets proposal status to "vetoed" immediately (1 veto kills the proposal)
     */
    fun castVeto(tribeId: String, proposalId: String, onSuccess: () -> Unit) {
        if (uid.isEmpty()) return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Mark the proposal as vetoed
                db.collection("tribes").document(tribeId)
                    .collection("proposals").document(proposalId)
                    .update(
                        mapOf(
                            "status" to "vetoed",
                            "vetoedBy" to FieldValue.arrayUnion(uid),
                            "vetoCount" to FieldValue.increment(1)
                        )
                    ).await()

                // Increment the user's vetoesUsed count (max 2 lifetime per tribe)
                db.collection("tribes").document(tribeId)
                    .collection("members").document(uid)
                    .update("vetoesUsed", FieldValue.increment(1)).await()

                _activeProposal.value = null
                onSuccess()
            } catch (e: Exception) {
                _error.value = "Failed to veto: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Accept a proposal — updates tribe alarmTime and all member user_alarms.
     * Called automatically when the veto window expires with no veto.
     */
    private suspend fun acceptProposal(tribeId: String, proposalId: String, proposedTime: String) {
        try {
            // Update the proposal status
            db.collection("tribes").document(tribeId)
                .collection("proposals").document(proposalId)
                .update("status", "accepted").await()

            // Update the tribe's canonical alarm time
            db.collection("tribes").document(tribeId)
                .update("alarmTime", proposedTime).await()

            // Update every member's followedAlarmTime in user_alarms
            val members = db.collection("tribes").document(tribeId)
                .collection("members").get().await()
            for (member in members.documents) {
                db.collection("user_alarms").document(member.id)
                    .update("followedAlarmTime", proposedTime).await()
            }

            _activeProposal.value = null
        } catch (e: Exception) {
            // Fail silently — another client will retry
        }
    }

    override fun onCleared() {
        super.onCleared()
        proposalListener?.remove()
    }
}
