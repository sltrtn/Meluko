package com.alarm.meluko.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alarm.meluko.data.FirestoreCollection
import com.alarm.meluko.data.FirestoreField
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
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

/**
 * Proposal lifecycle. A tribe has at most one live proposal, named by
 * tribes/{id}.activeProposalId (firestore.rules enforces this). Any member's
 * app finalizes it once the 2h window passes; the window is measured from the
 * server-stamped createdAt, so device clocks can't shorten it.
 */
@HiltViewModel
class ProposalViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : ViewModel() {

    companion object {
        const val WINDOW_MS = 2 * 60 * 60 * 1000L
        const val MAX_VETOES = 2
    }

    private val uid get() = auth.currentUser?.uid ?: ""

    private val _activeProposal = MutableStateFlow<AlarmProposal?>(null)
    val activeProposal: StateFlow<AlarmProposal?> = _activeProposal.asStateFlow()

    private val _vetoesUsed = MutableStateFlow(0)
    val vetoesUsed: StateFlow<Int> = _vetoesUsed.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun clearError() { _error.value = null }

    private var listeningTribeId: String? = null
    private var tribeListener: ListenerRegistration? = null
    private var proposalListener: ListenerRegistration? = null
    private var memberListener: ListenerRegistration? = null
    private var expiryJob: Job? = null
    private var accepting = false

    fun listenToTribeProposals(tribeId: String) {
        if (listeningTribeId == tribeId && tribeListener != null) return
        stopListening()
        listeningTribeId = tribeId
        val tribeRef = db.collection(FirestoreCollection.TRIBES).document(tribeId)

        tribeListener = tribeRef.addSnapshotListener { tribeSnap, error ->
            if (error != null || tribeSnap == null) return@addSnapshotListener
            val activeId = tribeSnap.getString(FirestoreField.ACTIVE_PROPOSAL_ID)
            proposalListener?.remove()
            proposalListener = null
            if (activeId == null) {
                _activeProposal.value = null
                return@addSnapshotListener
            }
            proposalListener = tribeRef.collection(FirestoreCollection.PROPOSALS).document(activeId)
                .addSnapshotListener { snap, err ->
                    if (err != null || snap == null || !snap.exists()) {
                        _activeProposal.value = null
                        return@addSnapshotListener
                    }
                    val proposal = snap.toProposal()
                    _activeProposal.value = proposal.takeIf { it.status == FirestoreField.STATUS_PENDING }
                }
        }

        memberListener = tribeRef.collection(FirestoreCollection.MEMBERS).document(uid)
            .addSnapshotListener { snap, _ ->
                _vetoesUsed.value = snap?.getLong(FirestoreField.VETOES_USED)?.toInt() ?: 0
            }

        // Finalize on a timer too — a snapshot won't arrive just because time passed.
        expiryJob = viewModelScope.launch {
            while (isActive) {
                maybeAccept(tribeId)
                delay(20_000L)
            }
        }
    }

