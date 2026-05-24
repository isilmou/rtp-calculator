package com.rtpcalculator

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rtpcalculator.data.withLegacyOddsDefaults
import com.rtpcalculator.model.BetAnalysis
import com.rtpcalculator.model.OddsFormat
import com.rtpcalculator.model.calculateRtp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Simulates loading analyses serialised by the pre-format model (no oddsFormat,
 * no frac/american fields). They must load without crashing and behave as decimal.
 */
class HistoryRetroCompatTest {

    private val gson = Gson()
    private val type = object : TypeToken<List<BetAnalysis>>() {}.type

    private val legacyJson = """
        [{
          "id":"a1","date":"01/01/2024","time":"20:45","eventName":"Old Match",
          "betType":"1X2","savedAt":1700000000000,
          "outcomes":[
            {"id":"o1","label":"Home","odds":"2.50","bookmaker":"BookA"},
            {"id":"o2","label":"Away","odds":"3.00","bookmaker":"BookB"}
          ]
        }]
    """.trimIndent()

    @Test
    fun legacyAnalysisLoadsAsDecimalWithoutCrash() {
        val parsed: List<BetAnalysis> = gson.fromJson(legacyJson, type)
        val sanitized = parsed.withLegacyOddsDefaults()

        val outcome = sanitized.first().outcomes.first()
        assertEquals(OddsFormat.DECIMAL, outcome.oddsFormat)
        assertEquals("", outcome.fracNumerator)
        assertEquals("", outcome.fracDenominator)
        assertEquals("", outcome.americanValue)
        assertEquals(false, outcome.americanNegative)
    }

    @Test
    fun legacyExistingFieldsArePreserved() {
        val sanitized = gson.fromJson<List<BetAnalysis>>(legacyJson, type).withLegacyOddsDefaults()
        val outcome = sanitized.first().outcomes.first()

        assertEquals("Home", outcome.label)
        assertEquals("2.50", outcome.odds)
        assertEquals("BookA", outcome.bookmaker)
    }

    @Test
    fun legacyAnalysisStillComputesRtp() {
        val sanitized = gson.fromJson<List<BetAnalysis>>(legacyJson, type).withLegacyOddsDefaults()
        assertNotNull(calculateRtp(sanitized.first().outcomes))
    }
}
