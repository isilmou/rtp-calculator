package com.rtpcalculator

import com.rtpcalculator.model.BetAnalysis
import com.rtpcalculator.model.OddsFormat
import com.rtpcalculator.model.calculateRtp
import com.rtpcalculator.model.hasMeaningfulContent
import com.rtpcalculator.model.Outcome
import com.rtpcalculator.ui.rawToDecimalExactOrNull
import com.rtpcalculator.ui.decimalToAmerican
import com.rtpcalculator.ui.decimalToFraction
import com.rtpcalculator.ui.displayOdds
import com.rtpcalculator.ui.dropInvalidDecimalInput
import com.rtpcalculator.ui.formatDecimalUpTo3
import com.rtpcalculator.ui.padDecimalToMinTwo
import com.rtpcalculator.ui.sanitizeDecimalInput
import com.rtpcalculator.ui.sanitizeIntegerInput
import com.rtpcalculator.ui.stripLeadingZeros
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class OddsConversionTest {

    // Conversion formulas under test, rendered as the 2-decimal string the assertions expect.
    private fun Double?.twoDp() = this?.let { "%.2f".format(Locale.ENGLISH, it) }

    private fun decimal(raw: String) =
        rawToDecimalExactOrNull(OddsFormat.DECIMAL, raw, "", "", false, "").twoDp()

    private fun fractional(num: String, den: String) =
        rawToDecimalExactOrNull(OddsFormat.FRACTIONAL, "", num, den, false, "").twoDp()

    private fun american(negative: Boolean, value: String) =
        rawToDecimalExactOrNull(OddsFormat.AMERICAN, "", "", "", negative, value).twoDp()

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

    @Test
    fun sanitizeDecimalInput_dropsRedundantLeadingZeros() {
        assertEquals("800", sanitizeDecimalInput("0800"))
        assertEquals("800.00", sanitizeDecimalInput("0800.00"))  // legacy stored value re-saved
        assertEquals("7", sanitizeDecimalInput("007"))
        assertEquals("2.00", sanitizeDecimalInput("2.00"))        // decimals never touched
        assertEquals("2.180", sanitizeDecimalInput("2.180"))
        assertEquals("100", sanitizeDecimalInput("100"))          // inner zeros untouched
        assertEquals("", sanitizeDecimalInput(""))
    }

    @Test
    fun sanitizeDecimalInput_refusesLeadingZeroOrDot() {
        // An odds is always > 1: nothing starting with "0" or "." can ever become valid.
        assertEquals("", sanitizeDecimalInput("0"))
        assertEquals("", sanitizeDecimalInput("000"))
        assertEquals("", sanitizeDecimalInput("0."))
        assertEquals("", sanitizeDecimalInput("0.5"))
        assertEquals("", sanitizeDecimalInput("0.50"))            // pasted value dropped whole
        assertEquals("", sanitizeDecimalInput("00.5"))
        assertEquals("", sanitizeDecimalInput("."))
        assertEquals("", sanitizeDecimalInput(".5"))
        assertEquals("1", sanitizeDecimalInput("1"))               // legitimate mid-typing state
        assertEquals("1.", sanitizeDecimalInput("1."))
        assertEquals("1.5", sanitizeDecimalInput("1.5"))
    }

    @Test
    fun stripLeadingZeros_rules() {
        assertEquals("800", stripLeadingZeros("0800"))
        assertEquals("7", stripLeadingZeros("007"))
        assertEquals("0", stripLeadingZeros("0"))
        assertEquals("0", stripLeadingZeros("000"))
        assertEquals("0.5", stripLeadingZeros("0.5"))
        assertEquals("0.5", stripLeadingZeros("00.5"))
        assertEquals("", stripLeadingZeros(""))
        assertEquals("120", stripLeadingZeros("120"))
        assertEquals("100", stripLeadingZeros("100"))           // inner zeros untouched
    }

    @Test
    fun sanitizeIntegerInput_digitsOnlyStrictlyPositive() {
        assertEquals("8", sanitizeIntegerInput("08"))
        assertEquals("120", sanitizeIntegerInput("0120"))
        assertEquals("", sanitizeIntegerInput("0"))              // a zero is never a valid part
        assertEquals("", sanitizeIntegerInput("00"))
        assertEquals("15", sanitizeIntegerInput("1a5"))          // non-digits dropped
        assertEquals("", sanitizeIntegerInput(""))
        assertEquals("", sanitizeIntegerInput("-"))
    }

    @Test
    fun dropInvalidDecimalInput_clearsAnythingBelowOne() {
        assertEquals("1", dropInvalidDecimalInput("1"))        // "1" is an odds (padded to 1.00 later)
        assertEquals("1.", dropInvalidDecimalInput("1."))
        assertEquals("1.00", dropInvalidDecimalInput("1.00"))
        assertEquals("", dropInvalidDecimalInput("."))
        assertEquals("", dropInvalidDecimalInput("0.5"))         // legacy data safety
        assertEquals("", dropInvalidDecimalInput(""))
        assertEquals("1.01", dropInvalidDecimalInput("1.01"))
        assertEquals("2", dropInvalidDecimalInput("2"))
        assertEquals("2.180", dropInvalidDecimalInput("2.180"))
    }

    @Test
    fun calculateRtp_ignoresOddsBelowOne() {
        fun o(odds: String) = Outcome(odds = odds)
        assertNotNull(calculateRtp(listOf(o("2.0"), o("2.0"))))
        assertNull(calculateRtp(listOf(o("0.5"), o("2.0"))))    // impossible odds -> no TRJ
        assertNotNull(calculateRtp(listOf(o("1.0"), o("2.0")))) // 1.00 counts: a TRJ shows
        assertNull(calculateRtp(listOf(o("0"), o("2.0"))))
        assertEquals(100.0, calculateRtp(listOf(o("2.0"), o("2.0")))!!.rtp, 1e-9)
    }

    @Test
    fun hasMeaningfulContent_ignoresInvalidDecimalInput() {
        fun form(decimalRaw: String) = BetAnalysis(outcomes = listOf(Outcome(decimalRaw = decimalRaw), Outcome()))
        assertFalse(form("0").hasMeaningfulContent())
        assertTrue(form("1").hasMeaningfulContent())
        assertTrue(form("1.00").hasMeaningfulContent())
        assertFalse(form(".").hasMeaningfulContent())
        assertFalse(form("").hasMeaningfulContent())
        assertTrue(form("2").hasMeaningfulContent())
        assertTrue(form("1.01").hasMeaningfulContent())
    }
}
