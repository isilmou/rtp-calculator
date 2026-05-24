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
    val americanValue: String = ""
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
