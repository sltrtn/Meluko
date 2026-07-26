package com.alarm.meluko.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
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
            Log.d("BootReceiver", "No signed-in user, skipping alarm reschedule")
            pendingResult.finish()
            return
        }

        FirebaseFirestore.getInstance()
            .collection(FirestoreCollection.USER_ALARMS)
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val alarmTime = doc.getString(FirestoreField.FOLLOWED_ALARM_TIME)
                if (alarmTime != null) {
                    val parts = alarmTime.split(":")
                    val hour = parts.getOrElse(0) { "7" }.toIntOrNull() ?: 7
                    val minute = parts.getOrElse(1) { "30" }.toIntOrNull() ?: 30
                    AlarmScheduler(context).schedule(hour, minute)
                    Log.d("BootReceiver", "Alarm rescheduled for $alarmTime")
                } else {
                    Log.d("BootReceiver", "No alarm time found, skipping")
                }
                pendingResult.finish()
            }
            .addOnFailureListener {
                Log.w("BootReceiver", "Failed to read alarm doc", it)
                pendingResult.finish()
            }
    }
}
