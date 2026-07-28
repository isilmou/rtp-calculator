package com.rtpcalculator.model

import androidx.compose.runtime.Immutable
import java.util.UUID

enum class OddsFormat { DECIMAL, FRACTIONAL, AMERICAN }

@Immutable
data class Outcome(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "",
    val odds: String = "",
    val bookmaker: String = "",
    val oddsFormat: OddsFormat = OddsFormat.DECIMAL,
    val fracNumerator: String = "",
    val fracDenominator: String = "",
    val americanNegative: Boolean = false,
    val americanValue: String = "",
    // Raw decimal (EU) DISPLAY string: exactly what the user typed (e.g. "2.180", trailing
    // zero preserved) when entered in decimal, or the formatted result when converted.
    val decimalRaw: String = "",
    // Exact decimal reference = the decimal translation of the user's LAST MANUAL entry.
    // ALL format conversions start from this value (never from an already-converted display),
    // so rounding never accumulates. Updated only on a manual edit.
    val refDecimal: String = ""
)

@Immutable
data class BetAnalysis(
    val id: String = UUID.randomUUID().toString(),
    val date: String = "",
    val time: String = "",
    val eventName: String = "",
    val betType: String = "",
    val outcomes: List<Outcome> = listOf(Outcome(), Outcome()),
    val savedAt: Long = 0L
)

/**
 * "Generous" guard for the silent auto-save on exit: returns true as soon as the form holds the
 * slightest real input, in ANY odds format. Used so leaving the form (back button or close icon)
 * persists a genuine analysis instead of discarding it, while a pristine/blank form is dropped.
 */
fun BetAnalysis.hasMeaningfulContent(): Boolean {
    if (eventName.isNotBlank()) return true
    if (betType.isNotBlank()) return true
    return outcomes.any { o ->
        o.label.isNotBlank() ||
        o.bookmaker.isNotBlank() ||
        o.decimalRaw.isNotBlank() ||
        o.fracNumerator.isNotBlank() ||
        o.fracDenominator.isNotBlank() ||
        o.americanValue.isNotBlank() ||
        (o.odds.toDoubleOrNull()?.let { it > 0.0 } == true)
    }
}

@Immutable
data class RtpResult(
    val rtp: Double,
    val margin: Double,
    val isSurebet: Boolean = false
)

fun calculateRtp(outcomes: List<Outcome>): RtpResult? {
    val odds = outcomes.mapNotNull { it.odds.toDoubleOrNull() }.filter { it > 0 }
    if (odds.size < 2) return null
    val impliedProbSum = odds.sumOf { 1.0 / it }
    if (impliedProbSum <= 0) return null
    val rtp = (1.0 / impliedProbSum) * 100.0
    val margin = 100.0 - rtp
    return RtpResult(rtp = rtp, margin = margin, isSurebet = rtp > 100.0)
}
