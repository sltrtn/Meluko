package com.alarm.meluko.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.alarm.meluko.data.AlarmCache
import com.alarm.meluko.data.FirestoreCollection
import com.alarm.meluko.data.FirestoreField
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
        private const val AUTH_TIMEOUT_MS = 5_000L
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        val auth = FirebaseAuth.getInstance()

        // FirebaseAuth may still be restoring the persisted user immediately after boot.
        // Wait briefly for an auth-state change before deciding we're signed out.
        val currentUser = auth.currentUser
        if (currentUser != null) {
            loadAlarmAndSchedule(context, currentUser.uid, pendingResult)
            return
        }

        var listener: FirebaseAuth.AuthStateListener? = null
        val timeoutHandler = Handler(Looper.getMainLooper())

        listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid
            if (uid != null) {
                timeoutHandler.removeCallbacksAndMessages(null)
                listener?.let { auth.removeAuthStateListener(it) }
                loadAlarmAndSchedule(context, uid, pendingResult)
            }
        }

        auth.addAuthStateListener(listener)

        timeoutHandler.postDelayed({
            auth.removeAuthStateListener(listener)
            // Timeout — no signed-in user restored. Fall back to cache.
            val cachedTime = AlarmCache.getAlarmTime(context)
            if (cachedTime != null) {
                scheduleFromCachedTime(context, cachedTime)
            }
            Log.d(TAG, "Auth restore timed out, used cache: $cachedTime")
            pendingResult.finish()
        }, AUTH_TIMEOUT_MS)
    }

    private fun loadAlarmAndSchedule(context: Context, uid: String, pendingResult: android.content.BroadcastReceiver.PendingResult) {
        FirebaseFirestore.getInstance()
            .collection(FirestoreCollection.USER_ALARMS)
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val alarmTime = doc.getString(FirestoreField.FOLLOWED_ALARM_TIME)
                if (alarmTime != null) {
                    scheduleFromCachedTime(context, alarmTime)
                    AlarmCache.saveAlarmTime(context, alarmTime)
                    Log.d(TAG, "Alarm rescheduled from Firestore: $alarmTime")
                } else {
                    val cachedTime = AlarmCache.getAlarmTime(context)
                    if (cachedTime != null) {
                        scheduleFromCachedTime(context, cachedTime)
                        Log.d(TAG, "Alarm rescheduled from cache: $cachedTime")
                    } else {
                        Log.d(TAG, "No alarm time found anywhere, skipping")
                    }
                }
                pendingResult.finish()
            }
            .addOnFailureListener {
                val cachedTime = AlarmCache.getAlarmTime(context)
                if (cachedTime != null) {
                    scheduleFromCachedTime(context, cachedTime)
                    Log.d(TAG, "Firestore failed, used cache: $cachedTime")
                } else {
                    Log.w(TAG, "Firestore failed and no cache available", it)
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
