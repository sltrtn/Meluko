package com.alarm.meluko.alarm

import android.app.NotificationManager
import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.alarm.meluko.ui.AlarmScreen
import com.alarm.meluko.ui.theme.MelukoTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

class AlarmActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ALARM_TIME = "extra_alarm_time"
    }

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()

        // Start audio
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        ringtone = RingtoneManager.getRingtone(this, alarmUri)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ringtone?.isLooping = true
        }
        ringtone?.play()

        // Start vibration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibrator = vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        val timings = longArrayOf(0, 800, 600)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(timings, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(timings, 0)
        }

        val alarmTime = intent.getStringExtra(EXTRA_ALARM_TIME) ?: "07:30"

        setContent {
            MelukoTheme(darkTheme = true) {
                AlarmScreen(
                    alarmTime = alarmTime,
                    onDismiss = {
                        handleDismiss(alarmTime)
                    }
                )
            }
        }
    }

    private fun handleDismiss(alarmTime: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val db = FirebaseFirestore.getInstance()

        if (uid != null) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            val today = dateFormat.format(Date())
            val wakeTime = timeFormat.format(Date())

            // Calculate yesterday's date string
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val yesterday = dateFormat.format(cal.time)

            db.collection("user_alarms").document(uid).get()
                .addOnSuccessListener { userAlarmDoc ->
                    val tribeId = userAlarmDoc.getString("primaryTribeId")
                    if (tribeId != null) {
                        val memberRef = db.collection("tribes").document(tribeId)
                            .collection("members").document(uid)

                        memberRef.get().addOnSuccessListener { memberDoc ->
                            val lastWakeDate = memberDoc.getString("lastWakeDate")
                            val currentStatus = memberDoc.getString("todayStatus") ?: "sleeping"

                            // Avoid double-counting if dismissed twice
                            if (currentStatus == "awake") {
                                finish()
                                return@addOnSuccessListener
                            }

                            val currentStreak = memberDoc.getLong("streakDays")?.toInt() ?: 0
                            val currentBestStreak = memberDoc.getLong("bestStreak")?.toInt() ?: 0
                            val currentTotalWakes = memberDoc.getLong("totalWakes")?.toInt() ?: 0

                            // Streak continues if they woke up yesterday, otherwise resets to 1
                            val newStreak = if (lastWakeDate == yesterday) currentStreak + 1 else 1
                            val newBestStreak = max(newStreak, currentBestStreak)
                            val newTotalWakes = currentTotalWakes + 1

                            memberRef.update(
                                mapOf(
                                    "todayStatus" to "awake",
                                    "todayWakeTime" to wakeTime,
                                    "streakDays" to newStreak,
                                    "bestStreak" to newBestStreak,
                                    "totalWakes" to newTotalWakes,
                                    "lastWakeDate" to today
                                )
                            ).addOnCompleteListener {
                                // Reschedule alarm for tomorrow with the same time
                                rescheduleAlarmForTomorrow(alarmTime, userAlarmDoc)
                                finish()
                            }
                        }.addOnFailureListener { finish() }
                    } else {
                        finish()
                    }
                }
                .addOnFailureListener { finish() }
        } else {
            finish()
        }
    }

    private fun rescheduleAlarmForTomorrow(
        alarmTime: String,
        userAlarmDoc: com.google.firebase.firestore.DocumentSnapshot
    ) {
        // Only reschedule if alarms are active
        val isActive = userAlarmDoc.getBoolean("active") ?: true
        if (!isActive) return

        try {
            val parts = alarmTime.split(":")
            if (parts.size != 2) return
            val hour = parts[0].toIntOrNull() ?: return
            val minute = parts[1].toIntOrNull() ?: return
            // AlarmScheduler.schedule() already handles today vs tomorrow logic:
            // if the time has passed today it schedules for tomorrow.
            AlarmScheduler(applicationContext).schedule(hour, minute)
        } catch (e: Exception) {
            // Fail silently
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ringtone?.stop()
        vibrator?.cancel()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(101)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
