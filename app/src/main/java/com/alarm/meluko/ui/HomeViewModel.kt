package com.alarm.meluko.ui

import androidx.lifecycle.ViewModel
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

    private val _nextAlarmTime = MutableStateFlow("07:30")
    val nextAlarmTime = _nextAlarmTime.asStateFlow()

    fun dismissAlarm() {
        _isAlarmRinging.value = false
        // In a real app, we might schedule the next alarm here
    }

    fun resetAlarm() {
        _isAlarmRinging.value = true
    }
}
