package com.alarm.meluko.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreakCalculatorTest {

    @Test
    fun `waking yesterday continues streak`() {
        val (streak, shouldCount) = StreakCalculator.computeNewStreak(
            lastWakeDate = "2026-08-26",
            today = "2026-08-27",
            yesterday = "2026-08-26",
            currentStreak = 5
        )
        assertEquals(6, streak)
        assertTrue(shouldCount)
    }

    @Test
    fun `waking after a gap resets streak`() {
        val (streak, shouldCount) = StreakCalculator.computeNewStreak(
            lastWakeDate = "2026-08-24",
            today = "2026-08-27",
            yesterday = "2026-08-26",
            currentStreak = 5
        )
        assertEquals(1, streak)
        assertTrue(shouldCount)
    }

    @Test
    fun `first ever wake starts streak at 1`() {
        val (streak, shouldCount) = StreakCalculator.computeNewStreak(
            lastWakeDate = null,
            today = "2026-08-27",
            yesterday = "2026-08-26",
            currentStreak = 0
        )
        assertEquals(1, streak)
        assertTrue(shouldCount)
    }

    @Test
    fun `double wake today does not count`() {
        val (streak, shouldCount) = StreakCalculator.computeNewStreak(
            lastWakeDate = "2026-08-27",
            today = "2026-08-27",
            yesterday = "2026-08-26",
            currentStreak = 5
        )
        assertEquals(5, streak)
        assertFalse(shouldCount)
    }
}
