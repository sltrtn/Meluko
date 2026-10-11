package com.alarm.meluko.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeUtilsTest {

    @Test
    fun `midnight AM converts to 00`() {
        assertEquals("00:00", to24HourTime("12", "00", "AM"))
    }

    @Test
    fun `noon PM stays 12`() {
        assertEquals("12:30", to24HourTime("12", "30", "PM"))
    }

    @Test
    fun `1 AM converts to 01`() {
        assertEquals("01:05", to24HourTime("1", "5", "AM"))
    }

    @Test
    fun `1 PM converts to 13`() {
        assertEquals("13:05", to24HourTime("1", "5", "PM"))
    }

    @Test
    fun `11 PM converts to 23`() {
        assertEquals("23:59", to24HourTime("11", "59", "PM"))
    }

    @Test
    fun `11 AM stays 11`() {
        assertEquals("11:59", to24HourTime("11", "59", "AM"))
    }

    @Test
    fun `12h display covers midnight noon and evening`() {
        assertEquals("12:00 AM", to12HourDisplay("00:00"))
        assertEquals("7:30 AM", to12HourDisplay("07:30"))
        assertEquals("12:15 PM", to12HourDisplay("12:15"))
        assertEquals("7:05 PM", to12HourDisplay("19:05"))
    }

    @Test
    fun `12h display passes through unparseable input`() {
        assertEquals("--:--", to12HourDisplay("--:--"))
    }
}
