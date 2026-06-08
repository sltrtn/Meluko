package com.alarm.meluko.alarm

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.alarm.meluko.ui.AlarmScreen
import com.alarm.meluko.ui.theme.MelukoTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.media.RingtoneManager
import android.media.Ringtone
import android.os.Vibrator
import android.os.VibrationEffect
import android.content.Context
import android.app.NotificationManager

/**
 * AlarmActivity
 *
 * A dedicated Activity for the alarm ringing experience.
 * Separate from MainActivity so it can:
 *  1. Show over the lock screen (user doesn't need to unlock to dismiss)
 *  2. Turn the screen on automatically
 *  3. Keep the screen on while ringing
 *  4. Be launched directly by AlarmReceiver without navigating the app stack
 *
 * Lock screen flags explanation:
 *  - setShowWhenLocked(true)  → renders on top of the keyguard (API 27+)
 *  - setTurnScreenOn(true)    → wakes the display when this activity starts (API 27+)
 *  - FLAG_KEEP_SCREEN_ON      → prevents screen from dimming while alarm is active
 */
class AlarmActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ALARM_TIME = "extra_alarm_time"
    }

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── Show over lock screen ─────────────────────────────────────────────
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

        // Keep display on for the full alarm duration
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        enableEdgeToEdge()

        // ── Start Audio & Vibration ───────────────────────────────────────────
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        ringtone = RingtoneManager.getRingtone(this, alarmUri)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ringtone?.isLooping = true
        }
        ringtone?.play()

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 1000, 1000)
            vibrator?.vibrate(VibrationEffect.createWaveform(timings, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 1000, 1000), 0)
        }

        val alarmTime = intent.getStringExtra(EXTRA_ALARM_TIME) ?: "07:30"

        setContent {
            // Force dark theme — the alarm screen has its own dark gradient background
            MelukoTheme(darkTheme = true) {
                AlarmScreen(
                    alarmTime = alarmTime,
                    onDismiss = {
                        val uid = FirebaseAuth.getInstance().currentUser?.uid
                        if (uid != null) {
                            FirebaseFirestore.getInstance().collection("user_alarms").document(uid).get()
                                .addOnSuccessListener { userAlarmDoc ->
                                    val tribeId = userAlarmDoc.getString("primaryTribeId")
                                    if (tribeId != null) {
                                        val wakeTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())

                                        // Write awake status to tribe member doc
                                        FirebaseFirestore.getInstance()
                                            .collection("tribes").document(tribeId)
                                            .collection("members").document(uid)
                                            .update(
                                                mapOf(
                                                    "todayStatus" to "awake",
                                                    "todayWakeTime" to wakeTime
                                                )
                                            )
                                    }
                                }
                        }
                        finish()
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop audio and vibration when activity is destroyed
        ringtone?.stop()
        vibrator?.cancel()
        
        // Clear the FullScreenIntent notification
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(101)

        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
