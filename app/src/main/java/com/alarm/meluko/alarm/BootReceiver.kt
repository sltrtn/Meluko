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
import com.google.firebase.firestore.Source

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
            // Without an authenticated user we cannot verify tribe membership, so do not
            // reschedule a potentially stale cached alarm.
            Log.d(TAG, "Auth restore timed out; skipping alarm reschedule")
            pendingResult.finish()
        }, AUTH_TIMEOUT_MS)
    }

    private fun loadAlarmAndSchedule(context: Context, uid: String, pendingResult: android.content.BroadcastReceiver.PendingResult) {
        val db = FirebaseFirestore.getInstance()
        db
            .collection(FirestoreCollection.USER_ALARMS)
            .document(uid)
            .get(Source.SERVER)
            .addOnSuccessListener { doc ->
                val alarmTime = doc.getString(FirestoreField.FOLLOWED_ALARM_TIME)
                val tribeId = doc.getString(FirestoreField.PRIMARY_TRIBE_ID)
                if (alarmTime == null || tribeId == null) {
                    Log.d(TAG, "No active tribe alarm found, skipping")
                    pendingResult.finish()
                    return@addOnSuccessListener
                }

                db.collection(FirestoreCollection.TRIBES)
                    .document(tribeId)
                    .collection(FirestoreCollection.MEMBERS)
                    .document(uid)
                    .get(Source.SERVER)
                    .addOnSuccessListener { memberDoc ->
                        if (memberDoc.exists()) {
                            scheduleFromCachedTime(context, alarmTime)
                            AlarmCache.saveAlarmTime(context, alarmTime)
                            Log.d(TAG, "Alarm rescheduled for verified tribe member: $alarmTime")
                        } else {
                            Log.d(TAG, "User is no longer a tribe member; skipping alarm reschedule")
                        }
                        pendingResult.finish()
                    }
                    .addOnFailureListener {
                        Log.w(TAG, "Couldn't verify tribe membership; skipping alarm reschedule", it)
                        pendingResult.finish()
                    }
            }
            .addOnFailureListener {
                Log.w(TAG, "Couldn't load current alarm membership; skipping alarm reschedule", it)
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
