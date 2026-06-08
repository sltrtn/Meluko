package com.alarm.meluko.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.alarm.meluko.R
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
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            1002, // Unique request code for AlarmActivity launch
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "alarm_channel",
                "Meluko Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for ringing alarms"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, "alarm_channel")
            .setSmallIcon(R.mipmap.ic_launcher) // Use app icon as fallback
            .setContentTitle("Meluko Alarm")
            .setContentText("It's time to wake up!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true) // TRUE is crucial to wake the screen and launch immediately
            .setAutoCancel(true)
            .build()

        notificationManager.notify(101, notification)
    }
}
