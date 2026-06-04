package com.alarm.meluko.alarm

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.alarm.meluko.ui.AlarmScreen
import com.alarm.meluko.ui.theme.MelukoTheme

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

        val alarmTime = intent.getStringExtra(EXTRA_ALARM_TIME) ?: "07:30"

        setContent {
            // Force dark theme — the alarm screen has its own dark gradient background
            MelukoTheme(darkTheme = true) {
                AlarmScreen(
                    alarmTime = alarmTime,
                    onDismiss = {
                        // TODO: mark as "Awake" in Firestore, update streak
                        finish()
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clear FLAG_KEEP_SCREEN_ON when alarm is dismissed
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
