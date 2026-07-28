package com.rtpcalculator

import com.rtpcalculator.model.OddsFormat
import com.rtpcalculator.model.Outcome
import com.rtpcalculator.ui.convertToDecimalOdds
import com.rtpcalculator.ui.decimalToAmerican
import com.rtpcalculator.ui.decimalToFraction
import com.rtpcalculator.ui.displayOdds
import com.rtpcalculator.ui.formatDecimalUpTo3
import com.rtpcalculator.ui.padDecimalToMinTwo
import com.rtpcalculator.ui.sanitizeDecimalInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OddsConversionTest {

    private fun decimal(raw: String) =
        convertToDecimalOdds(OddsFormat.DECIMAL, raw, "", "", false, "")

    private fun fractional(num: String, den: String) =
        convertToDecimalOdds(OddsFormat.FRACTIONAL, "", num, den, false, "")

    private fun american(negative: Boolean, value: String) =
        convertToDecimalOdds(OddsFormat.AMERICAN, "", "", "", negative, value)

    @Test
    fun decimal_normalisesCommaAndDecimals() {
        assertEquals("2.50", decimal("2,5"))
        assertEquals("3.50", decimal("3.5"))
    }

    @Test
    fun decimal_invalidReturnsNull() {
        assertNull(decimal(""))
        assertNull(decimal("abc"))
    }

    @Test
    fun fractional_fiveOverTwoIsThreeFifty() {
        assertEquals("3.50", fractional("5", "2"))
    }

    @Test
    fun fractional_zeroOrMissingDenominatorIsInvalid() {
        assertNull(fractional("5", "0"))
        assertNull(fractional("5", ""))
        assertNull(fractional("", "2"))
    }

    @Test
    fun american_positiveOneFiftyIsTwoFifty() {
        assertEquals("2.50", american(negative = false, value = "150"))
    }

    @Test
    fun american_negativeOneTwentyIsOneEightyThree() {
        assertEquals("1.83", american(negative = true, value = "120"))
    }

    @Test
    fun american_zeroOrMissingValueIsInvalid() {
        assertNull(american(negative = false, value = "0"))
        assertNull(american(negative = false, value = ""))
    }

    // --- displayOdds (history card) ---

    @Test
    fun display_decimalShowsDecimal() {
        assertEquals("2.50", Outcome(odds = "2.50", oddsFormat = OddsFormat.DECIMAL).displayOdds())
    }

    @Test
    fun display_fractionalShowsNumOverDen() {
        val o = Outcome(
            odds = "3.50", oddsFormat = OddsFormat.FRACTIONAL,
            fracNumerator = "5", fracDenominator = "2"
        )
        assertEquals("5/2", o.displayOdds())
    }

    @Test
    fun display_fractionalMissingPartFallsBackToDecimal() {
        val o = Outcome(
            odds = "3.50", oddsFormat = OddsFormat.FRACTIONAL,
            fracNumerator = "5", fracDenominator = ""
        )
        assertEquals("3.50", o.displayOdds())
    }

    @Test
    fun display_americanShowsSignedValue() {
        val pos = Outcome(odds = "2.50", oddsFormat = OddsFormat.AMERICAN, americanValue = "150")
        val neg = Outcome(odds = "1.83", oddsFormat = OddsFormat.AMERICAN, americanNegative = true, americanValue = "120")
        assertEquals("+150", pos.displayOdds())
        assertEquals("-120", neg.displayOdds())
    }

    @Test
    fun display_americanEmptyValueShowsDash() {
        assertEquals("—", Outcome(odds = "", oddsFormat = OddsFormat.AMERICAN, americanValue = "").displayOdds())
    }

    @Test
    fun display_legacyDefaultOutcomeShowsDecimal() {
        // Old analyses sanitize oddsFormat to DECIMAL with empty raw fields.
        assertEquals("2.50", Outcome(odds = "2.50").displayOdds())
    }

    @Test
    fun display_decimalKeepsSignificantThirdDecimal() {
        // History card must show the 3rd significant decimal exactly as entered (bug fix).
        assertEquals(
            "2.125",
            Outcome(odds = "2.125", decimalRaw = "2.125", oddsFormat = OddsFormat.DECIMAL).displayOdds()
        )
        assertEquals(
            "11.205",
            Outcome(odds = "11.205", decimalRaw = "11.205", oddsFormat = OddsFormat.DECIMAL).displayOdds()
        )
    }

    @Test
    fun display_decimalFromOddsFormatsUpToThree() {
        // No raw string: format the calc odds up to 3 decimals (still no truncation to 2).
        assertEquals("2.125", Outcome(odds = "2.125", oddsFormat = OddsFormat.DECIMAL).displayOdds())
    }

    // --- Auto-conversion: decimal -> American integer ---

    @Test
    fun decimalToAmerican_examples() {
        assertEquals(false to "118", decimalToAmerican(2.18))
        assertEquals(true to "120", decimalToAmerican(1.83))
        assertEquals(false to "150", decimalToAmerican(2.50))
        assertEquals(true to "200", decimalToAmerican(1.50))
    }

    @Test
    fun decimalToAmerican_atOrBelowOneIsNull() {
        assertNull(decimalToAmerican(1.0))
        assertNull(decimalToAmerican(0.5))
    }

    // --- Auto-conversion: decimal -> reduced fraction ---

    @Test
    fun decimalToFraction_examples() {
        assertEquals("59" to "50", decimalToFraction(2.18))
        assertEquals("5" to "2", decimalToFraction(3.50))
        assertEquals("333" to "100", decimalToFraction(4.33))
    }

    @Test
    fun decimalToFraction_atOrBelowOneIsNull() {
        assertNull(decimalToFraction(1.0))
    }

    // --- Reference-based round trip: conversions start from the reference, not a converted value ---

    @Test
    fun reference_2_18_toUkThenUs_comesFromReferenceNotFraction() {
        // 2.18 -> UK = 59/50 ; converting to US from the SAME reference 2.18 gives +118.
        assertEquals("59" to "50", decimalToFraction(2.18))
        assertEquals(false to "118", decimalToAmerican(2.18))
    }

    // --- EU display rule (conversion results) ---

    @Test
    fun formatDecimalUpTo3_rules() {
        assertEquals("2.18", formatDecimalUpTo3(2.18))      // trailing-zero 3rd decimal dropped
        assertEquals("2.18", formatDecimalUpTo3(2.180))     // same value
        assertEquals("2.125", formatDecimalUpTo3(2.125))    // exact 3-decimal kept
        assertEquals("1.83", formatDecimalUpTo3(100.0 / 120.0 + 1.0)) // 1.8333… -> 2 decimals
    }

    @Test
    fun padDecimalToMinTwo_padsAndPreservesTypedTrailingZeros() {
        assertEquals("2.00", padDecimalToMinTwo("2"))
        assertEquals("2.00", padDecimalToMinTwo("2."))
        assertEquals("1.50", padDecimalToMinTwo("1.5"))
        assertEquals("2.18", padDecimalToMinTwo("2.18"))
        assertEquals("2.125", padDecimalToMinTwo("2.125"))
        assertEquals("2.180", padDecimalToMinTwo("2.180"))
        assertEquals("2.100", padDecimalToMinTwo("2.100"))
        assertEquals("", padDecimalToMinTwo(""))
    }

    @Test
    fun sanitizeDecimalInput_commaAndMaxThreeDecimals() {
        assertEquals("2.5", sanitizeDecimalInput("2,5"))
        assertEquals("2.180", sanitizeDecimalInput("2.180"))   // typed trailing zero kept
        assertEquals("2.125", sanitizeDecimalInput("2.1254"))  // 4th decimal blocked
        assertEquals("2.18", sanitizeDecimalInput("2.1.8"))    // extra dot ignored
    }
}
