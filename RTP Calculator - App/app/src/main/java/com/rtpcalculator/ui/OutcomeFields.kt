package com.rtpcalculator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rtpcalculator.R
import com.rtpcalculator.model.OddsFormat
import com.rtpcalculator.ui.theme.AppShapeSmall
import com.rtpcalculator.ui.theme.FieldBackground
import com.rtpcalculator.ui.theme.FieldFocus
import com.rtpcalculator.ui.theme.FormatActiveSurface
import com.rtpcalculator.ui.theme.FormatActiveText
import com.rtpcalculator.ui.theme.FormatInactiveSurface
import com.rtpcalculator.ui.theme.FormatInactiveText
import com.rtpcalculator.ui.theme.OddsYellow
import com.rtpcalculator.ui.theme.TextPrimary
import com.rtpcalculator.ui.theme.TextSecondary

// One segment of the EU/UK/US format selector (three side-by-side tabs).
private val OddsFormatSegmentWidth = 36.dp

/**
 * EU / UK / US format selector: three side-by-side segments (segmented control), always visible.
 * The active segment is filled with its format colour ([OddsFormat.badgeColor]) and white text;
 * inactive segments are neutral. Tapping a segment calls [onSelected] (wired to updateOutcomeFormat,
 * which clears the odds as agreed for now). Height is driven by the parent row (fillMaxHeight) so it
 * lines up with the Cote field beside it.
 */
