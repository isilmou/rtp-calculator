@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.rtpcalculator.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rtpcalculator.R
import com.rtpcalculator.model.BetAnalysis
import com.rtpcalculator.model.OddsFormat
import com.rtpcalculator.model.Outcome
import com.rtpcalculator.model.RtpResult
import com.rtpcalculator.model.calculateRtp
import com.rtpcalculator.ui.theme.AppShapeLarge
import com.rtpcalculator.ui.theme.AppShapeSmall
import com.rtpcalculator.ui.theme.CardBg
import com.rtpcalculator.ui.theme.DangerRed
import com.rtpcalculator.ui.theme.DarkSurface
import com.rtpcalculator.ui.theme.DividerSubtle
import com.rtpcalculator.ui.theme.FieldBackground
import com.rtpcalculator.ui.theme.FieldFocus
import com.rtpcalculator.ui.theme.OddsYellow
import com.rtpcalculator.ui.theme.OverlayWhite30
import com.rtpcalculator.ui.theme.TextPrimary
import com.rtpcalculator.ui.theme.TextSecondary
import com.rtpcalculator.ui.theme.TextTertiary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Compact field/row sizing — refactored to a denser layout per the new charte. Line 1 of an
// OutcomeRow (Issue + Book) is 36dp tall; line 2 (selector + cote + trash) is tighter at 32dp
// to compress the saisie screen onto a single page on a typical phone.
private val OutcomeFieldHeight   = 36.dp
private val OutcomeLineTwoHeight = 32.dp
private val OutcomeRemoveSize    = 32.dp
private val OutcomeRowSpacing    = 5.dp
// Left inset of the field content (CompactField / FractionalField contentPadding).
// internal: also read by the field composables extracted to OutcomeFields.kt.
internal val OutcomeFieldContentInset = 8.dp

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor   = FieldBackground,
    unfocusedContainerColor = FieldBackground,
    focusedBorderColor      = FieldFocus,
    focusedLabelColor       = FieldFocus,
    cursorColor             = FieldFocus
)

/**
 * CompositionLocals plumbed by the screen hosting the form (MainActivity → its LazyColumn).
 * They give [scrollIntoViewOnFocus] direct access to the parent scroller and its live bounds
 * so it can ACTIVELY reposition a focused field at a fixed offset above the IME — even when
 * the field is already visible (BringIntoViewRequester cannot do that; it only scrolls the
 * minimum needed to make a rect visible).
 *
 *  - [LocalFormScrollState]: the LazyListState owning the form's scroll.
 *  - [LocalFormViewportCoords]: live LayoutCoordinates of the LazyColumn (its position in
 *    window + its current height, which shrinks under imePadding() when the keyboard is up).
 */
internal val LocalFormScrollState = compositionLocalOf<LazyListState?> { null }
internal val LocalFormViewportCoords = compositionLocalOf<State<LayoutCoordinates?>?> { null }

/**
 * On first focus, ACTIVELY scrolls the form so the field always lands at the same target:
 * its bottom edge sits **1.2 × field-height above the visible viewport bottom** (i.e. above
 * the IME when it is up). So a field too high on screen comes DOWN to the target (content
 * scrolls down), and a field too low (hidden behind the IME) comes UP to the target (content
 * scrolls up). The 1.2× factor multiplies the field's MEASURED height — no hard-coded dp,
 * so the reserved band follows any future change to row heights automatically.
 *
 * Implementation: drives the scroll directly via [LazyListState.animateScrollBy] using the
 * difference between the field's current bottom and its target bottom in window-absolute
 * coordinates ([LayoutCoordinates.positionInWindow]). The 300 ms delay lets the IME finish
 * its appear animation (windowSoftInputMode = adjustResize → the LazyColumn shrinks under
 * .imePadding()) before we read the post-resize viewport height.
 *
 * Unlike BringIntoViewRequester (passive: no-op when the rect is already visible), this also
 * pulls already-visible-but-too-high fields down to the target position.
 *
 * internal: also applied by the field composables extracted to OutcomeFields.kt.
 */