    private fun DocumentSnapshot.toProposal(): AlarmProposal {
        // ESTIMATE: a just-created proposal's pending server timestamp still sorts/expires sensibly.
        val created = getTimestamp(
            FirestoreField.CREATED_AT,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        )?.toDate()
        val expires = created?.let { Date(it.time + WINDOW_MS) } ?: getDate(FirestoreField.EXPIRES_AT)
        return AlarmProposal(
            id = id,
            proposedBy = getString(FirestoreField.PROPOSED_BY) ?: "",
            proposedByName = getString(FirestoreField.PROPOSED_BY_NAME) ?: "Someone",
            proposedTime = getString(FirestoreField.PROPOSED_TIME) ?: "",
            status = getString(FirestoreField.STATUS) ?: FirestoreField.STATUS_PENDING,
            vetoedBy = (get(FirestoreField.VETOED_BY) as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            vetoCount = getLong(FirestoreField.VETO_COUNT)?.toInt() ?: 0,
            createdAt = created,
            expiresAt = expires
        )
    }

    private fun maybeAccept(tribeId: String) {
        val proposal = _activeProposal.value ?: return
        val expires = proposal.expiresAt ?: return
        if (accepting || Date().before(expires) || proposal.proposedTime.isBlank()) return
        accepting = true
        viewModelScope.launch {
            try {
                val tribeRef = db.collection(FirestoreCollection.TRIBES).document(tribeId)
                db.batch().apply {
                    update(tribeRef.collection(FirestoreCollection.PROPOSALS).document(proposal.id),
                        FirestoreField.STATUS, FirestoreField.STATUS_ACCEPTED)
                    update(tribeRef, mapOf(
                        FirestoreField.ALARM_TIME to proposal.proposedTime,
                        FirestoreField.ACTIVE_PROPOSAL_ID to null
                    ))
                    set(db.collection(FirestoreCollection.USER_ALARMS).document(uid),
                        mapOf(FirestoreField.FOLLOWED_ALARM_TIME to proposal.proposedTime),
                        SetOptions.merge())
                }.commit().await()
                Log.d("ProposalVM", "Accepted proposal ${proposal.id} → ${proposal.proposedTime}")
            } catch (e: FirebaseFirestoreException) {
                // Usually: another member got there first, or our clock runs ahead of the
                // server's. The timer simply tries again; nothing to show the user.
                Log.d("ProposalVM", "Accept not possible yet: ${e.code}")
            } catch (e: Exception) {
                Log.w("ProposalVM", "Accept failed", e)
            } finally {
                accepting = false
            }
        }
    }

    fun createProposal(
        tribeId: String,
        proposedTime: String,
        onSuccess: () -> Unit,
        onError: (() -> Unit)? = null
    ) {
        if (uid.isEmpty()) { onError?.invoke(); return }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val tribeRef = db.collection(FirestoreCollection.TRIBES).document(tribeId)
                val tribe = tribeRef.get().await()
                if (tribe.getString(FirestoreField.ACTIVE_PROPOSAL_ID) != null) {
                    _error.value = "There's already a proposal open. Wait for it to resolve."
                    onError?.invoke()
                    return@launch
                }
                if (tribe.getString(FirestoreField.ALARM_TIME) == proposedTime) {
                    _error.value = "That's already the tribe's alarm time."
                    onError?.invoke()
                    return@launch
                }
                val name = db.collection(FirestoreCollection.USERS).document(uid).get().await()
                    .getString(FirestoreField.DISPLAY_NAME) ?: "A tribe member"
                val proposalId = UUID.randomUUID().toString()
                db.batch().apply {
                    set(tribeRef.collection(FirestoreCollection.PROPOSALS).document(proposalId), mapOf(
                        FirestoreField.PROPOSED_BY to uid,
                        FirestoreField.PROPOSED_BY_NAME to name,
                        FirestoreField.PROPOSED_TIME to proposedTime,
                        FirestoreField.STATUS to FirestoreField.STATUS_PENDING,
                        FirestoreField.VETOED_BY to emptyList<String>(),
                        FirestoreField.VETO_COUNT to 0,
                        FirestoreField.CREATED_AT to FieldValue.serverTimestamp()
                    ))
                    update(tribeRef, FirestoreField.ACTIVE_PROPOSAL_ID, proposalId)
                }.commit().await()
                onSuccess()
            } catch (e: Exception) {
                Log.w("ProposalVM", "Create proposal failed", e)
                _error.value = "Couldn't send the proposal. Someone may have just proposed — try again."
                onError?.invoke()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * First alarm during onboarding: a tribe of one just sets its time (nobody to
     * veto); anyone joining an existing tribe proposes instead.
     */
    fun setInitialAlarm(tribeId: String, time: String, onDone: () -> Unit) {
        if (uid.isEmpty()) { onDone(); return }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val tribeRef = db.collection(FirestoreCollection.TRIBES).document(tribeId)
                val tribe = tribeRef.get().await()
                val current = tribe.getString(FirestoreField.ALARM_TIME)
                val solo = (tribe.getLong(FirestoreField.MEMBER_COUNT) ?: 1L) <= 1L
                when {
                    time == current -> onDone()
                    solo -> {
                        tribeRef.update(FirestoreField.ALARM_TIME, time).await()
                        db.collection(FirestoreCollection.USER_ALARMS).document(uid)
                            .set(mapOf(FirestoreField.FOLLOWED_ALARM_TIME to time), SetOptions.merge()).await()
                        onDone()
                    }
                    else -> {
                        _isLoading.value = false
                        createProposal(tribeId, time, onSuccess = onDone)
                        return@launch
                    }
                }
            } catch (e: Exception) {
                Log.w("ProposalVM", "Set initial alarm failed", e)
                _error.value = "Couldn't set the alarm. Try again."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun castVeto(
        tribeId: String,
        proposalId: String,
        onSuccess: () -> Unit,
        onError: (() -> Unit)? = null
    ) {
        if (uid.isEmpty()) { onError?.invoke(); return }
        if (_vetoesUsed.value >= MAX_VETOES) {
            _error.value = "You've used both of your vetoes."
            onError?.invoke()
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val tribeRef = db.collection(FirestoreCollection.TRIBES).document(tribeId)
                // One atomic write: the veto, its cost, and freeing the tribe's proposal slot.
                db.batch().apply {
                    update(tribeRef.collection(FirestoreCollection.PROPOSALS).document(proposalId), mapOf(
                        FirestoreField.STATUS to FirestoreField.STATUS_VETOED,
                        FirestoreField.VETOED_BY to FieldValue.arrayUnion(uid),
                        FirestoreField.VETO_COUNT to FieldValue.increment(1)
                    ))
                    update(tribeRef.collection(FirestoreCollection.MEMBERS).document(uid),
                        FirestoreField.VETOES_USED, FieldValue.increment(1))
                    update(tribeRef, FirestoreField.ACTIVE_PROPOSAL_ID, null)
                }.commit().await()
                _activeProposal.value = null
                onSuccess()
            } catch (e: Exception) {
                Log.w("ProposalVM", "Veto failed", e)
                _error.value = "Couldn't veto. The window may have just closed."
                onError?.invoke()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun stopListening() {
        tribeListener?.remove(); tribeListener = null
        proposalListener?.remove(); proposalListener = null
        memberListener?.remove(); memberListener = null
        expiryJob?.cancel(); expiryJob = null
        listeningTribeId = null
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}
