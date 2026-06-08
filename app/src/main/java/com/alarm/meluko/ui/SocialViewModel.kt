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
        // dynamic now

        if (uid.isNotEmpty()) {
            loadPrimaryTribeMembers()
        }
    }

    private fun loadPrimaryTribeMembers() {
        // Read primaryTribeId from user_alarms
        db.collection("user_alarms").document(uid).addSnapshotListener { userAlarmSnap, _ ->
            val primaryTribeId = userAlarmSnap?.getString("primaryTribeId")
            if (primaryTribeId != null) {
                // Fetch tribe info for the list
                db.collection("tribes").document(primaryTribeId).addSnapshotListener { tribeSnap, _ ->
                    if (tribeSnap != null && tribeSnap.exists()) {
                        val name = tribeSnap.getString("name") ?: "Unknown Tribe"
                        val memberCount = tribeSnap.getLong("memberCount")?.toInt() ?: 1
                        _tribes.value = listOf(TribeProgress(name, null, 1, memberCount))
                    }
                }
                
                db.collection("tribes").document(primaryTribeId).collection("members")
            .addSnapshotListener { snap, _ ->
                if (snap != null && !snap.isEmpty) {
                    val membersList = snap.documents.mapNotNull { doc ->
                        TribeMember(
                            name       = doc.getString("displayName") ?: "Unknown",
                            photoUrl   = doc.getString("photoUrl"),
                            isYou      = doc.id == uid,
                            streakDays = doc.getLong("streakDays")?.toInt() ?: 0,
                            status     = when (doc.getString("todayStatus")) {
                                "awake"   -> WakeStatus.Awake
                                "missed"  -> WakeStatus.Missed
                                else      -> WakeStatus.Sleeping
                            },
                            wakeTime   = doc.getString("todayWakeTime")
                        )
                    }.sortedByDescending { it.streakDays }
                    _members.value = membersList
                } else {
                    // Fallback to sample data so the UI doesn't look empty before DB is populated
                    _members.value = listOf(
                        TribeMember("Aisha",  null, false, 14, WakeStatus.Awake,    "6:47 AM"),
                        TribeMember("You",    null, true,  12, WakeStatus.Awake,    "7:28 AM"),
                        TribeMember("Sachin", null, false,  8, WakeStatus.Awake,    "7:02 AM"),
                        TribeMember("Priya",  null, false,  5, WakeStatus.Sleeping, null),
                        TribeMember("Joe",    null, false,  3, WakeStatus.Missed,   null),
                        TribeMember("Riya",   null, false,  2, WakeStatus.Awake,    "7:15 AM")
                    ).sortedByDescending { it.streakDays }
                }
            }
        }
    }
}
}
