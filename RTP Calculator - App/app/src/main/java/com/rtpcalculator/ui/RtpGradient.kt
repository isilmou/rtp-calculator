package com.rtpcalculator.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.rtpcalculator.ui.theme.RtpGreen
import com.rtpcalculator.ui.theme.RtpOrange
import com.rtpcalculator.ui.theme.RtpRed
import com.rtpcalculator.ui.theme.RtpYellow

fun rtpGradientColor(rtp: Double): Color = when {
    rtp < 78.0 -> RtpRed
    rtp < 87.0 -> lerp(RtpRed,    RtpOrange, ((rtp - 78.0) / 9.0).toFloat())
    rtp < 92.0 -> lerp(RtpOrange, RtpYellow, ((rtp - 87.0) / 5.0).toFloat())
    rtp < 95.0 -> lerp(RtpYellow, RtpGreen,  ((rtp - 92.0) / 3.0).toFloat())
    else       -> RtpGreen
}
