package com.alarm.meluko.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.alarm.meluko.alarm.AlarmActivity

/**
 * AlarmReceiver
 *
 * Fired by AlarmManager when the scheduled alarm time arrives.
 * Android wakes the device (if sleeping) and delivers this broadcast.
 *
 * Why a BroadcastReceiver and not a Service?
 * AlarmManager delivers broadcasts even when the app is not running.
 * The receiver's only job is to launch AlarmActivity — it must complete
 * quickly (< 10s) before Android reclaims it.
 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmTime = intent.getStringExtra(AlarmActivity.EXTRA_ALARM_TIME) ?: "07:30"

        val activityIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra(AlarmActivity.EXTRA_ALARM_TIME, alarmTime)
            // NEW_TASK required when starting an Activity from a BroadcastReceiver
            // CLEAR_TOP prevents duplicate AlarmActivity instances on the back stack
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(activityIntent)
    }
}
