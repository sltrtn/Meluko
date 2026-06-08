package com.alarm.meluko.ui

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor() : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val uid = auth.currentUser?.uid ?: ""

    private val _displayName = MutableStateFlow("")
    val displayName = _displayName.asStateFlow()

    private val _username = MutableStateFlow("")
    val username = _username.asStateFlow()

    private val _photoUrl = MutableStateFlow<String?>(null)
    val photoUrl = _photoUrl.asStateFlow()

    private val _streakDays = MutableStateFlow(0)
    val streakDays = _streakDays.asStateFlow()

    private val _bestStreak = MutableStateFlow(0)
    val bestStreak = _bestStreak.asStateFlow()

    private val _totalWakes = MutableStateFlow(0)
    val totalWakes = _totalWakes.asStateFlow()

    init {
        if (uid.isNotEmpty()) {
            loadUserData()
        }
    }

    private fun loadUserData() {
        // Listen to User Profile
        db.collection("users").document(uid)
            .addSnapshotListener { snap, _ ->
                if (snap != null && snap.exists()) {
                    _displayName.value = snap.getString("displayName") ?: "Unknown User"
                    _username.value = snap.getString("username") ?: "user"
                    _photoUrl.value = snap.getString("photoUrl")
                }
            }

        // Listen to User Stats from primary tribe
        db.collection("user_alarms").document(uid).addSnapshotListener { userAlarmSnap, _ ->
            val primaryTribeId = userAlarmSnap?.getString("primaryTribeId")
            if (primaryTribeId != null) {
                db.collection("tribes").document(primaryTribeId).collection("members").document(uid)
                    .addSnapshotListener { snap, _ ->
                        if (snap != null && snap.exists()) {
                            _streakDays.value = snap.getLong("streakDays")?.toInt() ?: 0
                            _bestStreak.value = snap.getLong("bestStreak")?.toInt() ?: 0
                            _totalWakes.value = snap.getLong("totalWakes")?.toInt() ?: 0
                        }
                    }
            }
        }
    }
    
    fun signOut() {
        auth.signOut()
    }
}
