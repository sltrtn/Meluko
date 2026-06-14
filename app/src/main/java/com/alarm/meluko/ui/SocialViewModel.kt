package com.alarm.meluko.ui

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SocialViewModel @Inject constructor() : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val uid = auth.currentUser?.uid ?: ""

    private val _members = MutableStateFlow<List<TribeMember>>(emptyList())
    val members = _members.asStateFlow()

    private val _tribes = MutableStateFlow<List<TribeProgress>>(emptyList())
    val tribes = _tribes.asStateFlow()

    init {
        if (uid.isNotEmpty()) loadPrimaryTribeMembers()
    }

    fun loadMembersForTribe(tribeId: String) {
        db.collection("tribes").document(tribeId).collection("members")
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    _members.value = snap.documents.mapNotNull { doc ->
                        TribeMember(
                            name = doc.getString("displayName") ?: "Unknown",
                            photoUrl = doc.getString("photoUrl"),
                            isYou = doc.id == uid,
                            streakDays = doc.getLong("streakDays")?.toInt() ?: 0,
                            status = when (doc.getString("todayStatus")) {
                                "awake" -> WakeStatus.Awake
                                "missed" -> WakeStatus.Missed
                                else -> WakeStatus.Sleeping
                            },
                            wakeTime = doc.getString("todayWakeTime")
                        )
                    }.sortedByDescending { it.streakDays }
                } else {
                    _members.value = emptyList()
                }
            }
    }

    private fun loadPrimaryTribeMembers() {
        db.collection("user_alarms").document(uid).addSnapshotListener { userAlarmSnap, _ ->
            val primaryTribeId = userAlarmSnap?.getString("primaryTribeId") ?: return@addSnapshotListener

            db.collection("tribes").document(primaryTribeId).addSnapshotListener { tribeSnap, _ ->
                if (tribeSnap != null && tribeSnap.exists()) {
                    val name = tribeSnap.getString("name") ?: "Your Tribe"
                    val memberCount = tribeSnap.getLong("memberCount")?.toInt() ?: 1
                    val photoUrl = tribeSnap.getString("photoUrl")
                    _tribes.value = listOf(TribeProgress(name, photoUrl, 0, memberCount))
                }
            }

            loadMembersForTribe(primaryTribeId)
        }
    }
}
