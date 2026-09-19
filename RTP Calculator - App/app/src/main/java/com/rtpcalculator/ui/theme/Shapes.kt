package com.rtpcalculator.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Unified corner-rounding language for the whole app.
 *
 * One light, discrete rounding everywhere (close to square, just softened) — replaces the
 * former mix of 24/20/18/16/14/12 dp radii. Two close tokens keep a subtle hierarchy:
 * large containers are a touch rounder than the small controls sitting inside them.
 *
 *  - [AppShapeLarge] (8 dp): cards (event, issues, history), dialogs.
 *  - [AppShapeSmall] (6 dp): input fields, chips/badges, CTA & SAVE buttons, format selector,
 *    sign toggle.
 *
 * Colours and intrinsically circular shapes are intentionally untouched.
 */
val AppCornerLarge = 8.dp
val AppCornerSmall = 6.dp

val AppShapeLarge = RoundedCornerShape(AppCornerLarge)
val AppShapeSmall = RoundedCornerShape(AppCornerSmall)
