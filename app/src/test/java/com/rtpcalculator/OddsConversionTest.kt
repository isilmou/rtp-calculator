package com.rtpcalculator

import com.rtpcalculator.model.OddsFormat
import com.rtpcalculator.model.Outcome
import com.rtpcalculator.ui.convertToDecimalOdds
import com.rtpcalculator.ui.displayOdds
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
}
