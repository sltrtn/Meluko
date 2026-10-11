package com.alarm.meluko.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log

/**
 * Plays a tone on the ALARM stream (alarm volume, not ringer volume), so it
 * still rings when the phone is on silent. Falls back to the system default
 * alarm sound if the chosen URI can't be played.
 */
class AlarmTonePlayer(private val context: Context) {

    private var player: MediaPlayer? = null

    fun play(uri: Uri?, looping: Boolean, initialVolume: Float = 1f) {
        stop()
        val candidates = listOfNotNull(
            uri,
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        )
        for (candidate in candidates) {
            try {
                player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(context, candidate)
                    isLooping = looping
                    setVolume(initialVolume, initialVolume)
                    prepare()
                    start()
                }
                return
            } catch (e: Exception) {
                Log.w("AlarmTonePlayer", "Can't play $candidate, trying fallback", e)
                player?.release()
                player = null
            }
        }
    }

    fun setVolume(volume: Float) {
        player?.setVolume(volume, volume)
    }

    fun stop() {
        player?.run {
            try { stop() } catch (_: IllegalStateException) {}
            release()
        }
        player = null
    }
}
