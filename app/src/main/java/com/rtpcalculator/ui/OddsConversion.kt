package com.rtpcalculator.ui

import com.rtpcalculator.model.OddsFormat
import com.rtpcalculator.model.Outcome
import java.util.Locale

/**
 * Converts raw odds input (in a given [format]) into a normalised decimal "X.XX"
 * string, or `null` when the input is incomplete or invalid.
 *
 * The returned decimal value is what feeds `calculateRtp`, which is never changed
 * and always works on decimal odds. Conversion formulas:
 *  - DECIMAL    : parsed as today via [normalizeOddsOrNull].
 *  - FRACTIONAL : decimal = (numerator / denominator) + 1, denominator > 0.
 *  - AMERICAN   : positive -> (value / 100) + 1, negative -> (100 / value) + 1,
 *                 where `value` is the absolute value (> 0) and the sign is
 *                 carried separately by [americanNegative].
 */
fun convertToDecimalOdds(
    format: OddsFormat,
    decimalRaw: String,
    fracNumerator: String,
    fracDenominator: String,
    americanNegative: Boolean,
    americanValue: String
): String? = when (format) {
    OddsFormat.DECIMAL -> normalizeOddsOrNull(decimalRaw)

    OddsFormat.FRACTIONAL -> {
        val num = fracNumerator.toIntOrNull()
        val den = fracDenominator.toIntOrNull()
        if (num == null || den == null || num < 0 || den <= 0) null
        else formatDecimalOdds(num.toDouble() / den.toDouble() + 1.0)
    }

    OddsFormat.AMERICAN -> {
        val value = americanValue.toIntOrNull()
        if (value == null || value <= 0) null
        else {
            val decimal =
                if (americanNegative) 100.0 / value.toDouble() + 1.0
                else value.toDouble() / 100.0 + 1.0
            formatDecimalOdds(decimal)
        }
    }
}

/**
 * The display string for an outcome's odds in the format the user entered it:
 *  - FRACTIONAL : "num/den" (e.g. "5/2"); falls back to decimal if a part is missing.
 *  - AMERICAN   : sign + value (e.g. "+150", "-120"); "—" via [formatOddsForDisplay] if empty.
 *  - DECIMAL (and any null/legacy format) : the decimal odds as today.
 *
 * Display only — [calculateRtp] keeps using the decimal `odds`.
 */
fun Outcome.displayOdds(): String = when (oddsFormat) {
    OddsFormat.FRACTIONAL ->
        if (fracNumerator.isNotBlank() && fracDenominator.isNotBlank()) "$fracNumerator/$fracDenominator"
        else formatOddsForDisplay(odds)

    OddsFormat.AMERICAN ->
        if (americanValue.isNotBlank()) (if (americanNegative) "-" else "+") + americanValue
        else formatOddsForDisplay(odds)

    else -> formatOddsForDisplay(odds)
}

/** Convenience overload working directly from an [Outcome]'s raw fields. */
fun Outcome.toDecimalOddsOrNull(): String? = convertToDecimalOdds(
    format = oddsFormat,
    decimalRaw = odds,
    fracNumerator = fracNumerator,
    fracDenominator = fracDenominator,
    americanNegative = americanNegative,
    americanValue = americanValue
)

private fun formatDecimalOdds(value: Double): String =
    "%.2f".format(Locale.ENGLISH, value)
