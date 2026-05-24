@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.rtpcalculator.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rtpcalculator.R
import com.rtpcalculator.model.BetAnalysis
import com.rtpcalculator.model.OddsFormat
import com.rtpcalculator.model.Outcome
import com.rtpcalculator.model.RtpResult
import com.rtpcalculator.model.calculateRtp
import com.rtpcalculator.ui.theme.ActionAccent
import com.rtpcalculator.ui.theme.ButtonLabel
import com.rtpcalculator.ui.theme.CardBg
import com.rtpcalculator.ui.theme.CardOutlineSoft
import com.rtpcalculator.ui.theme.FieldBackground
import com.rtpcalculator.ui.theme.FieldFocus
import com.rtpcalculator.ui.theme.FormatBadgeAmerican
import com.rtpcalculator.ui.theme.FormatBadgeDecimal
import com.rtpcalculator.ui.theme.FormatBadgeFractional
import com.rtpcalculator.ui.theme.OverlayWhite30
import com.rtpcalculator.ui.theme.TextPrimary
import com.rtpcalculator.ui.theme.TextSecondary
import com.rtpcalculator.ui.theme.TextTertiary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CardBorder = BorderStroke(1.dp, CardOutlineSoft)

/** Matches the natural min height of the OutlinedTextField fields; used by the badge. */
private val OutcomeFieldHeight = 56.dp

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor   = FieldBackground,
    unfocusedContainerColor = FieldBackground,
    focusedBorderColor      = FieldFocus,
    focusedLabelColor       = FieldFocus,
    cursorColor             = FieldFocus
)

private fun Modifier.scrollIntoViewOnFocus(): Modifier = composed {
    val requester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }
    bringIntoViewRequester(requester).onFocusEvent { state ->
        if (state.isFocused) {
            job = scope.launch {
                delay(300)
                requester.bringIntoView()
            }
        } else {
            // Cancel a pending scroll so tapping another field doesn't yank the view.
            job?.cancel()
            job = null
        }
    }
}

private enum class FieldAlign { DYNAMIC, CENTER, START, END }

/**
 * The single shared editable field for the whole form (Issue, Book, Event Name, Bet Type,
 * EU value, UK numerator/denominator, US value) — one place to tune field behaviour.
 *
 * - Holds ONE stable [TextFieldValue] in `remember`; it is NEVER reconstructed on
 *   recomposition. It is mutated only for: (1) a real edit, (2) focus -> select-all,
 *   (3) blur -> caret to start (first char visible at rest), (4) a real external string
 *   change while unfocused (e.g. decimal normalization). → native single-line scroll
 *   stays bounded to the content (no infinite drag).
 * - Select-all-on-tap via [LaunchedEffect] keyed on focus, so it runs AFTER the tap caret.
 * - [align] DYNAMIC = centre when the text fits, left-align when it overflows; CENTER /
 *   START / END are fixed (the odds fields).
 * - Optional in-field [placeholder] (shown when empty/floated label).
 * - Optional native floating [label]: short title when floated (focused/filled),
 *   [labelOptional] (e.g. "… (optional)") when resting empty. Used by the top fields.
 * - [bordered] true = shared OutlinedTextField border shell; false = bare (inside
 *   [OddsFieldShell], which owns the border).
 */
