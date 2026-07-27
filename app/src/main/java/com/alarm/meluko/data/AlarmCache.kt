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

    fun saveAlarmToneUri(context: Context, uri: String) {
        prefs(context).edit().putString(KEY_ALARM_TONE_URI, uri).apply()
    }

    fun getAlarmToneUri(context: Context): String? =
        prefs(context).getString(KEY_ALARM_TONE_URI, null)

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
