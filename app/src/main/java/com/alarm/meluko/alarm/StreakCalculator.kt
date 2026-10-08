package com.alarm.meluko.alarm

/**
 * Pure helper for streak math.
 *
 * @param lastWakeDate date of the last wake in "yyyy-MM-dd", or null
 * @param today        today's date in "yyyy-MM-dd"
 * @param yesterday    yesterday's date in "yyyy-MM-dd"
 * @param currentStreak current streak value (only used when lastWakeDate == yesterday)
 */
object StreakCalculator {
    fun computeNewStreak(
        lastWakeDate: String?,
        today: String,
        yesterday: String,
        currentStreak: Int
    ): Pair<Int, Boolean> {
        // If the user already woke today, do not double-count.
        if (lastWakeDate == today) {
            return currentStreak to false
        }
        val newStreak = if (lastWakeDate == yesterday) currentStreak + 1 else 1
        return newStreak to true
    }
}