internal fun Modifier.scrollIntoViewOnFocus(): Modifier = composed {
    val listState = LocalFormScrollState.current
    val viewportCoordsState = LocalFormViewportCoords.current
    val scope = rememberCoroutineScope()
    var fieldCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    onGloballyPositioned { coords -> fieldCoords = coords }
        .onFocusEvent { state ->
            // `hasFocus` (not `isFocused`) so the helper also fires when a DESCENDANT focusable
            // gains focus — e.g. the UK denominator BasicTextField nested inside the FractionalField
            // host's DecorationBox. Using the OUTER composable's measured size keeps the 1.2 × h
            // target visually consistent regardless of which sub-zone the user tapped.
            if (state.hasFocus && listState != null && viewportCoordsState != null) {
                scope.launch {
                    delay(300)
                    val field = fieldCoords ?: return@launch
                    val viewport = viewportCoordsState.value ?: return@launch
                    val h = field.size.height.toFloat()
                    val fieldTopInWindow = field.positionInWindow().y
                    val viewportTopInWindow = viewport.positionInWindow().y
                    val viewportBottomInWindow = viewportTopInWindow + viewport.size.height.toFloat()
                    val targetFieldBottomInWindow = viewportBottomInWindow - 1.2f * h
                    // delta > 0: field too low  -> scroll content UP   (LazyList scrollBy +).
                    // delta < 0: field too high -> scroll content DOWN (LazyList scrollBy -).
                    val delta = (fieldTopInWindow + h) - targetFieldBottomInWindow
                    if (kotlin.math.abs(delta) > 0.5f) {
                        listState.animateScrollBy(delta)
                    }
                }
            }
        }
}

/**
 * On the FIRST focus gained, sets the selection of [tfv] to the whole text so a single tap
 * lets the user overwrite the existing value. Subsequent taps inside the same focused field
 * place the cursor normally (the select-all branch only fires on focus=false -> focus=true).
 * Also runs [onFocusLost] on focus=true -> focus=false.
 *
 * The selection update goes through a [LaunchedEffect] (not directly inside onFocusEvent) so
 * it lands AFTER the IME has done its own initial selection sync on focus gain — otherwise
 * the IME can stomp on our select-all and the cursor ends up at the end of the text instead.
 * This was the cause of the "no select-all after a format change" bug.
 * (See ~/memory/odds-input-scroll-selectall-gotcha.md.)
 *
 * internal: also applied by the field composables extracted to OutcomeFields.kt.
 */
