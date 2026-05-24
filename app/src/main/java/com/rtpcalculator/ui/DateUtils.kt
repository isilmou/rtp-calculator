package com.rtpcalculator.ui

import java.text.SimpleDateFormat
import java.util.Locale

private val DATE_FORMATS = listOf(
    "yyyy-MM-dd",
    "dd/MM/yyyy",
    "MM/dd/yyyy",
    "dd-MM-yyyy",
    "dd MMM yyyy",
    "MMM dd, yyyy",
    "d/M/yyyy",
    "d-M-yyyy",
    "d MMM yyyy"
)

fun parseDateToMillis(date: String): Long? {
    val trimmed = date.trim()
    if (trimmed.isEmpty()) return null
    for (fmt in DATE_FORMATS) {
        try {
            val sdf = SimpleDateFormat(fmt, Locale.ENGLISH)
            sdf.isLenient = false
            val parsed = sdf.parse(trimmed)
            if (parsed != null) return parsed.time
        } catch (_: Exception) { }
    }
    return null
}

fun parseDateTimeToMillis(date: String, time: String): Long? {
    val dateMillis = parseDateToMillis(date) ?: return null
    val parts = time.trim().split(":")
    if (parts.size != 2) return dateMillis
    val h = parts[0].toIntOrNull() ?: return dateMillis
    val m = parts[1].toIntOrNull() ?: return dateMillis
    if (h !in 0..23 || m !in 0..59) return dateMillis
    return dateMillis + h * 3_600_000L + m * 60_000L
}
