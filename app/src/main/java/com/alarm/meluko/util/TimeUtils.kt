package com.alarm.meluko.util

/**
 * Convert a 12-hour wheel picker value to a canonical 24-hour "HH:MM" string.
 *
 * @param hour12   hour as displayed on a 12h wheel (01..12)
 * @param minute   minute as displayed (00..59)
 * @param amPm     "AM" or "PM"
 */
fun to24HourTime(hour12: String, minute: String, amPm: String): String {
    val hour24 = when {
        amPm == "AM" && hour12 == "12" -> "00"
        amPm == "PM" && hour12 != "12" -> {
            val h = hour12.toIntOrNull() ?: 12
            "%02d".format((h % 12) + 12)
        }
        else -> hour12.padStart(2, '0')
    }
    return "$hour24:${minute.padStart(2, '0')}"
}
