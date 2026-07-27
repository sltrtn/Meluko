package com.alarm.meluko.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alarm.meluko.alarm.AlarmScheduler
import com.alarm.meluko.data.AlarmCache
import com.alarm.meluko.data.FirestoreCollection
import com.alarm.meluko.data.FirestoreField
import com.alarm.meluko.util.ConnectivityObserver
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val connectivityObserver: ConnectivityObserver
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

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

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline = _isOnline.asStateFlow()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val uid get() = auth.currentUser?.uid

    private var userAlarmRegistration: ListenerRegistration? = null
    private var memberRegistration: ListenerRegistration? = null
    private var tribeRegistration: ListenerRegistration? = null

    init {
        // Load from local cache immediately for instant display
        val cachedTime = AlarmCache.getAlarmTime(context)
        if (cachedTime != null) {
            _nextAlarmTime.value = cachedTime
        }
        val cachedTribeId = AlarmCache.getTribeId(context)
        if (cachedTribeId != null) {
            _primaryTribeId.value = cachedTribeId
        }

        // Monitor connectivity
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online ->
                _isOnline.value = online
            }
        }

        val currentUid = uid
        if (currentUid != null) {
            userAlarmRegistration = db.collection(FirestoreCollection.USER_ALARMS).document(currentUid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w("HomeVM", "User alarm listener error", error)
                        _error.value = "Failed to load alarm data"
                        _isLoading.value = false
                        return@addSnapshotListener
                    }
                    if (snapshot == null) return@addSnapshotListener
                    _isLoading.value = false
                    val tribeId = snapshot.getString(FirestoreField.PRIMARY_TRIBE_ID)
                    _primaryTribeId.value = tribeId
                    if (tribeId != null) AlarmCache.saveTribeId(context, tribeId)

                    val time = snapshot.getString(FirestoreField.FOLLOWED_ALARM_TIME)
                        ?: return@addSnapshotListener
                    if (time == _nextAlarmTime.value && time != "--:--") return@addSnapshotListener
                    _nextAlarmTime.value = time
                    AlarmCache.saveAlarmTime(context, time)
                    scheduleAlarm(time)

                    if (tribeId != null) {
                        tribeRegistration?.remove()
                        tribeRegistration = db.collection(FirestoreCollection.TRIBES).document(tribeId)
                            .addSnapshotListener { tribeSnap, tribeError ->
                                if (tribeError != null) {
                                    Log.w("HomeVM", "Tribe listener error", tribeError)
                                    return@addSnapshotListener
                                }
                                if (tribeSnap == null || !tribeSnap.exists()) return@addSnapshotListener
                                val tribeAlarm = tribeSnap.getString(FirestoreField.ALARM_TIME)
                                if (tribeAlarm != null && tribeAlarm != time) {
                                    db.collection(FirestoreCollection.USER_ALARMS).document(currentUid)
                                        .update(FirestoreField.FOLLOWED_ALARM_TIME, tribeAlarm)
                                }
                            }

                        memberRegistration?.remove()
                        memberRegistration = db.collection(FirestoreCollection.TRIBES).document(tribeId)
                            .collection(FirestoreCollection.MEMBERS).document(currentUid)
                            .addSnapshotListener { memberSnap, memberError ->
                                if (memberError != null) {
                                    Log.w("HomeVM", "Member listener error", memberError)
                                    _error.value = "Failed to load member data"
                                    return@addSnapshotListener
                                }
                                if (memberSnap != null && memberSnap.exists()) {
                                    _streakDays.value = memberSnap.getLong(FirestoreField.STREAK_DAYS)?.toInt() ?: 0
                                    _totalWakes.value = memberSnap.getLong(FirestoreField.TOTAL_WAKES)?.toInt() ?: 0
                                    _rank.value = memberSnap.getLong(FirestoreField.RANK)?.toInt() ?: 1
                                }
                            }
                    }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        userAlarmRegistration?.remove()
        memberRegistration?.remove()
        tribeRegistration?.remove()
    }

    fun clearError() {
        _error.value = null
    }

    private fun scheduleAlarm(time: String) {
        try {
            val parts = time.split(":")
            if (parts.size != 2) return
            val hour = parts[0].toIntOrNull() ?: return
            val minute = parts[1].toIntOrNull() ?: return
            AlarmScheduler(context).schedule(hour, minute)
        } catch (e: Exception) {
            Log.w("HomeVM", "Failed to schedule alarm", e)
        }
    }

    fun dismissAlarm() {
        _isAlarmRinging.value = false
    }

    fun resetAlarm() {
        _isAlarmRinging.value = true
    }
}
