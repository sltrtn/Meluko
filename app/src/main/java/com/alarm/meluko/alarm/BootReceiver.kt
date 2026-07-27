package com.alarm.meluko.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.alarm.meluko.data.AlarmCache
import com.alarm.meluko.data.FirestoreCollection
import com.alarm.meluko.data.FirestoreField
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            // No signed-in user — try local cache as absolute fallback
            val cachedTime = AlarmCache.getAlarmTime(context)
            if (cachedTime != null) {
                scheduleFromCachedTime(context, cachedTime)
            }
            Log.d("BootReceiver", "No signed-in user, used cache: $cachedTime")
            pendingResult.finish()
            return
        }

        // Try Firestore first
        FirebaseFirestore.getInstance()
            .collection(FirestoreCollection.USER_ALARMS)
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val alarmTime = doc.getString(FirestoreField.FOLLOWED_ALARM_TIME)
                if (alarmTime != null) {
                    scheduleFromCachedTime(context, alarmTime)
                    AlarmCache.saveAlarmTime(context, alarmTime)
                    Log.d("BootReceiver", "Alarm rescheduled from Firestore: $alarmTime")
                } else {
                    // Firestore returned no alarm — use local cache
                    val cachedTime = AlarmCache.getAlarmTime(context)
                    if (cachedTime != null) {
                        scheduleFromCachedTime(context, cachedTime)
                        Log.d("BootReceiver", "Alarm rescheduled from cache: $cachedTime")
                    } else {
                        Log.d("BootReceiver", "No alarm time found anywhere, skipping")
                    }
                }
                pendingResult.finish()
            }
            .addOnFailureListener {
                // Firestore failed — fall back to local cache
                val cachedTime = AlarmCache.getAlarmTime(context)
                if (cachedTime != null) {
                    scheduleFromCachedTime(context, cachedTime)
                    Log.d("BootReceiver", "Firestore failed, used cache: $cachedTime")
                } else {
                    Log.w("BootReceiver", "Firestore failed and no cache available", it)
                }
                pendingResult.finish()
            }
    }

    private fun scheduleFromCachedTime(context: Context, alarmTime: String) {
        val parts = alarmTime.split(":")
        val hour = parts.getOrElse(0) { "7" }.toIntOrNull() ?: 7
        val minute = parts.getOrElse(1) { "30" }.toIntOrNull() ?: 30
        AlarmScheduler(context).schedule(hour, minute)
    }
}
