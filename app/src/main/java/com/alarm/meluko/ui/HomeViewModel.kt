package com.alarm.meluko.ui

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * HomeViewModel handles the state of the Home screen, including alarm status.
 */
@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {
    private val _isAlarmRinging = MutableStateFlow(true)
    val isAlarmRinging = _isAlarmRinging.asStateFlow()

    private val _nextAlarmTime = MutableStateFlow("--:--")
    val nextAlarmTime = _nextAlarmTime.asStateFlow()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    init {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            // Listen to the user's alarm config in real-time
            db.collection("user_alarms").document(uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    
                    val time = snapshot?.getString("followedAlarmTime") ?: "--:--"
                    _nextAlarmTime.value = time
                }
        }
    }

    fun dismissAlarm() {
        _isAlarmRinging.value = false
        // In a real app, we might schedule the next alarm here
    }

    fun resetAlarm() {
        _isAlarmRinging.value = true
    }
}
