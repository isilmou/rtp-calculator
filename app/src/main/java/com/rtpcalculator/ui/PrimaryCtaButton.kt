package com.rtpcalculator.ui

import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rtpcalculator.ui.theme.AppShapeSmall
import com.rtpcalculator.ui.theme.CtaOnSurface
import com.rtpcalculator.ui.theme.CtaSurface

/**
 * Shared CTA button styled per the validated charte — off-white surface, near-black text,
 * compact 46-dp height, AppShapeSmall corners, 13 sp Bold with 0.5 sp letter spacing.
 * Used by both the "CREATE A BET ANALYSIS" button (history screen) and the "SAVE" button
 * (input screen bottom bar) so they stay visually identical.
 */
@Composable
fun PrimaryCtaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(46.dp),
        shape = AppShapeSmall,
        colors = ButtonDefaults.buttonColors(containerColor = CtaSurface)
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            color = CtaOnSurface,
            fontSize = 13.sp,
            letterSpacing = 0.5.sp,
            maxLines = 1
        )
    }
}
