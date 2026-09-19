package com.rtpcalculator.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Aligned with the new charte palette (see Color.kt). primary = off-white CTA
// surface (with near-black text on it), background/surface use the two depth
// levels, accents (DangerRed) drive the error slot. Anything that needs an
// accent colour reads from the explicit tokens in Color.kt — colorScheme.primary
// stays neutral so default Material components don't pollute the look.
private val AppColorScheme = darkColorScheme(
    primary          = CtaSurface,
    onPrimary        = CtaOnSurface,
    background       = AppBackground,
    onBackground     = TextPrimary,
    surface          = CardBg,
    onSurface        = TextPrimary,
    surfaceVariant   = CardBg,
    onSurfaceVariant = TextSecondary,
    outline          = DividerSubtle,
    error            = DangerRed,
    onError          = TextPrimary
)

@Composable
fun RTPCalculatorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography  = Typography,
        content     = content
    )
}