internal fun Modifier.selectAllOnFirstFocus(
    tfv: MutableState<TextFieldValue>,
    onFocusLost: () -> Unit = {}
): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    var wasFocused by remember { mutableStateOf(false) }
    LaunchedEffect(focused) {
        if (focused && !wasFocused) {
            wasFocused = true
            tfv.value = tfv.value.copy(selection = TextRange(0, tfv.value.text.length))
        }
    }
    onFocusEvent { state ->
        if (state.isFocused) {
            focused = true
        } else {
            focused = false
            // Run onFocusLost synchronously (not via the LaunchedEffect) so it still fires
            // if the composable is being disposed right after losing focus — e.g. a format
            // change replacing the cote field would otherwise cancel the LaunchedEffect
            // before its body could pad the EU decimal to 2 decimals.
            if (wasFocused) {
                wasFocused = false
                onFocusLost()
            }
        }
    }
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
    onOutcomeFracNumeratorChange: (String, String) -> Unit,
    onOutcomeFracDenominatorChange: (String, String) -> Unit,
    onOutcomeAmericanValueChange: (String, String) -> Unit,
    onOutcomeAmericanSignToggle: (String) -> Unit,
    onOutcomeBookmakerChange: (String, String) -> Unit,
    onOutcomeFormatChange: (String, OddsFormat) -> Unit,
    onAddOutcome: () -> Unit,
    onRemoveOutcome: (String) -> Unit,
    focusOutcomeId: String?,
    onFocusOutcomeConsumed: () -> Unit,
    modifier: Modifier = Modifier
) {
    // ---- Material 3 date picker (calendar). Display is localised by the picker; the value we
    // store stays canonical dd/MM/yyyy so parseDateToMillis / history sorting are unaffected. ----
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = remember(analysis.id) { parseDateToMillis(analysis.date) }
    )
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            colors = appDatePickerColors(),
            shape = AppShapeLarge,
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateChange(SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(Date(millis)))
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.action_ok), color = TextPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel), color = TextSecondary)
                }
            }
        ) {
            DatePicker(state = datePickerState, colors = appDatePickerColors())
        }
    }

    // ---- Material 3 time picker (clock), 24h. Stored as HH:mm (parseTimeOrNull-compatible). ----
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
            containerColor = DarkSurface,
            shape = AppShapeLarge,
            confirmButton = {
                TextButton(onClick = {
                    onTimeChange("%02d:%02d".format(timePickerState.hour, timePickerState.minute))
                    showTimePicker = false
                }) {
                    Text(stringResource(R.string.action_ok), color = TextPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.action_cancel), color = TextSecondary)
                }
            },
            text = { TimePicker(state = timePickerState, colors = appTimePickerColors()) }
        )
    }

    // Live TRJ: recomputed on every odds change so the pill at the bottom of the form updates
    // in real time. Null when at least one outcome's `odds` is empty/invalid (incomplete entry).
    val rtpResult: RtpResult? = remember(analysis.outcomes) { calculateRtp(analysis.outcomes) }

    // The pill must only surface once EVERY issue has a valid (> 0) decimal odds. calculateRtp
    // returns a value as soon as 2 odds are valid (enough to preview a 2-way), so on a 3+ way
    // bet it would otherwise flash a misleading TRJ computed from a partial set mid-entry.
    val allOddsFilled = remember(analysis.outcomes) {
        analysis.outcomes.all { (it.odds.toDoubleOrNull() ?: 0.0) > 0.0 }
    }

    // No outer card: the form sits on the screen background; cards (event + each issue) sit
    // over it and fields sit over the cards.
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {

        // ---- Event card: name + date/time + bet type, grouped as the "bet context" block. ----
        SectionCard {
            CompactField(
                value = analysis.eventName,
                onValueChange = onEventNameChange,
                label = stringResource(R.string.event_name_optional),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(OutcomeFieldHeight)
            )

            Spacer(modifier = Modifier.height(5.dp))

            // Date + Time side by side. Time stays disabled (greyed) until a date is set.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(OutcomeFieldHeight),
                horizontalArrangement = Arrangement.spacedBy(OutcomeRowSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DateClickField(
                    value = analysis.date,
                    onClear = { onDateChange("") },
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1.2f).fillMaxHeight()
                )
                TimeClickField(
                    value = analysis.time,
                    enabled = analysis.date.isNotBlank(),
                    onClear = { onTimeChange("") },
                    onClick = { showTimePicker = true },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }

            Spacer(modifier = Modifier.height(5.dp))

            CompactField(
                value = analysis.betType,
                onValueChange = onBetTypeChange,
                // The placeholder string now embeds both the label "Type de pari (facultatif)"
                // AND the examples ("1X2, …"), so the user keeps the field's purpose in mind
                // even after the previous bare-examples placeholder was shown alone.
                label = stringResource(R.string.bet_type_placeholder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(OutcomeFieldHeight)
            )
        }

        // ---- One card per issue. ----
        val canRemoveOutcome = analysis.outcomes.size > 2
        analysis.outcomes.forEach { outcome ->
            key(outcome.id) {
                SectionCard {
                    OutcomeRow(
                        outcome = outcome,
                        canRemove = canRemoveOutcome,
                        autoFocusIssue = focusOutcomeId == outcome.id,
                        onAutoFocusConsumed = onFocusOutcomeConsumed,
                        onLabelChange     = { onOutcomeLabelChange(outcome.id, it) },
                        onOddsChange      = { onOutcomeOddsChange(outcome.id, it) },
                        onFracNumeratorChange   = { onOutcomeFracNumeratorChange(outcome.id, it) },
                        onFracDenominatorChange = { onOutcomeFracDenominatorChange(outcome.id, it) },
                        onAmericanValueChange   = { onOutcomeAmericanValueChange(outcome.id, it) },
                        onAmericanSignToggle    = { onOutcomeAmericanSignToggle(outcome.id) },
                        onBookmakerChange = { onOutcomeBookmakerChange(outcome.id, it) },
                        onFormatChange    = { onOutcomeFormatChange(outcome.id, it) },
                        onRemove          = { onRemoveOutcome(outcome.id) }
                    )
                }
            }
        }

        // ---- Bottom row: LIVE TRJ pill + "marge X.XX%" on the LEFT, "Add outcome" on the RIGHT.
        // The pill only appears when calculateRtp(outcomes) is non-null (every odds filled).
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: real-time TRJ pill ONLY (margin removed per design refresh — keeping the
            // input screen as quiet as possible while the user fills the odds). Shared RtpPill so
            // it stays pixel-identical to the history card's pill.
            if (allOddsFilled && rtpResult != null) {
                RtpPill(rtp = rtpResult.rtp)
            } else {
                // Until every odds is filled, show NOTHING on the left — just keep the "Add
                // outcome" button anchored to the right (no misleading partial TRJ, no help text).
                Spacer(modifier = Modifier.width(1.dp))
            }

            // Right: "Add outcome" pill — label, then "+" icon.
            Row(
                modifier = Modifier
                    .clip(AppShapeSmall)
                    .background(CardBg)
                    .clickable { onAddOutcome() }
                    .padding(horizontal = 9.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.add_outcome),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Outlined.AddCircle,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = TextSecondary
                )
            }
        }

        // SAVE button moved out of the form content into the Scaffold's bottomBar
        // (MainActivity.RtpApp) — it's now sticky at the bottom of the screen and lifts above
        // the IME via .imePadding().
    }
}

