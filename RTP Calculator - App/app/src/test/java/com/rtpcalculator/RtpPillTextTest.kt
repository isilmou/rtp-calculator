package com.rtpcalculator

import com.rtpcalculator.model.Outcome
import com.rtpcalculator.model.calculateRtp
import com.rtpcalculator.ui.isSurebetRtp
import com.rtpcalculator.ui.rtpPillText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Text and surebet rule of the TRJ pill (RtpPill.kt), shared by the input screen, the history
 * card and the share image. Introduced with 2.2.0 (SUREBET label).
 */
class RtpPillTextTest {

    @Test
    fun pillText_belowHundred_showsRtpOnly() {
        assertEquals("TRJ 95.3%", rtpPillText("TRJ", "SUREBET", 95.3))
        assertEquals("RTP 78.0%", rtpPillText("RTP", "SUREBET", 78.0))
    }

    @Test
    fun pillText_exactlyHundred_isNotASurebet() {
        assertEquals("TRJ 100.0%", rtpPillText("TRJ", "SUREBET", 100.0))
        assertFalse(isSurebetRtp(100.0))
    }

    @Test
    fun pillText_aboveHundred_appendsSurebetLabel() {
        assertEquals("TRJ 102.1% · SUREBET", rtpPillText("TRJ", "SUREBET", 102.1))
        assertTrue(isSurebetRtp(100.05))
    }

    @Test
    fun pillText_roundsToOneDecimal_withDotInEveryLocale() {
        assertEquals("TRJ 95.3%", rtpPillText("TRJ", "SUREBET", 95.26))
        assertEquals("RTP 105.0% · SUREBET", rtpPillText("RTP", "SUREBET", 104.99))
    }

    @Test
    fun calculateRtp_twoAtTwoTen_isSurebetAt105() {
        val result = calculateRtp(listOf(Outcome(odds = "2.10"), Outcome(odds = "2.10")))
        assertNotNull(result)
        assertEquals(105.0, result!!.rtp, 0.01)
        assertTrue(result.isSurebet)
        assertEquals("TRJ 105.0% · SUREBET", rtpPillText("TRJ", "SUREBET", result.rtp))
    }
}
