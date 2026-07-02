package com.alarm.meluko.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import com.alarm.meluko.alarm.AlarmScheduler
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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

    private val _primaryTribeId = MutableStateFlow<String?>(null)
    val primaryTribeId = _primaryTribeId.asStateFlow()

    private val _streakDays = MutableStateFlow(0)
    val streakDays = _streakDays.asStateFlow()

    private val _totalWakes = MutableStateFlow(0)
    val totalWakes = _totalWakes.asStateFlow()

    private val _rank = MutableStateFlow(1)
    val rank = _rank.asStateFlow()

    private val _isAlarmRinging = MutableStateFlow(false)
    val isAlarmRinging = _isAlarmRinging.asStateFlow()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val uid get() = auth.currentUser?.uid

    private var memberRegistration: ListenerRegistration? = null

    init {
        val currentUid = uid
        if (currentUid != null) {
            db.collection("user_alarms").document(currentUid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val tribeId = snapshot.getString("primaryTribeId")
                    _primaryTribeId.value = tribeId
                    val time = snapshot.getString("followedAlarmTime") ?: return@addSnapshotListener
                    if (time == _nextAlarmTime.value && time != "--:--") return@addSnapshotListener
                    _nextAlarmTime.value = time
                    scheduleAlarm(time)

                    // Set up member stats listener when tribeId is known
                    if (tribeId != null) {
                        memberRegistration?.remove()
                        memberRegistration = db.collection("tribes").document(tribeId)
                            .collection("members").document(currentUid)
                            .addSnapshotListener { memberSnap, _ ->
                                if (memberSnap != null && memberSnap.exists()) {
                                    _streakDays.value = memberSnap.getLong("streakDays")?.toInt() ?: 0
                                    _totalWakes.value = memberSnap.getLong("totalWakes")?.toInt() ?: 0
                                    _rank.value = memberSnap.getLong("rank")?.toInt() ?: 1
                                }
                            }
                    }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        memberRegistration?.remove()
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