@Composable
internal fun OddsFormatSelector(
    selected: OddsFormat,
    onSelected: (OddsFormat) -> Unit,
    modifier: Modifier = Modifier
) {
    // Three pill-segments side by side, no inner divider, rounded outer corners via clip.
    // CRITICAL: the active text uses FormatActiveText (#0A0A0A black) on FormatActiveSurface
    // (#E8E8E8 off-white) — explicit colors (no theme inheritance) to avoid the
    // white-on-white bug seen previously.
    Row(
        modifier = modifier.clip(AppShapeSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OddsFormat.entries.forEach { format ->
            val active = format == selected
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(OddsFormatSegmentWidth)
                    .background(if (active) FormatActiveSurface else FormatInactiveSurface)
                    .clickable { onSelected(format) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(format.shortLabelRes()),
                    color = if (active) FormatActiveText else FormatInactiveText,
                    fontSize = 10.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

private fun OddsFormat.shortLabelRes(): Int = when (this) {
    OddsFormat.DECIMAL    -> R.string.odds_format_decimal_short
    OddsFormat.FRACTIONAL -> R.string.odds_format_fractional_short
    OddsFormat.AMERICAN   -> R.string.odds_format_american_short
}

/**
 * Compact single-line text field with PLACEHOLDER inside (no floating label) — replaces the
 * old DecorationBox-based design to save vertical space.
 *  - Box wrapper handles the dark background ([FieldBackground] = AppBackground) + rounded
 *    corners ([AppShapeSmall]) + content alignment.
 *  - When the value is empty, [label] is rendered as a placeholder inside the box.
 *  - The text style (size, weight, colour) is parameterised so the cote field can render the
 *    odds in big OddsYellow ExtraBold and the others stay in normal TextPrimary.
 *  - Height is driven by the caller's [modifier] (typically a Row.height parent) — no built-in
 *    minimum so the field fits a 30-32 dp line without overshoot.
 */
@Composable
internal fun CompactField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String?,  // placeholder shown when the value is empty; pass null for no placeholder (e.g. cote EU field — the selector beside it is enough context).
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    textAlign: TextAlign = TextAlign.Start,
    textColor: Color = TextPrimary,
    textFontSize: TextUnit = 12.sp,
    textFontWeight: FontWeight = FontWeight.Normal,
    focusRequester: FocusRequester? = null,
    // Pass false when an ANCESTOR already handles the scroll-into-view (e.g. OutcomeRow's
    // outer Column applies it to keep the WHOLE issue visible). Default true for standalone
    // usage like the event-card eventName / betType fields.
    scrollIntoViewOnFocus: Boolean = true,
    valueTransform: (String) -> String = { it },
    onFocusLost: () -> Unit = {}
) {
    val tfv = remember { mutableStateOf(TextFieldValue(value)) }
    LaunchedEffect(value) {
        if (tfv.value.text != value) tfv.value = TextFieldValue(value, TextRange(value.length))
    }
    Box(
        modifier = modifier
            .clip(AppShapeSmall)
            .background(FieldBackground)
            .padding(horizontal = OutcomeFieldContentInset),
        contentAlignment = if (textAlign == TextAlign.Center) Alignment.Center else Alignment.CenterStart
    ) {
        if (tfv.value.text.isEmpty() && label != null) {
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth()
            )
        }
        BasicTextField(
            value = tfv.value,
            onValueChange = { new ->
                val transformedText = valueTransform(new.text)
                val transformed = if (transformedText == new.text) new else new.copy(text = transformedText)
                tfv.value = transformed
                if (transformed.text != value) onValueChange(transformed.text)
            },
            modifier = (if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .fillMaxWidth()
                .let { if (scrollIntoViewOnFocus) it.scrollIntoViewOnFocus() else it }
                .selectAllOnFirstFocus(tfv, onFocusLost),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(
                color = textColor,
                fontSize = textFontSize,
                fontWeight = textFontWeight,
                textAlign = textAlign
            ),
            cursorBrush = SolidColor(FieldFocus),
            keyboardOptions = keyboardOptions
        )
    }
}

/**
 * UK (fractional) odds input — compact box ([FieldBackground] bg, [AppShapeSmall] corners)
 * holding [numerator] "/" [denominator]. Both sub-fields share one interactionSource so the
 * outer FractionalField's hasFocus picks up either tap (used by [scrollIntoViewOnFocus]).
 * No label/placeholder: the format selector to the left makes it obvious it's the odds field.
 */
@Composable
internal fun FractionalField(
    numerator: String,
    denominator: String,
    onNumeratorChange: (String) -> Unit,
    onDenominatorChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)

    val numTfv = remember { mutableStateOf(TextFieldValue(numerator)) }
    LaunchedEffect(numerator) {
        if (numTfv.value.text != numerator) numTfv.value = TextFieldValue(numerator, TextRange(numerator.length))
    }
    val denTfv = remember { mutableStateOf(TextFieldValue(denominator)) }
    LaunchedEffect(denominator) {
        if (denTfv.value.text != denominator) denTfv.value = TextFieldValue(denominator, TextRange(denominator.length))
    }

    val oddsTextStyle = LocalTextStyle.current.copy(
        color = OddsYellow,
        fontSize = 14.sp,
        fontWeight = FontWeight.ExtraBold
    )

    // Outer Box: bg + clip. The OutcomeRow's outer Column owns scrollIntoViewOnFocus, so
    // focusing the numerator or the inner denominator triggers ONE coordinated scroll using
    // the whole OutcomeRow's bounds (and not the tiny inner BasicTextField's measured size).
    Box(
        modifier = modifier
            .clip(AppShapeSmall)
            .background(FieldBackground),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = OutcomeFieldContentInset),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = numTfv.value,
                onValueChange = { new ->
                    val digits = sanitizeIntegerInput(new.text)
                    numTfv.value = if (digits == new.text) new else new.copy(text = digits, selection = TextRange(digits.length))
                    if (digits != numerator) onNumeratorChange(digits)
                },
                modifier = Modifier
                    .weight(1f)
                    .selectAllOnFirstFocus(numTfv),
                singleLine = true,
                textStyle = oddsTextStyle.copy(textAlign = TextAlign.End),
                cursorBrush = SolidColor(FieldFocus),
                keyboardOptions = keyboardOptions
            )
            Text(
                text = "/",
                color = OddsYellow,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            BasicTextField(
                value = denTfv.value,
                onValueChange = { new ->
                    val digits = sanitizeIntegerInput(new.text)
                    denTfv.value = if (digits == new.text) new else new.copy(text = digits, selection = TextRange(digits.length))
                    if (digits != denominator) onDenominatorChange(digits)
                },
                modifier = Modifier
                    .weight(1f)
                    .selectAllOnFirstFocus(denTfv),
                singleLine = true,
                textStyle = oddsTextStyle.copy(textAlign = TextAlign.Start),
                cursorBrush = SolidColor(FieldFocus),
                keyboardOptions = keyboardOptions
            )
        }
    }
}

/**
 * US (American) odds input — compact box ([FieldBackground] bg, [AppShapeSmall] corners) with a
 * tappable "+"/"-" sign glyph on the left and the digit-only [value] field centred to the right.
 * The sign is toggled by tap (never typed). Yellow ExtraBold odds text, no label/placeholder.
 */
@Composable
internal fun AmericanField(
    value: String,
    negative: Boolean,
    onValueChange: (String) -> Unit,
    onToggleSign: () -> Unit,
    label: String? = null,  // placeholder shown to the right of the sign when value is empty.
    modifier: Modifier = Modifier
) {
    val keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)

    val tfv = remember { mutableStateOf(TextFieldValue(value)) }
    LaunchedEffect(value) {
        if (tfv.value.text != value) tfv.value = TextFieldValue(value, TextRange(value.length))
    }

    Box(
        modifier = modifier
            .clip(AppShapeSmall)
            .background(FieldBackground),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = OutcomeFieldContentInset),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (negative) "-" else "+",
                color = OddsYellow,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                modifier = Modifier
                    .clip(AppShapeSmall)
                    .clickable { onToggleSign() }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
            // Value centred in the remaining width to the right of the sign. Placeholder is
            // overlaid below the BasicTextField — it only shows while the value is empty.
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                if (tfv.value.text.isEmpty() && label != null) {
                    Text(
                        text = label,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
                BasicTextField(
                    value = tfv.value,
                    onValueChange = { new ->
                        val digits = sanitizeIntegerInput(new.text)
                        tfv.value = if (digits == new.text) new else new.copy(text = digits, selection = TextRange(digits.length))
                        if (digits != value) onValueChange(digits)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectAllOnFirstFocus(tfv),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        color = OddsYellow,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    ),
                    cursorBrush = SolidColor(FieldFocus),
                    keyboardOptions = keyboardOptions
                )
            }
        }
    }
}
