package com.rtpcalculator.ui

import com.rtpcalculator.model.OddsFormat
import com.rtpcalculator.model.Outcome
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * The display string for an outcome's odds in the format the user entered it:
 *  - FRACTIONAL : "num/den" (e.g. "5/2"); falls back to decimal if a part is missing.
 *  - AMERICAN   : sign + value (e.g. "+150", "-120"); falls back to decimal, then "—", if empty.
 *  - DECIMAL (and any null/legacy format) : the decimal odds as today.
 *
 * Display only — [calculateRtp] keeps using the decimal `odds`.
 */
fun Outcome.displayOdds(): String = when (oddsFormat) {
    OddsFormat.FRACTIONAL ->
        if (fracNumerator.isNotBlank() && fracDenominator.isNotBlank()) "$fracNumerator/$fracDenominator"
        else decimalDisplay()

    OddsFormat.AMERICAN ->
        if (americanValue.isNotBlank()) (if (americanNegative) "-" else "+") + americanValue
        else decimalDisplay()

    else -> decimalDisplay()
}

/**
 * Decimal display string using the SAME rule as the input screen (up to 3 decimals,
 * 3rd shown only if significant — [formatDecimalUpTo3]). Prefers the exact raw the user
 * typed (so a significant 3rd decimal like "2.125" / "11.205" is shown verbatim), else
 * formats the calc `odds`, else "—". Fixes the previous 2-decimal truncation.
 */
private fun Outcome.decimalDisplay(): String {
    if (decimalRaw.isNotBlank()) return decimalRaw
    val d = odds.replace(',', '.').toDoubleOrNull() ?: return "—"
    return formatDecimalUpTo3(d)
}

// ---------------------------------------------------------------------------
// Auto-conversion helpers (PURE / testable).
//
// All conversions take an exact reference decimal (the decimal of the user's last
// manual entry) and produce the raw display for another format. They never read an
// already-converted display, so rounding never accumulates.
// ---------------------------------------------------------------------------

/**
 * Exact decimal value of raw inputs in [format], as a [Double] (full precision, no 2-dp
 * rounding) or `null` if incomplete/invalid. Used to (a) set the reference on manual entry
 * and (b) recompute the calc `odds` at full precision so the TRJ matches what is displayed.
 */
fun rawToDecimalExactOrNull(
    format: OddsFormat,
    decimalRaw: String,
    fracNumerator: String,
    fracDenominator: String,
    americanNegative: Boolean,
    americanValue: String
): Double? = when (format) {
    OddsFormat.DECIMAL -> decimalRaw.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 }

    OddsFormat.FRACTIONAL -> {
        val num = fracNumerator.toIntOrNull()
        val den = fracDenominator.toIntOrNull()
        if (num == null || den == null || num < 0 || den <= 0) null
        else num.toDouble() / den.toDouble() + 1.0
    }

    OddsFormat.AMERICAN -> {
        val value = americanValue.toIntOrNull()
        if (value == null || value <= 0) null
        else if (americanNegative) 100.0 / value.toDouble() + 1.0
        else value.toDouble() / 100.0 + 1.0
    }
}

/**
 * Decimal -> American integer (conventional):
 *  - decimal d >= 2 -> (+, round((d-1)*100))
 *  - decimal d <  2 -> (-, round(100/(d-1)))
 * Returns (negative, absoluteValueString) or `null` when d <= 1 (no valid American odds).
 * Examples: 2.18 -> (false,"118") ; 1.83 -> (true,"120") ; 2.50 -> (false,"150") ; 1.50 -> (true,"200").
 */
fun decimalToAmerican(decimal: Double): Pair<Boolean, String>? {
    if (decimal <= 1.0) return null
    return if (decimal >= 2.0) {
        false to ((decimal - 1.0) * 100.0).roundToLong().toString()
    } else {
        true to (100.0 / (decimal - 1.0)).roundToLong().toString()
    }
}

/**
 * Decimal -> reduced fraction of (d - 1), over a 1/1000 grid then reduced by the GCD.
 * Returns (numerator, denominator) or `null` when d <= 1.
 * Examples: 2.18 -> 59/50 ; 3.50 -> 5/2 ; 4.33 -> 333/100.
 */
fun decimalToFraction(decimal: Double): Pair<String, String>? {
    if (decimal <= 1.0) return null
    val numerator = ((decimal - 1.0) * 1000.0).roundToLong()
    if (numerator <= 0L) return null
    val denominator = 1000L
    val g = gcd(numerator, denominator)
    return (numerator / g).toString() to (denominator / g).toString()
}

/**
 * Decimal display string for the EU field, up to 3 decimals:
 *  - value exactly representable in 3 decimals: show 3 decimals, or 2 if the 3rd is 0
 *    (e.g. 2.125 -> "2.125", 2.180 -> "2.18").
 *  - otherwise (e.g. a repeating decimal like 1.8333…) show 2 decimals (-> "1.83").
 * NOTE: a value the user TYPED with a trailing zero ("2.180") is preserved verbatim upstream
 * (decimalRaw); this function is only for CONVERSION results.
 */
