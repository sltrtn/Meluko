package com.alarm.meluko.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.alarm.meluko.data.AlarmCache
import com.alarm.meluko.data.FirestoreCollection
import com.alarm.meluko.data.FirestoreField
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * MissedAlarmReceiver
 *
 * Fired a grace period (e.g. 10 minutes) after the main alarm.
 * If the current user is still marked sleeping, they missed the alarm:
 *   - todayStatus -> missed
 *   - streakDays  -> 0
 *
 * Each device only updates its own user document; Firestore listeners
 * propagate the status to every tribe member's screen.
 */
class MissedAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            Log.d(TAG, "No signed-in user, cannot mark missed")
            pendingResult.finish()
            return
        }

        val tribeId = AlarmCache.getTribeId(context)
        if (tribeId == null) {
            Log.d(TAG, "No cached tribe, cannot mark missed")
            pendingResult.finish()
            return
        }

        val memberRef = FirebaseFirestore.getInstance()
            .collection(FirestoreCollection.TRIBES).document(tribeId)
            .collection(FirestoreCollection.MEMBERS).document(uid)

        memberRef.get()
            .addOnSuccessListener { doc ->
                val status = doc.getString(FirestoreField.TODAY_STATUS) ?: FirestoreField.STATUS_SLEEPING
                if (status == FirestoreField.STATUS_SLEEPING) {
                    memberRef.update(
                        mapOf(
                            FirestoreField.TODAY_STATUS to FirestoreField.STATUS_MISSED,
                            FirestoreField.STREAK_DAYS to 0
                        )
                    ).addOnCompleteListener {
                        Log.d(TAG, "Marked user as missed")
                        pendingResult.finish()
                    }
                } else {
                    Log.d(TAG, "User already $status, no missed update needed")
                    pendingResult.finish()
                }
            }
            .addOnFailureListener {
                Log.w(TAG, "Failed to read member doc for missed check", it)
                pendingResult.finish()
            }
    }

    companion object {
        private const val TAG = "MissedAlarmReceiver"
        private const val MISSED_REQUEST_CODE = 1003
        private const val GRACE_PERIOD_MINUTES = 10

        /**
         * Schedule a missed-check alarm GRACE_PERIOD_MINUTES after the main alarm.
         */
        fun schedule(context: Context, alarmTime: String) {
            val parts = alarmTime.split(":")
            if (parts.size != 2) return
            val hour = parts[0].toIntOrNull() ?: return
            val minute = parts[1].toIntOrNull() ?: return

            val alarmManager = context.getSystemService(AlarmManager::class.java)
            val triggerAtMillis = nextOccurrenceMillis(hour, minute) + (GRACE_PERIOD_MINUTES * 60 * 1000L)

            val intent = Intent(context, MissedAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                MISSED_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }

            Log.d(TAG, "Scheduled missed check for $alarmTime + $GRACE_PERIOD_MINUTES min")
        }

        fun cancel(context: Context) {
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            val intent = Intent(context, MissedAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                MISSED_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let { alarmManager.cancel(it) }
        }

        private fun nextOccurrenceMillis(hour: Int, minute: Int): Long {
            val now = java.util.Calendar.getInstance()
            val target = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, hour)
                set(java.util.Calendar.MINUTE, minute)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            if (target.before(now)) {
                target.add(java.util.Calendar.DAY_OF_MONTH, 1)
            }
            return target.timeInMillis
        }
    }
}
