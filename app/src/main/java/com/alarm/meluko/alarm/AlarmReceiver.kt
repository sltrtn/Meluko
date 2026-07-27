package com.alarm.meluko.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.alarm.meluko.R

/**
 * AlarmReceiver
 *
 * Fired by AlarmManager when the scheduled alarm time arrives.
 * Android wakes the device (if sleeping) and delivers this broadcast.
 *
 * Two responsibilities:
 * 1. Post a full-screen notification that launches AlarmActivity
 * 2. Immediately reschedule the alarm for the next day (so it survives
 *    force-kills, missed dismissals, etc.)
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmTime = intent.getStringExtra(AlarmActivity.EXTRA_ALARM_TIME) ?: "07:30"
        val pendingResult = goAsync()

        // ── 1. Reschedule for tomorrow immediately ───────────────────────────
        rescheduleTomorrow(context, alarmTime)

        // ── 2. Post full-screen notification ──────────────────────────────────
        val activityIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra(AlarmActivity.EXTRA_ALARM_TIME, alarmTime)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingActivityIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_ACTIVITY,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Meluko Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Full-screen alarm alerts"
                setBypassDnd(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Meluko")
            .setContentText("Time to wake up!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingActivityIntent, true)
            .setAutoCancel(true)
            .setOngoing(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)

        pendingResult.finish()
    }

    /**
     * Read the alarm time from Firestore and schedule the next-day alarm.
     * This runs even if the user never dismisses the alarm — the next
     * morning's alarm is guaranteed to be set.
     */
    private fun rescheduleTomorrow(context: Context, alarmTime: String) {
        val parts = alarmTime.split(":")
        if (parts.size != 2) return
        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return

        AlarmScheduler(context).schedule(hour, minute)
        Log.d("AlarmReceiver", "Rescheduled alarm for tomorrow at $alarmTime")
    }

    companion object {
        private const val CHANNEL_ID = "alarm_channel"
        private const val NOTIFICATION_ID = 101
        private const val REQUEST_CODE_ACTIVITY = 1002
    }
}