fun formatDecimalUpTo3(decimal: Double): String {
    val scaled = decimal * 1000.0
    val rounded = scaled.roundToLong()
    val isThreeDecimalExact = abs(scaled - rounded) < 1e-6
    return if (isThreeDecimalExact && rounded % 10L != 0L) {
        "%.3f".format(Locale.ENGLISH, decimal)
    } else {
        "%.2f".format(Locale.ENGLISH, decimal)
    }
}

/**
 * High-precision decimal string for the calc `odds` (feeds calculateRtp). Keeps enough
 * precision that the TRJ reflects the displayed value (e.g. -120 -> "1.833333"), with
 * trailing zeros trimmed (e.g. 3.5 -> "3.5", 2.18 -> "2.18").
 */
fun exactDecimalString(decimal: Double): String =
    "%.6f".format(Locale.ENGLISH, decimal).trimEnd('0').trimEnd('.')

/**
 * Pads a (sanitised) decimal raw string so its DISPLAY has AT LEAST 2 decimals — applied to
 * user-typed EU values at blur / save / reopen. Trailing zeros the user explicitly typed are
 * preserved (so "2.180" stays "2.180", "2.100" stays "2.100"), only MISSING decimals are
 * filled with zeros. Empty input is left empty.
 *
 *  "" -> "" ; "2" -> "2.00" ; "2." -> "2.00" ; "1.5" -> "1.50" ;
 *  "2.18" -> "2.18" ; "2.125" -> "2.125" ; "2.180" -> "2.180" ; "2.100" -> "2.100".
 */
fun padDecimalToMinTwo(raw: String): String {
    if (raw.isEmpty()) return raw
    val dot = raw.indexOf('.')
    if (dot < 0) return "$raw.00"
    val decimals = raw.length - dot - 1
    return when {
        decimals >= 2 -> raw
        decimals == 1 -> raw + "0"
        else -> raw + "00" // dot with no digit after, e.g. "2."
    }
}

/**
 * Sanitises raw decimal keyboard input for the EU field: comma -> dot, digits only,
 * a single dot, at most 3 digits after the dot (blocks a 4th decimal), a leading "0" or "."
 * refused (an odds is always >= 1), redundant leading
 * zeros removed from the integer part ("0800" -> "800", see [stripLeadingZeros]). Trailing zeros the
 * user types are kept (e.g. "2.180").
 */
fun sanitizeDecimalInput(raw: String): String {
    val sb = StringBuilder()
    var hasDot = false
    var decimals = 0
    for (c in raw) {
        when {
            c.isDigit() -> {
                if (!hasDot) sb.append(c)
                else if (decimals < 3) { sb.append(c); decimals++ }
            }
            (c == '.' || c == ',') && !hasDot -> { hasDot = true; sb.append('.') }
        }
    }
    val cleaned = stripLeadingZeros(sb.toString())
    // An odds is always > 1, so a string starting with "0" or "." can never become valid:
    // refuse it outright (the keystroke is simply not shown; a pasted "0.50" is dropped).
    return if (cleaned.startsWith("0") || cleaned.startsWith(".")) "" else cleaned
}

/**
 * Removes redundant leading zeros from the integer part of a digit string: a zero is dropped
 * only when another digit follows it, so "0", "0.50" and "" are untouched.
 *  "0800" -> "800" ; "007" -> "7" ; "00.5" -> "0.5" ; "0" -> "0" ; "000" -> "0" ; "0.5" -> "0.5".
 */
fun stripLeadingZeros(raw: String): String {
    var i = 0
    while (i < raw.length - 1 && raw[i] == '0' && raw[i + 1].isDigit()) i++
    return raw.substring(i)
}

/**
 * Sanitises raw integer keyboard input for the UK (numerator / denominator) and US fields:
 * digits only, redundant leading zeros removed ([stripLeadingZeros]).
 */
fun sanitizeIntegerInput(raw: String): String {
    val cleaned = stripLeadingZeros(raw.filter(Char::isDigit))
    // Numerator, denominator and US value are strictly positive integers: a leading "0" (only
    // possible when the whole input is zeros) is refused outright.
    return if (cleaned.startsWith("0")) "" else cleaned
}

/**
 * Commit-time rule for the EU field (blur / save): a raw string that does not hold a number
 * >= 1 — a bare "." — is not an odds and is cleared to "". ("0…" never gets this far:
 * [sanitizeDecimalInput] refuses it at the keystroke.) "1" is a legitimate odds and is kept
 * (padded to "1.00" by the caller). Valid input is returned untouched; empty stays empty.
 */
fun dropInvalidDecimalInput(raw: String): String =
    if (raw.isEmpty() || (raw.toDoubleOrNull() ?: 0.0) >= 1.0) raw else ""

private tailrec fun gcd(a: Long, b: Long): Long = if (b == 0L) a else gcd(b, a % b)