@Composable
private fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    label: String? = null,
    labelOptional: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    align: FieldAlign = FieldAlign.DYNAMIC,
    bordered: Boolean = true,
    fixedHeight: Boolean = false,
    cornerRadius: Dp = 14.dp,
    focusRequester: FocusRequester? = null,
    onFocusLost: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    var tfv by remember { mutableStateOf(TextFieldValue(value)) }
    var focused by remember { mutableStateOf(false) }
    var overflow by remember { mutableStateOf(false) }

    // (4) real external string change, only while unfocused
    if (!focused && tfv.text != value) {
        tfv = tfv.copy(text = value, selection = TextRange(0))
    }
    // (2) focus -> select all ; (3) blur -> caret to start (first char visible at rest)
    LaunchedEffect(focused) {
        tfv = if (focused) tfv.copy(selection = TextRange(0, tfv.text.length))
              else tfv.copy(selection = TextRange(0))
    }

    val textAlign = when (align) {
        FieldAlign.CENTER -> TextAlign.Center
        FieldAlign.START -> TextAlign.Start
        FieldAlign.END -> TextAlign.End
        FieldAlign.DYNAMIC -> if (overflow) TextAlign.Start else TextAlign.Center
    }
    // Native label text: short title when floated (focused/filled), "… (optional)" at rest.
    val labelText = label?.let { if (focused || tfv.text.isNotBlank()) it else (labelOptional ?: it) }

    BasicTextField(
        value = tfv,
        onValueChange = {
            tfv = it                                       // (1) real edit
            if (it.text != value) onValueChange(it.text)
        },
        modifier = modifier
            .then(if (fixedHeight) Modifier.height(OutcomeFieldHeight) else Modifier)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .scrollIntoViewOnFocus()
            .onFocusChanged { st ->
                if (focused && !st.isFocused) onFocusLost()
                focused = st.isFocused
            },
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(color = TextPrimary, textAlign = textAlign),
        onTextLayout = { overflow = it.hasVisualOverflow },
        cursorBrush = SolidColor(FieldFocus),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        interactionSource = interactionSource,
        decorationBox = { inner ->
            if (bordered) {
                OutlinedTextFieldDefaults.DecorationBox(
                    value = tfv.text,
                    innerTextField = inner,
                    enabled = true,
                    singleLine = true,
                    visualTransformation = VisualTransformation.None,
                    interactionSource = interactionSource,
                    placeholder = placeholder?.let { ph ->
                        @Composable {
                            Text(
                                ph,
                                color = TextTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = textAlign,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    label = labelText?.let { lt ->
                        @Composable { Text(lt, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    },
                    contentPadding = PaddingValues(
                        horizontal = 8.dp,
                        vertical = if (label != null) 16.dp else 10.dp
                    ),
                    container = {
                        OutlinedTextFieldDefaults.Container(
                            enabled = true,
                            isError = false,
                            interactionSource = interactionSource,
                            colors = fieldColors(),
                            shape = RoundedCornerShape(cornerRadius)
                        )
                    }
                )
            } else {
                inner()
            }
        }
    )
}

private fun parseTimeOrNull(time: String): Pair<Int, Int>? {
    val parts = time.split(":")
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    return h to m
}

@Composable
fun BetAnalysisCard(
    analysis: BetAnalysis,
    onDateChange: (String) -> Unit,
    onTimeChange: (String) -> Unit,
    onEventNameChange: (String) -> Unit,
    onBetTypeChange: (String) -> Unit,
    onOutcomeLabelChange: (String, String) -> Unit,
    onOutcomeOddsChange: (String, String) -> Unit,
    onOutcomeBookmakerChange: (String, String) -> Unit,
    onOutcomeFormatChange: (String, OddsFormat) -> Unit,
    onOutcomeFracNumeratorChange: (String, String) -> Unit,
    onOutcomeFracDenominatorChange: (String, String) -> Unit,
    onOutcomeAmericanValueChange: (String, String) -> Unit,
    onOutcomeAmericanSignToggle: (String) -> Unit,
    onAddOutcome: () -> Unit,
    onRemoveOutcome: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rtpResult = remember(analysis.outcomes) {
        val allOddsFilled = analysis.outcomes.all { (it.odds.toDoubleOrNull() ?: 0.0) > 0.0 }
        if (allOddsFilled) calculateRtp(analysis.outcomes) else null
    }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = remember(analysis.id) { parseDateToMillis(analysis.date) }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val formatted = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(Date(millis))
                        onDateChange(formatted)
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.action_ok), color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel), color = TextSecondary)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    var showTimePicker by remember { mutableStateOf(false) }
    if (showTimePicker) {
        val (initialHour, initialMinute) = remember(analysis.id, analysis.time) {
            parseTimeOrNull(analysis.time) ?: (12 to 0)
        }
        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onTimeChange("%02d:%02d".format(timePickerState.hour, timePickerState.minute))
                    showTimePicker = false
                }) {
                    Text(stringResource(R.string.action_ok), color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.action_cancel), color = TextSecondary)
                }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = CardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {

            AppTextField(
                value = analysis.eventName,
                onValueChange = onEventNameChange,
                label = stringResource(R.string.event_name_title),
                labelOptional = stringResource(R.string.event_name_optional),
                cornerRadius = 16.dp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = analysis.date,
                    onValueChange = {},
                    readOnly = true,
                    label = {
                        Text(
                            if (analysis.date.isNotBlank()) stringResource(R.string.event_date_title)
                            else stringResource(R.string.event_date_optional),
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    textStyle = LocalTextStyle.current.copy(color = TextPrimary, textAlign = TextAlign.Center),
                    shape = RoundedCornerShape(16.dp),
                    colors = fieldColors(),
                    trailingIcon = {
                        if (analysis.date.isNotBlank()) {
                            IconButton(onClick = { onDateChange("") }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.cd_clear_date),
                                    tint = TextSecondary
                                )
                            }
                        } else {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = stringResource(R.string.cd_pick_date),
                                tint = TextSecondary,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                // Transparent overlay captures taps and opens the picker.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(end = if (analysis.date.isNotBlank()) 48.dp else 0.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { showDatePicker = true }
                )
            }

            if (analysis.date.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = analysis.time,
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            Text(
                                if (analysis.time.isNotBlank()) stringResource(R.string.event_time_title)
                                else stringResource(R.string.event_time_optional),
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        textStyle = LocalTextStyle.current.copy(color = TextPrimary, textAlign = TextAlign.Center),
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors(),
                        trailingIcon = {
                            if (analysis.time.isNotBlank()) {
                                IconButton(onClick = { onTimeChange("") }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = stringResource(R.string.cd_clear_time),
                                        tint = TextSecondary
                                    )
                                }
                            } else {
                                Icon(
                                    Icons.Default.AccessTime,
                                    contentDescription = stringResource(R.string.cd_pick_time),
                                    tint = TextSecondary,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(end = if (analysis.time.isNotBlank()) 48.dp else 0.dp)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { showTimePicker = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            AppTextField(
                value = analysis.betType,
                onValueChange = onBetTypeChange,
                label = stringResource(R.string.bet_type_title),
                labelOptional = stringResource(R.string.bet_type_optional),
                placeholder = stringResource(R.string.bet_type_placeholder),
                cornerRadius = 16.dp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.outcomes),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            val canRemoveOutcome = analysis.outcomes.size > 2
            analysis.outcomes.forEach { outcome ->
                key(outcome.id) {
                    OutcomeRow(
                        outcome = outcome,
                        canRemove = canRemoveOutcome,
                        onLabelChange     = { onOutcomeLabelChange(outcome.id, it) },
                        onOddsChange      = { onOutcomeOddsChange(outcome.id, it) },
                        onBookmakerChange = { onOutcomeBookmakerChange(outcome.id, it) },
                        onFormatChange    = { onOutcomeFormatChange(outcome.id, it) },
                        onFracNumeratorChange   = { onOutcomeFracNumeratorChange(outcome.id, it) },
                        onFracDenominatorChange = { onOutcomeFracDenominatorChange(outcome.id, it) },
                        onAmericanValueChange   = { onOutcomeAmericanValueChange(outcome.id, it) },
                        onAmericanSignToggle    = { onOutcomeAmericanSignToggle(outcome.id) },
                        onRemove          = { onRemoveOutcome(outcome.id) }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.Start)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onAddOutcome() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.AddCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = TextSecondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.add_outcome),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (rtpResult != null) {
                RtpDisplay(result = rtpResult)
            } else {
                Text(
                    text = stringResource(R.string.fill_odds_to_calculate_rtp),
                    fontSize = 12.sp,
                    color = TextTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, OverlayWhite30),
                colors = ButtonDefaults.buttonColors(containerColor = ActionAccent)
            ) {
                Text(
                    text = stringResource(R.string.save_to_history),
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Bold,
                    color = ButtonLabel,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
private fun OutcomeRow(
    outcome: Outcome,
    canRemove: Boolean,
    onLabelChange: (String) -> Unit,
    onOddsChange: (String) -> Unit,
    onBookmakerChange: (String) -> Unit,
    onFormatChange: (OddsFormat) -> Unit,
    onFracNumeratorChange: (String) -> Unit,
    onFracDenominatorChange: (String) -> Unit,
    onAmericanValueChange: (String) -> Unit,
    onAmericanSignToggle: () -> Unit,
    onRemove: () -> Unit
) {
    val oddsFocusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    // Bumped when a format is picked from the badge; focuses the odds field afterwards.
    var focusTick by remember { mutableStateOf(0) }
    LaunchedEffect(focusTick) {
        if (focusTick > 0) {
            delay(50) // let the dropdown dismiss and the new field attach first
            runCatching { oddsFocusRequester.requestFocus() }
            keyboard?.show()
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AppTextField(
            value = outcome.label,
            onValueChange = onLabelChange,
            placeholder = stringResource(R.string.outcome),
            modifier = Modifier.weight(1.5f)
        )
        OddsFormatBadge(
            format = outcome.oddsFormat,
            onFormatSelected = {
                onFormatChange(it)
                focusTick++
            }
        )
        OddsInput(
            outcome = outcome,
            focusRequester = oddsFocusRequester,
            onDecimalChange = onOddsChange,
            onFracNumeratorChange = onFracNumeratorChange,
            onFracDenominatorChange = onFracDenominatorChange,
            onAmericanValueChange = onAmericanValueChange,
            onAmericanSignToggle = onAmericanSignToggle,
            modifier = Modifier.weight(1.8f)
        )
        AppTextField(
            value = outcome.bookmaker,
            onValueChange = onBookmakerChange,
            placeholder = stringResource(R.string.book),
            modifier = Modifier.weight(1.2f)
        )
        IconButton(
            onClick = onRemove,
            enabled = canRemove,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = stringResource(R.string.cd_remove_outcome),
                tint = if (canRemove) MaterialTheme.colorScheme.error
                       else TextTertiary.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun OddsInput(
    outcome: Outcome,
    focusRequester: FocusRequester,
    onDecimalChange: (String) -> Unit,
    onFracNumeratorChange: (String) -> Unit,
    onFracDenominatorChange: (String) -> Unit,
    onAmericanValueChange: (String) -> Unit,
    onAmericanSignToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (outcome.oddsFormat) {
        OddsFormat.DECIMAL -> AppTextField(
            value = outcome.odds,
            onValueChange = onDecimalChange,
            placeholder = stringResource(R.string.odds),
            modifier = modifier,
            keyboardType = KeyboardType.Decimal,
            align = FieldAlign.CENTER,
            fixedHeight = true,
            focusRequester = focusRequester,
            onFocusLost = {
                if (outcome.odds.isNotBlank()) {
                    val formatted = formatOddsTwoDecimals(outcome.odds)
                    if (formatted != outcome.odds) onDecimalChange(formatted)
                }
            }
        )

        OddsFormat.FRACTIONAL -> FractionField(
            numerator = outcome.fracNumerator,
            denominator = outcome.fracDenominator,
            onNumeratorChange = { onFracNumeratorChange(it.filter(Char::isDigit)) },
            onDenominatorChange = { onFracDenominatorChange(it.filter(Char::isDigit)) },
            numeratorFocusRequester = focusRequester,
            modifier = modifier
        )

        OddsFormat.AMERICAN -> AmericanField(
            negative = outcome.americanNegative,
            value = outcome.americanValue,
            onToggleSign = onAmericanSignToggle,
            onValueChange = { onAmericanValueChange(it.filter(Char::isDigit)) },
            valueFocusRequester = focusRequester,
            modifier = modifier
        )
    }
}

/**
 * Wraps custom odds content (UK two fields, US sign+value) in the SAME bordered shell as
 * the standalone fields: identical border via [fieldColors], no label/placeholder. Child
 * focus is mirrored into the interaction source so the border highlights on focus.
 */
@Composable
private fun OddsFieldShell(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scope = rememberCoroutineScope()
    var focus by remember { mutableStateOf<FocusInteraction.Focus?>(null) }
    Box(
        modifier = modifier
            .height(OutcomeFieldHeight)
            .onFocusChanged { state ->
                if (state.hasFocus) {
                    if (focus == null) {
                        val f = FocusInteraction.Focus()
                        focus = f
                        scope.launch { interactionSource.emit(f) }
                    }
                } else {
                    focus?.let { f -> scope.launch { interactionSource.emit(FocusInteraction.Unfocus(f)) } }
                    focus = null
                }
            }
    ) {
        OutlinedTextFieldDefaults.DecorationBox(
            value = "",
            innerTextField = content,
            enabled = true,
            singleLine = true,
            visualTransformation = VisualTransformation.None,
            interactionSource = interactionSource,
            label = null,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
            container = {
                OutlinedTextFieldDefaults.Container(
                    enabled = true,
                    isError = false,
                    interactionSource = interactionSource,
                    colors = fieldColors(),
                    shape = RoundedCornerShape(14.dp)
                )
            }
        )
    }
}

/** US odds: same shell as EU; a left-pinned sign glyph + the value in the remaining space. */
@Composable
private fun AmericanField(
    negative: Boolean,
    value: String,
    onToggleSign: () -> Unit,
    onValueChange: (String) -> Unit,
    valueFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    OddsFieldShell(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.width(20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                AmericanSignToggle(negative = negative, onToggle = onToggleSign)
            }
            AppTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                keyboardType = KeyboardType.Number,
                align = FieldAlign.START,
                bordered = false,
                focusRequester = valueFocusRequester
            )
        }
    }
}

/** UK odds: same shell as EU; [numerator] "/" [denominator], the slash fixed at the centre. */
@Composable
private fun FractionField(
    numerator: String,
    denominator: String,
    onNumeratorChange: (String) -> Unit,
    onDenominatorChange: (String) -> Unit,
    numeratorFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    OddsFieldShell(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            AppTextField(
                value = numerator,
                onValueChange = onNumeratorChange,
                modifier = Modifier.weight(1f),
                keyboardType = KeyboardType.Number,
                align = FieldAlign.END,
                bordered = false,
                focusRequester = numeratorFocusRequester
            )
            Text(
                text = "/",
                color = TextSecondary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            AppTextField(
                value = denominator,
                onValueChange = onDenominatorChange,
                modifier = Modifier.weight(1f),
                keyboardType = KeyboardType.Number,
                align = FieldAlign.START,
                bordered = false
            )
        }
    }
}

@Composable
private fun AmericanSignToggle(
    negative: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = if (negative) "−" else "+",
        color = TextPrimary,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onToggle() }
            .padding(horizontal = 2.dp, vertical = 2.dp)
    )
}

private fun OddsFormat.shortLabelRes(): Int = when (this) {
    OddsFormat.DECIMAL    -> R.string.odds_format_decimal_short
    OddsFormat.FRACTIONAL -> R.string.odds_format_fractional_short
    OddsFormat.AMERICAN   -> R.string.odds_format_american_short
}

private fun OddsFormat.labelRes(): Int = when (this) {
    OddsFormat.DECIMAL    -> R.string.odds_format_decimal
    OddsFormat.FRACTIONAL -> R.string.odds_format_fractional
    OddsFormat.AMERICAN   -> R.string.odds_format_american
}

private fun OddsFormat.badgeColor(): Color = when (this) {
    OddsFormat.DECIMAL    -> FormatBadgeDecimal
    OddsFormat.FRACTIONAL -> FormatBadgeFractional
    OddsFormat.AMERICAN   -> FormatBadgeAmerican
}

@Composable
private fun OddsFormatBadge(
    format: OddsFormat,
    onFormatSelected: (OddsFormat) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .width(46.dp)
                .height(OutcomeFieldHeight)
                .clip(RoundedCornerShape(14.dp))
                .background(format.badgeColor())
                .clickable { menuOpen = true }
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(format.shortLabelRes()),
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = stringResource(R.string.cd_select_odds_format),
                tint = TextPrimary,
                modifier = Modifier.size(13.dp)
            )
        }
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false }
        ) {
            OddsFormat.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.labelRes()), color = TextPrimary) },
                    onClick = {
                        menuOpen = false
                        onFormatSelected(option)
                    },
                    trailingIcon = {
                        if (option == format) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun RtpDisplay(result: RtpResult) {
    val rtpColor = remember(result.rtp) { rtpGradientColor(result.rtp) }
    val rtpText = remember(result.rtp, result.isSurebet) {
        val pct = "%.2f".format(Locale.ENGLISH, result.rtp)
        if (result.isSurebet) "⭐ $pct%" else "$pct%"
    }
    val marginText = remember(result.margin) {
        "${"%.2f".format(Locale.ENGLISH, result.margin)}%"
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = androidx.compose.ui.graphics.Color.Black,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.rtp_return_to_player),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Normal,
                    color = TextSecondary
                )
                Text(
                    text = rtpText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = rtpColor
                )
            }
            VerticalDivider(
                modifier = Modifier.height(40.dp),
                color = OverlayWhite30
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.books_margin),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Normal,
                    color = TextSecondary
                )
                Text(
                    text = marginText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = rtpColor
                )
            }
        }
    }
}
