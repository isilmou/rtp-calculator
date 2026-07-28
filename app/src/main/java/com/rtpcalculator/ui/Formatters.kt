package com.rtpcalculator.ui

import java.util.Locale

/**
 * Parses a user-entered odds string (with `,` or `.` decimal) and returns
 * a normalised "X.XX" representation, or `null` if it is not a valid number.
 */
fun normalizeOddsOrNull(raw: String): String? {
    val d = raw.replace(',', '.').toDoubleOrNull() ?: return null
    return "%.2f".format(Locale.ENGLISH, d)
}

fun formatOddsTwoDecimals(raw: String): String =
    normalizeOddsOrNull(raw) ?: raw
