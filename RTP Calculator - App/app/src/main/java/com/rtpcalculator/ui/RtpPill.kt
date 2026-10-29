package com.rtpcalculator.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rtpcalculator.R
import com.rtpcalculator.ui.theme.AppShapeSmall
import com.rtpcalculator.ui.theme.CtaOnSurface
import com.rtpcalculator.ui.theme.OddsPlaceholder
import java.util.Locale

/**
 * Text of the TRJ pill: "TRJ 95.3%", or "TRJ 102.1% · SUREBET" when the RTP exceeds 100 % (every
 * outcome can be covered for a guaranteed profit). The profit itself is not shown: it follows from
 * the RTP. Pure function so it is unit-testable; one decimal, dot separator in every locale.
 */
fun rtpPillText(rtpShort: String, surebetLabel: String, rtp: Double): String {
    val base = "$rtpShort ${"%.1f".format(Locale.ENGLISH, rtp)}%"
    return if (isSurebetRtp(rtp)) "$base · $surebetLabel" else base
}

/** A surebet is an RTP strictly above 100 % (same rule as calculateRtp's isSurebet). */
fun isSurebetRtp(rtp: Double): Boolean = rtp > 100.0

/**
 * Accent colour of an analysis: the TRJ gradient, or sky blue ([OddsPlaceholder]) for a surebet.
 * Used by the pill AND the vertical accent bar of the history card / share image, so they always match.
 */
fun rtpAccentColor(rtp: Double): Color = if (isSurebetRtp(rtp)) OddsPlaceholder else rtpGradientColor(rtp)

/**
 * Solid TRJ pill — [rtpAccentColor] background (gradient, or sky blue with the SUREBET label when the
 * RTP exceeds 100 %), near-black text on top.
 * Shared by the history card (event header, also used by the share image) and the input screen,
 * so all three render identically.
 */
@Composable
fun RtpPill(rtp: Double, modifier: Modifier = Modifier) {
    Surface(
        shape = AppShapeSmall,
        color = rtpAccentColor(rtp),
        modifier = modifier
    ) {
        Text(
            text = rtpPillText(stringResource(R.string.rtp_short), stringResource(R.string.surebet), rtp),
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = CtaOnSurface,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
