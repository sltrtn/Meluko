package com.alarm.meluko.alarm

import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.alarm.meluko.data.FirestoreCollection
import com.alarm.meluko.data.FirestoreField
import com.alarm.meluko.ui.AlarmScreen
import com.alarm.meluko.ui.theme.MelukoTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.max

@AndroidEntryPoint
class AlarmActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ALARM_TIME = "extra_alarm_time"
    }

    @Inject lateinit var firebaseAuth: FirebaseAuth
    @Inject lateinit var firebaseFirestore: FirebaseFirestore

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.setBackgroundColor(Color.BLACK)

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

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        ringtone = RingtoneManager.getRingtone(this, alarmUri)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ringtone?.isLooping = true
        }
        ringtone?.play()

        val uid = firebaseAuth.currentUser?.uid
        if (uid != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val doc = firebaseFirestore.collection(FirestoreCollection.USER_ALARMS)
                        .document(uid).get().await()
                    val customUri = doc.getString(FirestoreField.ALARM_TONE_URI)
                    if (customUri != null) {
                        val customRingtone = RingtoneManager.getRingtone(this@AlarmActivity, Uri.parse(customUri))
                        if (customRingtone != null && !isDestroyed) {
                            runOnUiThread {
                                ringtone?.stop()
                                ringtone = customRingtone
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                    ringtone?.isLooping = true
                                }
                                ringtone?.play()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("AlarmActivity", "Failed to load custom tone", e)
                }
            }
        }

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
        val uid = firebaseAuth.currentUser?.uid

        if (uid != null) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            val today = dateFormat.format(Date())
            val wakeTime = timeFormat.format(Date())

            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val yesterday = dateFormat.format(cal.time)

            firebaseFirestore.collection(FirestoreCollection.USER_ALARMS).document(uid).get()
                .addOnSuccessListener { userAlarmDoc ->
                    val tribeId = userAlarmDoc.getString(FirestoreField.PRIMARY_TRIBE_ID)
                    if (tribeId != null) {
                        val memberRef = firebaseFirestore.collection(FirestoreCollection.TRIBES).document(tribeId)
                            .collection(FirestoreCollection.MEMBERS).document(uid)

                        memberRef.get().addOnSuccessListener { memberDoc ->
                            val lastWakeDate = memberDoc.getString("lastWakeDate")
                            val currentStatus = memberDoc.getString(FirestoreField.TODAY_STATUS) ?: "sleeping"

                            if (currentStatus == "awake") {
                                finish()
                                return@addOnSuccessListener
                            }

                            val currentStreak = memberDoc.getLong(FirestoreField.STREAK_DAYS)?.toInt() ?: 0
                            val currentBestStreak = memberDoc.getLong(FirestoreField.BEST_STREAK)?.toInt() ?: 0
                            val currentTotalWakes = memberDoc.getLong(FirestoreField.TOTAL_WAKES)?.toInt() ?: 0

                            val newStreak = if (lastWakeDate == yesterday) currentStreak + 1 else 1
                            val newBestStreak = max(newStreak, currentBestStreak)
                            val newTotalWakes = currentTotalWakes + 1

                            memberRef.update(
                                mapOf(
                                    FirestoreField.TODAY_STATUS to "awake",
                                    FirestoreField.TODAY_WAKE_TIME to wakeTime,
                                    FirestoreField.STREAK_DAYS to newStreak,
                                    FirestoreField.BEST_STREAK to newBestStreak,
                                    FirestoreField.TOTAL_WAKES to newTotalWakes,
                                    "lastWakeDate" to today
                                )
                            ).addOnCompleteListener {
                                // Alarm already rescheduled in AlarmReceiver.onReceive
                                // when it fired — no need to reschedule again here.
                                finish()
                            }
                        }.addOnFailureListener {
                            Log.w("AlarmActivity", "Member doc read failed", it)
                            finish()
                        }
                    } else {
                        finish()
                    }
                }
                .addOnFailureListener {
                    Log.w("AlarmActivity", "User alarm doc read failed", it)
                    finish()
                }
        } else {
            finish()
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
