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

/** "19:05" → ("7:05", "PM"). Falls back to the raw string if it isn't HH:MM. */
fun to12HourParts(time24: String): Pair<String, String> {
    val parts = time24.split(":")
    val hour = parts.getOrNull(0)?.trim()?.toIntOrNull()
    val minute = parts.getOrNull(1)?.trim()?.take(2)?.toIntOrNull()
    if (hour == null || minute == null || hour !in 0..23 || minute !in 0..59) return time24 to ""
    val h12 = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return "%d:%02d".format(h12, minute) to if (hour >= 12) "PM" else "AM"
}

/** "19:05" → "7:05 PM". */
fun to12HourDisplay(time24: String): String {
    val (time, amPm) = to12HourParts(time24)
    return if (amPm.isEmpty()) time else "$time $amPm"
}
