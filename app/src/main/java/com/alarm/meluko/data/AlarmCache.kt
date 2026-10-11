package com.alarm.meluko.data

import android.content.Context
import android.content.SharedPreferences

/**
 * AlarmCache — thin SharedPreferences layer for alarm data persistence.
 *
 * Why: BootReceiver and AlarmReceiver need alarm time even when offline.
 * Firestore is the source of truth; this is a fallback for system-level
 * reliability.
 */
object AlarmCache {

    private const val PREFS_NAME = "meluko_alarm_cache"
    private const val KEY_ALARM_TIME = "alarm_time"
    private const val KEY_TRIBE_ID = "tribe_id"
    private const val KEY_ALARM_TONE_URI = "alarm_tone_uri"
    private const val KEY_ALARM_TONE_TITLE = "alarm_tone_title"
    private const val KEY_ALARM_TONE_CHOSEN = "alarm_tone_chosen"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveAlarmTime(context: Context, time: String) {
        prefs(context).edit().putString(KEY_ALARM_TIME, time).apply()
    }

    fun getAlarmTime(context: Context): String? =
        prefs(context).getString(KEY_ALARM_TIME, null)

    fun saveTribeId(context: Context, tribeId: String) {
        prefs(context).edit().putString(KEY_TRIBE_ID, tribeId).apply()
    }

    fun getTribeId(context: Context): String? =
        prefs(context).getString(KEY_TRIBE_ID, null)

    /** Tone is device-local (content URIs don't exist on other devices), so it lives here, not in Firestore. Null = system default. */
    fun saveAlarmTone(context: Context, uri: String?, title: String?) {
        prefs(context).edit()
            .putString(KEY_ALARM_TONE_URI, uri)
            .putString(KEY_ALARM_TONE_TITLE, title)
            .putBoolean(KEY_ALARM_TONE_CHOSEN, true)
            .apply()
    }

    fun hasChosenAlarmTone(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ALARM_TONE_CHOSEN, false)

    fun getAlarmToneUri(context: Context): String? =
        prefs(context).getString(KEY_ALARM_TONE_URI, null)

    fun getAlarmToneTitle(context: Context): String? =
        prefs(context).getString(KEY_ALARM_TONE_TITLE, null)

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
