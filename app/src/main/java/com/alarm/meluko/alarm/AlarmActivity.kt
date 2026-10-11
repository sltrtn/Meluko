package com.alarm.meluko.alarm

import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.alarm.meluko.data.AlarmCache
import com.alarm.meluko.data.FirestoreCollection
import com.alarm.meluko.data.FirestoreField
import com.alarm.meluko.ui.AlarmScreen
import com.alarm.meluko.ui.AlarmTribeSnapshot
import com.alarm.meluko.ui.WakeCelebration
import com.alarm.meluko.ui.theme.MelukoTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
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
        private const val CRESCENDO_MS = 30_000L
        private const val CELEBRATION_MS = 1_800L
    }

    @Inject lateinit var firebaseAuth: FirebaseAuth
    @Inject lateinit var firebaseFirestore: FirebaseFirestore

    private lateinit var tonePlayer: AlarmTonePlayer
    private var vibrator: Vibrator? = null
    private var crescendoJob: Job? = null

    private var tribeSnapshot by mutableStateOf<AlarmTribeSnapshot?>(null)
    private var celebration by mutableStateOf<WakeCelebration?>(null)

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

        // No snooze, no escape: swipe-to-wake is the only way out.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        })

        startTone()
        startVibration()
        loadTribeSnapshot()

        val reduceMotion = Settings.Global.getFloat(
            contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f
        ) == 0f

        setContent {
            MelukoTheme(darkTheme = true) {
                AlarmScreen(
                    tribe = tribeSnapshot,
                    celebration = celebration,
                    reduceMotion = reduceMotion,
                    onWake = ::handleWake
                )
            }
        }
    }

    private fun startTone() {
        tonePlayer = AlarmTonePlayer(this)
        val toneUri = AlarmCache.getAlarmToneUri(this)?.let(Uri::parse)
        tonePlayer.play(toneUri, looping = true, initialVolume = 0.1f)
        crescendoJob = lifecycleScope.launch {
            val start = System.currentTimeMillis()
            while (isActive) {
                val t = ((System.currentTimeMillis() - start).toFloat() / CRESCENDO_MS).coerceIn(0f, 1f)
                tonePlayer.setVolume(0.1f + 0.9f * t)
                if (t >= 1f) break
                delay(500)
            }
        }
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        val timings = longArrayOf(0, 800, 600)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(timings, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(timings, 0)
        }
    }

    private fun stopAlert() {
        crescendoJob?.cancel()
        tonePlayer.stop()
        vibrator?.cancel()
    }

    private suspend fun resolveTribeId(uid: String): String? =
        AlarmCache.getTribeId(this) ?: try {
            firebaseFirestore.collection(FirestoreCollection.USER_ALARMS).document(uid)
                .get().await().getString(FirestoreField.PRIMARY_TRIBE_ID)
        } catch (e: Exception) {
            null
        }

    /** Best-effort: who in the tribe is already up. Firestore serves cached data when offline. */
    private fun loadTribeSnapshot() {
        val uid = firebaseAuth.currentUser?.uid ?: return
        lifecycleScope.launch {
            try {
                val tribeId = resolveTribeId(uid) ?: return@launch
                val tribeRef = firebaseFirestore.collection(FirestoreCollection.TRIBES).document(tribeId)
                val tribeName = tribeRef.get().await().getString(FirestoreField.NAME)
                val members = tribeRef.collection(FirestoreCollection.MEMBERS).get().await().documents
                val awake = members.filter {
                    it.id != uid && it.getString(FirestoreField.TODAY_STATUS) == FirestoreField.STATUS_AWAKE
                }
                tribeSnapshot = AlarmTribeSnapshot(
                    tribeName = tribeName ?: "Your tribe",
                    awakeCount = awake.size,
                    otherCount = members.count { it.id != uid },
                    awakeMembers = awake.map {
                        (it.getString(FirestoreField.DISPLAY_NAME) ?: "?") to it.getString(FirestoreField.PHOTO_URL)
                    }
                )
            } catch (e: Exception) {
                Log.w("AlarmActivity", "Tribe snapshot unavailable", e)
            }
        }
    }

    private fun handleWake() {
        if (celebration != null) return
        stopAlert()
        celebration = WakeCelebration(streakDays = null)

        lifecycleScope.launch {
            val minDisplay = async { delay(CELEBRATION_MS) }
            val streak = withTimeoutOrNull(4_000L) { recordWake() }
            if (streak != null) celebration = WakeCelebration(streakDays = streak)
            minDisplay.await()
            if (streak != null) delay(600) // let the streak number land
            finish()
        }
    }

    /** Marks the user awake and updates streaks. Returns the resulting streak, or null on failure. */
    private suspend fun recordWake(): Int? {
        val uid = firebaseAuth.currentUser?.uid ?: return null
        return try {
            val tribeId = resolveTribeId(uid) ?: return null
            val memberRef = firebaseFirestore.collection(FirestoreCollection.TRIBES).document(tribeId)
                .collection(FirestoreCollection.MEMBERS).document(uid)

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = dateFormat.format(Date())
            val yesterday = dateFormat.format(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.time)
            val wakeTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())

            // Transaction so two rapid dismisses can't double-count.
            firebaseFirestore.runTransaction { transaction ->
                val memberDoc = transaction.get(memberRef)
                val currentStreak = memberDoc.getLong(FirestoreField.STREAK_DAYS)?.toInt() ?: 0
                val currentStatus = memberDoc.getString(FirestoreField.TODAY_STATUS) ?: FirestoreField.STATUS_SLEEPING
                if (currentStatus == FirestoreField.STATUS_AWAKE) return@runTransaction currentStreak

                val (newStreak, shouldCount) = StreakCalculator.computeNewStreak(
                    memberDoc.getString(FirestoreField.LAST_WAKE_DATE), today, yesterday, currentStreak
                )
                if (!shouldCount) return@runTransaction currentStreak

                val bestStreak = memberDoc.getLong(FirestoreField.BEST_STREAK)?.toInt() ?: 0
                val totalWakes = memberDoc.getLong(FirestoreField.TOTAL_WAKES)?.toInt() ?: 0
                transaction.update(
                    memberRef,
                    mapOf(
                        FirestoreField.TODAY_STATUS to FirestoreField.STATUS_AWAKE,
                        FirestoreField.TODAY_WAKE_TIME to wakeTime,
                        FirestoreField.STREAK_DAYS to newStreak,
                        FirestoreField.BEST_STREAK to max(newStreak, bestStreak),
                        FirestoreField.TOTAL_WAKES to totalWakes + 1,
                        FirestoreField.LAST_WAKE_DATE to today
                    )
                )
                newStreak
            }.await()
            // Alarm already rescheduled in AlarmReceiver.onReceive when it fired.
        } catch (e: Exception) {
            Log.w("AlarmActivity", "Failed to record wake", e)
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlert()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(AlarmReceiver.NOTIFICATION_ID)
        MissedAlarmReceiver.cancel(this)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
