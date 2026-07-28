package com.rtpcalculator.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rtpcalculator.R
import com.rtpcalculator.ui.theme.AppShapeSmall
import com.rtpcalculator.ui.theme.CtaOnSurface
import java.util.Locale

/**
 * Solid TRJ pill — gradient-coloured background per the RTP value, near-black text on top.
 * Shared by the history card (event header) and the input screen, so both render identically.
 */
@Composable
fun RtpPill(rtp: Double, modifier: Modifier = Modifier) {
    Surface(
        shape = AppShapeSmall,
        color = rtpGradientColor(rtp),
        modifier = modifier
    ) {
        Text(
            text = "${stringResource(R.string.rtp_short)} ${"%.1f".format(Locale.ENGLISH, rtp)}%",
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = CtaOnSurface,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
