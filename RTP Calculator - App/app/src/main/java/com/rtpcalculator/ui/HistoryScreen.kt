package com.rtpcalculator.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rtpcalculator.R
import com.rtpcalculator.model.BetAnalysis
import com.rtpcalculator.model.RtpResult
import com.rtpcalculator.model.calculateRtp
import com.rtpcalculator.ui.theme.ActionIcon
import com.rtpcalculator.ui.theme.AppShapeLarge
import com.rtpcalculator.ui.theme.AppShapeSmall
import com.rtpcalculator.ui.theme.CardBg
import com.rtpcalculator.ui.theme.DangerRed
import com.rtpcalculator.ui.theme.DividerSubtle
import com.rtpcalculator.ui.theme.FieldBackground
import com.rtpcalculator.ui.theme.TextPrimary
import com.rtpcalculator.ui.theme.TextSecondary
import com.rtpcalculator.ui.theme.TextTertiary

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    history: List<BetAnalysis>,
    onReopen: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDuplicate: (String, Boolean) -> Unit,
    onShare: (String) -> Unit,
    scrollToHistoryId: Flow<String>,
    bottomContentPadding: Dp = 0.dp,
    banner: @Composable () -> Unit = {}
) {
    var newestFirst by remember { mutableStateOf(true) }

    val sortedEntries = remember(history, newestFirst) {
        sortHistory(history, newestFirst)
    }

    // Hoisted state so the scroll-to handler below can drive the list directly.
    val listState = rememberLazyListState()
    // Always read the LATEST sortedEntries from the long-running collector below.
    val sortedEntriesState = rememberUpdatedState(sortedEntries)
    // Small top padding (px) left above a scrolled-to item so it doesn't stick to the top edge.
    // Captured at composable scope (LocalDensity isn't available inside the collector coroutine).
    val scrollTopPaddingPx = with(LocalDensity.current) { 16.dp.roundToPx() }

    // STABLE per-card callbacks so HistoryCard is skippable (its other params — analysis,
    // onReopen/onDelete/onShare — are already stable). onDuplicate needs the live `newestFirst`,
    // so we read it through a snapshot state inside a remembered lambda rather than capturing a
    // fresh closure per item (which would defeat skipping). Toggling the sort no longer recomposes
    // every card — only the list reorders.
    val newestFirstState = rememberUpdatedState(newestFirst)
    val onDuplicateCard = remember(onDuplicate) {
        { id: String -> onDuplicate(id, newestFirstState.value) }
    }

    // Tap on the sort toggle -> smoothly scroll back to the top so the user starts at the
    // first item of the new ordering (otherwise the pixel-offset stays put and the user sees
    // arbitrary middle entries after the swap, which is disorienting).
    // The guard avoids any motion on cold start (list already at top) and when the user
    // taps the toggle while already at the top — no parasitic scroll in those cases.
    LaunchedEffect(newestFirst) {
        if (listState.firstVisibleItemIndex != 0 || listState.firstVisibleItemScrollOffset != 0) {
            listState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(scrollToHistoryId) {
        scrollToHistoryId.collect { id ->
            // saveToHistory / duplicateInHistory set _history.value BEFORE emitting this id, so by
            // the time we get here `sortedEntries` may ALREADY be the re-sorted list. The old
            // approach captured `listBefore` here then waited for a DIFFERENT (!==) instance — but
            // on a date edit that re-sort had already landed, so that wait never resolved and the
            // moved card was never followed. Instead, don't compare against a too-early snapshot:
            //  1) wait until the id is present in the up-to-date sorted list (covers a brand-new
            //     entry whose id only appears once the recomposition runs),
            //  2) let recomposition + LazyColumn relayout + the form's slide-out (~300ms) settle,
            //  3) read the CURRENT index of the id (never a stale pre-sort one) and scroll there.
            snapshotFlow { sortedEntriesState.value }
                .first { list -> list.any { it.id == id } }

            // Settle step 1: a frame + a delay to outlast the form's slide-out (~300ms) so the
            // re-sort recomposition has landed and the LazyColumn has started its relayout.
            withFrameNanos { }
            delay(300)

            // Settle step 2: WAIT FOR REAL LAYOUT STABILITY before judging visibility. The
            // item-placement animation (Modifier.animateItem()) runs AFTER the re-sort; while a
            // card is animating toward its new slot it KEEPS being reported in visibleItemsInfo at
            // an interpolated (still in-viewport) offset even though the scroll anchor says it's
            // off-screen — logs showed firstVisibleIndex=5 yet the target reported offset=110, so
            // the guard wrongly judged it visible and skipped the scroll. Watching only the scroll
            // anchor (firstVisibleItem*) exits too early (it was already stable). So the stability
            // signature ALSO includes the target card's own reported offset (or null when it has
            // left the visible set): the loop only breaks once that offset stops moving, i.e. the
            // placement animation has actually finished. Capped so we never hang.
            var lastSig: Triple<Int, Int, Int?>? = null
            var settleFrames = 0
            while (settleFrames < 90) {
                withFrameNanos { }
                val li = listState.layoutInfo
                val targetOffset = li.visibleItemsInfo.firstOrNull { it.key == id }?.offset
                val sig = Triple(
                    listState.firstVisibleItemIndex,
                    listState.firstVisibleItemScrollOffset,
                    targetOffset
                )
                if (sig == lastSig) break
                lastSig = sig
                settleFrames++
            }

            // Re-read the index from the CURRENT (post re-sort) list — this is the position the
            // card actually occupies now, even if its date moved it far away.
            val targetIndex = sortedEntriesState.value.indexOfFirst { it.id == id }
            if (targetIndex < 0) return@collect

            // Decide visibility by INDEX, never by visibleItemsInfo offsets. Logs proved the
            // offset of a card under Modifier.animateItem() is its ANIMATED/translated visual
            // position, not its logical one (firstVisibleIndex=5 yet the target reported offset=110
            // — impossible in a settled layout), which fooled the offset guard into fullyVisible.
            // The scroll anchor firstVisibleItemIndex is authoritative and animation-proof, so we
            // build the contiguous run of fully-visible indices STARTING from it: a target whose
            // index is below the run (upward move) or above it (downward move) is off-screen.
            fun fullyVisibleRun(): IntRange {
                val li = listState.layoutInfo
                val first = listState.firstVisibleItemIndex
                val clipped = listState.firstVisibleItemScrollOffset > 0
                val visible = li.visibleItemsInfo
                    .filter {
                        it.offset >= li.viewportStartOffset &&
                            it.offset + it.size <= li.viewportEndOffset
                    }
                    .map { it.index }
                    .toHashSet()
                // Top of the run = the anchor item, unless its top edge is clipped.
                val top = if (clipped) first + 1 else first
                var last = top - 1
                var i = top
                while (visible.contains(i)) {
                    last = i
                    i++
                }
                return top..last
            }

            val fullyVisible = targetIndex in fullyVisibleRun()

            if (!fullyVisible) {
                // Smooth follow (animated) for a pleasant glide instead of a hard jump. The
                // concurrent animateItem placement animation can occasionally knock the animated
                // scroll off target, so AFTER it has run we re-check — index-based, the only
                // animation-proof signal — and snap-correct ONLY if the target still isn't in the
                // fully-visible run. In the normal case the re-check passes and the snap never
                // fires (no visible jolt); otherwise it guarantees the landing.
                listState.animateScrollToItem(targetIndex, -scrollTopPaddingPx)
                if (targetIndex !in fullyVisibleRun()) {
                    listState.scrollToItem(targetIndex, -scrollTopPaddingPx)
                }
            }
        }
    }

    if (history.isEmpty()) {
        Column(modifier = Modifier.fillMaxSize()) {
            banner()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_analyses_yet),
                    fontSize = 14.sp,
                    color = TextTertiary,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(AppShapeSmall)
                    .clickable { newestFirst = !newestFirst }
                    // Keep a comfortable ≥36 dp tap target even though the visual content shrank.
                    .heightIn(min = 36.dp)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.sort_by_event_date),
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (newestFirst) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    contentDescription = stringResource(R.string.cd_toggle_sort_order),
                    modifier = Modifier.size(15.dp),
                    tint = TextSecondary
                )
            }
        }

        banner()

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = 8.dp,
                bottom = 8.dp + bottomContentPadding
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(sortedEntries, key = { it.id }) { analysis ->
                HistoryCard(
                    analysis = analysis,
                    onReopen = onReopen,
                    onDelete = onDelete,
                    onDuplicate = onDuplicateCard,
                    onShare = onShare,
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

@Composable
private fun HistoryCard(
    analysis: BetAnalysis,
    // id-based callbacks (stable refs from the caller) so this composable stays skippable: the
    // per-id binding happens INSIDE, only when the card actually (re)composes.
    onReopen: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDuplicate: (String) -> Unit,
    onShare: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val rtpResult = remember(analysis.outcomes) { calculateRtp(analysis.outcomes) }
    val outcomes = analysis.outcomes
    val overflow = outcomes.size > 3
    // Collapsed/expanded state survives recompositions and scrolling within the screen
    // (LazyColumn keys items by id, so the saveable state is per-card naturally).
    var expanded by rememberSaveable { mutableStateOf(false) }
    val visible = if (overflow && !expanded) outcomes.take(3) else outcomes

    // Vertical TRJ accent bar on the LEFT of the card (3.dp wide, full card height).
    // Transparent when no RTP can be computed (incomplete odds) — keeps the card clean.
    val sideBarColor = rtpResult?.let { rtpAccentColor(it.rtp) } ?: Color.Transparent  // same colour as the pill (sky blue for a surebet).

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onReopen(analysis.id) },
        shape = AppShapeLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        // Uniform neutral card background — the side bar + RTP pill carry the only TRJ colour.
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        // IntrinsicSize.Min lets the 3.dp side bar fillMaxHeight() match the content Column
        // — otherwise fillMaxHeight inside a wrap-content Row collapses to zero.
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(sideBarColor)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 9.dp, vertical = 5.dp)
            ) {
                CardHeader(
                    eventName = analysis.eventName,
                    date      = analysis.date,
                    time      = analysis.time,
                    betType   = analysis.betType,
                    rtpResult = rtpResult
                )

                Spacer(modifier = Modifier.height(5.dp))

                if (!overflow) {
                    // Collapsed ≤3 case: a single row, blocks share the full width equally (no
                    // empty padding cells) — so a 2-way bet shows two half-width blocks.
                    OutcomeRows(
                        outcomes = outcomes,
                        rowSpacing = 4.dp,
                        fillIncompleteRows = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Expanded: rows of 3, trailing partial row padded to keep column alignment.
                    // animateContentSize drives the expand/collapse height animation.
                    OutcomeRows(
                        outcomes = visible,
                        rowSpacing = 4.dp,
                        fillIncompleteRows = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    ExpandToggle(
                        expanded = expanded,
                        extraCount = outcomes.size - 3,
                        onClick = { expanded = !expanded }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(thickness = 1.dp, color = DividerSubtle)
                Spacer(modifier = Modifier.height(2.dp))
                CardActionBar(
                    onDuplicate = { onDuplicate(analysis.id) },
                    onShare = { onShare(analysis.id) },
                    onDelete = { onDelete(analysis.id) }
                )
            }
        }
    }
}

@Composable
internal fun CardHeader(
    eventName: String,
    date: String,
    time: String,
    betType: String,
    rtpResult: RtpResult?
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Line 1: metadata segments on the LEFT (only those actually filled in — no fallback,
        // no orphan separator) + RTP pill ALWAYS on the right. When no metadata is set, the
        // left slot is empty (zero-width Spacer) and the pill stays alone at the right.
        val metadata = remember(date, time, betType) {
            buildList {
                if (date.isNotBlank()) add(date)
                if (time.isNotBlank()) add(time)
                if (betType.isNotBlank()) add(betType)
            }.joinToString(" · ")
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (metadata.isNotEmpty()) {
                Text(
                    text = metadata,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            if (rtpResult != null) {
                RtpPill(rtp = rtpResult.rtp)
            }
        }

        // Line 2: event name CENTRED — only when actually set. No "unnamed" fallback, no
        // reserved height when empty (the card just compacts).
        if (eventName.isNotBlank()) {
            // Gap between line 1 (meta + TRJ) and line 2 (event name) — gives the event name some
            // air above. Same value in both the on-screen card and the shared PNG.
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = eventName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Bottom action bar, right-aligned: Duplicate + Share (posed only, no action yet) and a
 * standalone red trash button (functional — deletes the history entry).
 */
@Composable
private fun CardActionBar(onDuplicate: () -> Unit, onShare: () -> Unit, onDelete: () -> Unit) {
    // Right-aligned, generous spacing (per brief) — Duplicate + Share (icon + label) then the
    // standalone red trash icon. Inner clickables consume the tap so the card's onReopen
    // never fires from these actions.
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CardAction(
            icon = Icons.Outlined.ContentCopy,
            label = stringResource(R.string.duplicate),
            onClick = onDuplicate
        )
        CardAction(
            icon = Icons.Outlined.Share,
            label = stringResource(R.string.share),
            onClick = onShare
        )
        // Delete uses IconButton to keep a 48-dp touch target despite the small 14-dp icon.
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.cd_delete),
                tint = DangerRed,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun CardAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(AppShapeSmall)
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = ActionIcon,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            color = ActionIcon
        )
    }
}

/**
 * Discrete centred pill below the odds blocks: shows "+N" when collapsed (N = extra issues
 * beyond the 3 shown), or the localised "Show less" when expanded. Owns its own clickable so
 * the tap never bubbles up to the card's "reopen" handler.
 */
@Composable
private fun ExpandToggle(expanded: Boolean, extraCount: Int, onClick: () -> Unit) {
    val label = if (expanded) stringResource(R.string.show_less) else "+$extraCount"
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        // Discreet pill — small text on the field background, no border, just enough padding
        // to keep a confortable tap target. It used to dominate visually like a block of its
        // own; now it reads as a subordinate toggle below the odds blocks.
        Box(
            modifier = Modifier
                .clip(AppShapeSmall)
                .background(FieldBackground)
                .heightIn(min = 22.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
        }
    }
}
