package com.rtpcalculator.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rtpcalculator.model.Outcome
import com.rtpcalculator.ui.theme.AppBackground
import com.rtpcalculator.ui.theme.AppShapeSmall
import com.rtpcalculator.ui.theme.OddsYellow
import com.rtpcalculator.ui.theme.OutcomeBlockBg
import com.rtpcalculator.ui.theme.TextSecondary
import com.rtpcalculator.ui.theme.TextTertiary

/**
 * Lays out a list of outcomes as rows of up to 3 [OddsBlock]s. Shared by the on-screen history
 * card (collapsed single row, and expanded multi-row) and the off-screen [ShareableHistoryCard],
 * so all three render identically.
 *
 * Per ROW it computes the label/bookmaker reservation flags (true if ANY outcome in that row uses
 * them) so each row collapses or stays aligned independently.
 *
 *  - [rowSpacing] drives BOTH the gap between rows (vertical) and between blocks (horizontal):
 *    4.dp on screen, 3.dp on the shared PNG.
 *  - [fillIncompleteRows]: when true, a trailing partial row is padded with empty weighted cells
 *    so its blocks keep the same width/column alignment as full rows (expanded card + shared PNG).
 *    When false (the collapsed ≤3 case), the blocks simply share the full width equally — a 2-way
 *    bet shows two half-width blocks, never two thirds + a gap.
 */
@Composable
internal fun OutcomeRows(
    outcomes: List<Outcome>,
    rowSpacing: Dp,
    fillIncompleteRows: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(rowSpacing)
    ) {
        outcomes.chunked(3).forEach { rowOutcomes ->
            // Each row computes its own flags independently, so a row with no labels collapses
            // while a sibling row that has them keeps its reserved space.
            val rowHasAnyLabel = rowOutcomes.any { it.label.isNotBlank() }
            val rowHasAnyBookmaker = rowOutcomes.any { it.bookmaker.isNotBlank() }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(rowSpacing)
            ) {
                rowOutcomes.forEach { outcome ->
                    OddsBlock(
                        label = outcome.label,
                        oddsText = outcome.displayOdds(),
                        bookmaker = outcome.bookmaker,
                        modifier = Modifier.weight(1f),
                        reserveLabelSpace = rowHasAnyLabel,
                        reserveBookmakerSpace = rowHasAnyBookmaker
                    )
                }
                if (fillIncompleteRows) {
                    repeat(3 - rowOutcomes.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * One issue block: ISSUE on top, COTE in the middle (dominant, auto-sized to stay whole on one
 * line), BOOKMAKER at the bottom. The label/bookmaker rows are only laid out when the WHOLE
 * row reserves space for them ([reserveLabelSpace] / [reserveBookmakerSpace], computed once per
 * row by the caller) — so a row with no labels/bookmakers collapses entirely, while a mixed row
 * keeps minLines = 1 on every column to stay aligned. The odds sit in a min-height box so the
 * blocks of a row keep the same height regardless of the font size each cote lands on.
 */
@Composable
internal fun OddsBlock(
    label: String,
    oddsText: String,
    bookmaker: String,
    modifier: Modifier = Modifier,
    // Computed per ROW by the caller: true when ANY outcome in the row has a label/bookmaker.
    // When true, every column reserves one line (minLines = 1) even if its own value is blank,
    // so the 3 cotes stay vertically aligned. When false, the Text is dropped outright (no
    // empty placeholder line) and the row collapses by ~one line height.
    reserveLabelSpace: Boolean = true,
    reserveBookmakerSpace: Boolean = true
) {
    // Piste B: each issue is wrapped in an OutcomeBlockBg surface a touch lighter than CardBg,
    // so the 3 columns read as distinct islands (and stay legible when overflow mode stacks
    // rows — ROW N's bookmaker no longer blends into ROW N+1's issue label on the same bg).
    // Inside: label on top + cote inside the still-darker AppBackground frame + bookmaker
    // at the bottom. Whether the label/bookmaker line is laid out is decided PER ROW by the
    // caller via reserveLabelSpace / reserveBookmakerSpace: when a row reserves a line, every
    // column keeps it (minLines = 1, blank or not) so the 3 cotes stay aligned; when no column
    // in the row uses it, the line is dropped entirely and the row collapses.
    //
    // Paddings/spacings unified to a tight, dense layout for BOTH the on-screen card and the
    // shared PNG — the previous "compact" flag is gone; the values below are the new common
    // baseline that makes the cote (16 sp ExtraBold OddsYellow) stand out more by contrast.
    Surface(
        shape = AppShapeSmall,
        color = OutcomeBlockBg,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (reserveLabelSpace) {
                Text(
                    text = label,
                    fontSize = 9.sp,
                    // lineHeight pinned to the glyph size removes the ~1.3-1.5x default leading,
                    // so a blank/filled label reserves only its own height (no breathing air).
                    lineHeight = 9.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    minLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            // No explicit Spacer between the label, the cote frame and the bookmaker — the
            // font line-leading already gives ~1-2 px of visual breathing. Result is a
            // visibly tighter trio (per the "resserrement drastique" request); bump back to a
            // 1 dp Spacer if it ever feels glued on device.
            Surface(
                shape = AppShapeSmall,
                color = AppBackground,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(vertical = 1.dp)) {
                    AutoSizeOddsText(text = oddsText, modifier = Modifier.fillMaxWidth())
                }
            }
            if (reserveBookmakerSpace) {
                Text(
                    text = bookmaker,
                    fontSize = 8.sp,
                    lineHeight = 8.sp,
                    color = TextTertiary,
                    maxLines = 1,
                    minLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Odds text that always stays whole on ONE line: starts at [maxFontSize] (big, impactful for
 * short odds) and steps down by 1sp on overflow until it fits, down to [minFontSize] — so long
 * odds (107/100, 11.205, +1500) shrink to fit instead of wrapping or being clipped. The
 * fixed-height box keeps every block the same height whatever the font size lands on.
 */
@Composable
private fun AutoSizeOddsText(
    text: String,
    modifier: Modifier = Modifier,
    maxFontSize: TextUnit = 16.sp,
    minFontSize: TextUnit = 11.sp
) {
    var fontSize by remember(text) { mutableStateOf(maxFontSize) }
    // Min-height box: tall enough to never crush the cote glyph (~19-20 dp at 16 sp) and to keep
    // a row's blocks aligned, but it no longer pads out fixed slack above/below the digits.
    Box(modifier = modifier.heightIn(min = 20.dp), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            color = OddsYellow,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            textAlign = TextAlign.Center,
            onTextLayout = { result ->
                if (result.hasVisualOverflow && fontSize > minFontSize) {
                    fontSize = (fontSize.value - 1f).coerceAtLeast(minFontSize.value).sp
                }
            }
        )
    }
}
