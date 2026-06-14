package com.alarm.meluko.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import com.alarm.meluko.alarm.AlarmScheduler
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _nextAlarmTime = MutableStateFlow("--:--")
    val nextAlarmTime = _nextAlarmTime.asStateFlow()

    // Kept for HomeScreen UI compatibility (debug overlay)
    private val _isAlarmRinging = MutableStateFlow(false)
    val isAlarmRinging = _isAlarmRinging.asStateFlow()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    init {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("user_alarms").document(uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val time = snapshot.getString("followedAlarmTime") ?: return@addSnapshotListener
                    if (time == _nextAlarmTime.value && time != "--:--") return@addSnapshotListener
                    _nextAlarmTime.value = time
                    // Auto-schedule the alarm whenever the time changes in Firestore
                    scheduleAlarm(time)
                }
        }
    }

    private fun scheduleAlarm(time: String) {
        try {
            val parts = time.split(":")
            if (parts.size != 2) return
            val hour = parts[0].toIntOrNull() ?: return
            val minute = parts[1].toIntOrNull() ?: return
            AlarmScheduler(context).schedule(hour, minute)
        } catch (e: Exception) {
            // Fail silently
        }
    }

    fun dismissAlarm() {
        _isAlarmRinging.value = false
    }

    fun resetAlarm() {
        _isAlarmRinging.value = true
    }
}