/**
 * A section surface: one visual level above the screen background, one below the input fields.
 * Used for the event block and for each issue, so the eye separates them at a glance.
 * Paddings stay compact — the hierarchy comes from the background/border, not empty space.
 */
@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapeLarge,
        // No border, no elevation — the dark CardBg on the slightly darker AppBackground gives
        // enough hierarchy on its own (matches the new history-card look).
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            content = content
        )
    }
}

/**
 * Read-only Date field that opens the Material 3 [DatePicker] on tap. A transparent overlay
 * captures the tap (the field itself is read-only). When a date is set, the trailing clear "X"
 * is exposed (the overlay leaves its area free); otherwise a calendar icon is shown.
 */
@Composable
private fun DateClickField(
    value: String,
    onClear: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Compact box: tap anywhere opens the date picker (the Box is clickable). When a date is
    // set, a trailing X clears it (its own clickable consumes the tap so the picker doesn't
    // open). When empty, a small calendar glyph is shown.
    val placeholder = stringResource(R.string.event_date_ph)
    Box(
        modifier = modifier
            .clip(AppShapeSmall)
            .background(FieldBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = OutcomeFieldContentInset),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (value.isNotBlank()) value else placeholder,
                color = if (value.isNotBlank()) TextPrimary else TextSecondary,
                fontSize = if (value.isNotBlank()) 12.sp else 11.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (value.isNotBlank()) {
                IconButton(onClick = onClear, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.cd_clear_date),
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            } else {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = stringResource(R.string.cd_pick_date),
                    tint = TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * Read-only Time field that opens the Material 3 [TimePicker] on tap. Disabled (greyed, no
 * overlay) until a date is present, so it can only be set after the date — kept side by side
 * with the date rather than hidden.
 */
@Composable
private fun TimeClickField(
    value: String,
    enabled: Boolean,
    onClear: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Same compact pattern as DateClickField; the whole Box becomes non-clickable when there's
    // no date yet (kept side-by-side with the date but greyed out, per spec).
    val placeholder = stringResource(R.string.event_time_ph)
    Box(
        modifier = modifier
            .clip(AppShapeSmall)
            .background(FieldBackground)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = OutcomeFieldContentInset),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (value.isNotBlank()) value else placeholder,
                color = when {
                    !enabled -> TextTertiary
                    value.isNotBlank() -> TextPrimary
                    else -> TextSecondary
                },
                fontSize = if (value.isNotBlank()) 12.sp else 11.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (enabled && value.isNotBlank()) {
                IconButton(onClick = onClear, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.cd_clear_time),
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            } else {
                Icon(
                    Icons.Default.AccessTime,
                    contentDescription = stringResource(R.string.cd_pick_time),
                    tint = if (enabled) TextSecondary else TextTertiary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * Picker palette with the orange accent removed: selection is a WHITE container with BLACK text;
 * "today" gets a neutral grey ring; everything else follows the dark theme.
 */
@Composable
private fun appDatePickerColors() = DatePickerDefaults.colors(
    containerColor             = DarkSurface,
    titleContentColor          = TextSecondary,
    headlineContentColor       = TextPrimary,
    weekdayContentColor        = TextSecondary,
    subheadContentColor        = TextSecondary,
    navigationContentColor     = TextPrimary,
    yearContentColor           = TextPrimary,
    currentYearContentColor    = TextPrimary,
    selectedYearContentColor   = Color.Black,
    selectedYearContainerColor = Color.White,
    dayContentColor            = TextPrimary,
    selectedDayContentColor    = Color.Black,
    selectedDayContainerColor  = Color.White,
    todayContentColor          = TextPrimary,
    todayDateBorderColor       = TextSecondary,
    dividerColor               = DividerSubtle
)

@Composable
private fun appTimePickerColors() = TimePickerDefaults.colors(
    clockDialColor                     = FieldBackground,
    clockDialSelectedContentColor      = Color.Black,
    clockDialUnselectedContentColor    = TextPrimary,
    selectorColor                      = Color.White,
    containerColor                     = DarkSurface,
    timeSelectorSelectedContainerColor = Color.White,
    timeSelectorSelectedContentColor   = Color.Black,
    timeSelectorUnselectedContainerColor = FieldBackground,
    timeSelectorUnselectedContentColor = TextPrimary
)

@Composable
private fun OutcomeRow(
    outcome: Outcome,
    canRemove: Boolean,
    autoFocusIssue: Boolean,
    onAutoFocusConsumed: () -> Unit,
    onLabelChange: (String) -> Unit,
    onOddsChange: (String) -> Unit,
    onFracNumeratorChange: (String) -> Unit,
    onFracDenominatorChange: (String) -> Unit,
    onAmericanValueChange: (String) -> Unit,
    onAmericanSignToggle: () -> Unit,
    onBookmakerChange: (String) -> Unit,
    onFormatChange: (OddsFormat) -> Unit,
    onRemove: () -> Unit
) {
    // Auto-focus the Issue field of a freshly-added outcome (+ open the IME) so the user can
    // start typing immediately. A short delay lets the new CompactField finish composing and
    // attach to the FocusRequester before we requestFocus.
    val labelFocusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(autoFocusIssue) {
        if (autoFocusIssue) {
            delay(50)
            runCatching { labelFocusRequester.requestFocus() }
            keyboard?.show()
            onAutoFocusConsumed()
        }
    }

    // Two lines per outcome:
    //   Line 1: [Issue name (wide)] [Bookmaker]
    //   Line 2: [EU/UK/US selector] [Cote field (remaining width)] [trash]
    // scrollIntoViewOnFocus on the OUTER Column (the whole two-line OutcomeRow) so any
    // sub-field gaining focus (Issue, Book, EU/UK/US cote, even the UK denominator nested in
    // the FractionalField's host) scrolls the LazyColumn to keep the WHOLE issue visible
    // above the IME — never half-cut. The inner fields' own scroll helpers are removed below
    // to avoid double-firing.
    Column(
        modifier = Modifier.scrollIntoViewOnFocus(),
        verticalArrangement = Arrangement.spacedBy(OutcomeRowSpacing)
    ) {

        // Line 1: Issue (weight 1.5f) + Book (weight 1f), 36dp tall, placeholder-only fields.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(OutcomeFieldHeight),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(OutcomeRowSpacing)
        ) {
            CompactField(
                value = outcome.label,
                onValueChange = onLabelChange,
                label = stringResource(R.string.outcome),
                modifier = Modifier.weight(1.5f).fillMaxHeight(),
                focusRequester = labelFocusRequester,
                scrollIntoViewOnFocus = false  // handled by the outer OutcomeRow Column.
            )
            CompactField(
                value = outcome.bookmaker,
                onValueChange = onBookmakerChange,
                label = stringResource(R.string.book),
                modifier = Modifier.weight(1f).fillMaxHeight(),
                scrollIntoViewOnFocus = false
            )
        }

        // Line 2: EU/UK/US selector + cote (yellow ExtraBold, no "Cote" label) + trash, 32dp tall.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(OutcomeLineTwoHeight),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(OutcomeRowSpacing)
        ) {
            // clearFocus before a format change so the next tap on the (possibly replaced) cote
            // field is a clean false→true focus transition, which retriggers select-all.
            val focusManager = LocalFocusManager.current
            OddsFormatSelector(
                selected = outcome.oddsFormat,
                onSelected = { format ->
                    focusManager.clearFocus()
                    onFormatChange(format)
                },
                modifier = Modifier.fillMaxHeight()
            )
            // Cote takes all remaining width to the right of the selector.
            when (outcome.oddsFormat) {
                OddsFormat.FRACTIONAL -> FractionalField(
                    numerator = outcome.fracNumerator,
                    denominator = outcome.fracDenominator,
                    onNumeratorChange = onFracNumeratorChange,
                    onDenominatorChange = onFracDenominatorChange,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                OddsFormat.AMERICAN -> AmericanField(
                    value = outcome.americanValue,
                    negative = outcome.americanNegative,
                    onValueChange = onAmericanValueChange,
                    onToggleSign = onAmericanSignToggle,
                    label = stringResource(R.string.odds),  // "Cote" placeholder when empty.
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                OddsFormat.DECIMAL -> CompactField(
                    value = outcome.decimalRaw,
                    onValueChange = onOddsChange,
                    label = stringResource(R.string.odds),  // "Cote" placeholder when empty.
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textAlign = TextAlign.Center,
                    textColor = OddsYellow,
                    textFontSize = 14.sp,
                    textFontWeight = FontWeight.ExtraBold,
                    scrollIntoViewOnFocus = false,  // handled by the outer OutcomeRow Column.
                    valueTransform = { it.replace(',', '.') },
                    onFocusLost = {
                        val padded = padDecimalToMinTwo(outcome.decimalRaw)
                        if (padded != outcome.decimalRaw) onOddsChange(padded)
                    }
                )
            }
            // Trash is always visible to keep the row aligned. With exactly 2 issues (minimum)
            // both trashes are shown but greyed and inactive; with 3+ they turn red and active.
            IconButton(
                onClick = onRemove,
                enabled = canRemove,
                modifier = Modifier.size(OutcomeRemoveSize)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.cd_remove_outcome),
                    tint = if (canRemove) DangerRed else OverlayWhite30,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
