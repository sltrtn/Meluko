package com.alarm.meluko.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

/**
 * AlarmScheduler
 *
 * Single responsibility: schedule and cancel alarms via AlarmManager.
 *
 * Why setExactAndAllowWhileIdle?
 *  AlarmManager has several precision levels. setExact() can be deferred by
 *  Doze mode (battery optimization). setExactAndAllowWhileIdle() fires even
 *  in Doze — important for a wake-up alarm.
 *
 * Permission note:
 *  On API 31+, exact alarms require SCHEDULE_EXACT_ALARM permission.
 *  On API 33+, USE_EXACT_ALARM is an alternative that doesn't need user grant.
 *  We declare both in the manifest and check canScheduleExactAlarms() at runtime.
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /**
     * Schedule an alarm at [hour]:[minute] for today (or tomorrow if the time
     * has already passed today).
     */
    fun schedule(hour: Int, minute: Int) {
        val triggerAtMillis = nextOccurrenceMillis(hour, minute)
        val alarmTime = "%02d:%02d".format(hour, minute)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmActivity.EXTRA_ALARM_TIME, alarmTime)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // API 31+: check permission before scheduling exact alarm
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            // If permission not granted, fall back to inexact (still works, just ±few min)
            else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    /**
     * Cancel any pending alarm.
     */
    fun cancel() {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let { alarmManager.cancel(it) }
    }

    /**
     * Returns epoch millis for the next occurrence of [hour]:[minute].
     * If that time has already passed today, schedules for tomorrow.
     */
    private fun nextOccurrenceMillis(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // If the target time is in the past, add one day
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_MONTH, 1)
        }
        return target.timeInMillis
    }

    companion object {
        private const val REQUEST_CODE = 1001
    }
}
