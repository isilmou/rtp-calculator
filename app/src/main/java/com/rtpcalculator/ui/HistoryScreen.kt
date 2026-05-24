package com.rtpcalculator.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rtpcalculator.R
import com.rtpcalculator.model.BetAnalysis
import com.rtpcalculator.model.RtpResult
import com.rtpcalculator.model.calculateRtp
import com.rtpcalculator.ui.theme.CardBg
import com.rtpcalculator.ui.theme.DangerRed
import com.rtpcalculator.ui.theme.OverlayWhite30
import com.rtpcalculator.ui.theme.TextPrimary
import com.rtpcalculator.ui.theme.TextSecondary
import com.rtpcalculator.ui.theme.TextTertiary

private val CardBorder = BorderStroke(1.5.dp, OverlayWhite30)

private fun tintedCardBg(rtpResult: RtpResult?): Color {
    if (rtpResult == null) return CardBg
    return lerp(CardBg, rtpGradientColor(rtpResult.rtp), 0.15f)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    history: List<BetAnalysis>,
    onReopen: (String) -> Unit,
    onDelete: (String) -> Unit,
    bottomContentPadding: Dp = 0.dp,
    banner: @Composable () -> Unit = {}
) {
    var newestFirst by remember { mutableStateOf(true) }

    val sortedEntries = remember(history, newestFirst) {
        sortHistory(history, newestFirst)
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
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { newestFirst = !newestFirst }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.sort_by_event_date),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (newestFirst) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    contentDescription = stringResource(R.string.cd_toggle_sort_order),
                    modifier = Modifier.size(20.dp),
                    tint = TextSecondary
                )
            }
        }

        banner()

        LazyColumn(
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
                    onReopen = { onReopen(analysis.id) },
                    onDelete = { onDelete(analysis.id) },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

@Composable
private fun HistoryCard(
    analysis: BetAnalysis,
    onReopen: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rtpResult = remember(analysis.outcomes) { calculateRtp(analysis.outcomes) }
    val cardColor = remember(rtpResult) { tintedCardBg(rtpResult) }
    val previewOdds = remember(analysis.outcomes) { analysis.outcomes.take(3) }
    val extraCount = analysis.outcomes.size - previewOdds.size

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onReopen),
        shape = RoundedCornerShape(24.dp),
        border = CardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val unnamedLabel = stringResource(R.string.unnamed)
            HeaderArea(
                date      = analysis.date,
                time      = analysis.time,
                eventName = analysis.eventName.ifBlank { unnamedLabel },
                betType   = analysis.betType,
                onDelete  = onDelete
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                previewOdds.forEach { outcome ->
                    OddsChip(outcome.label, outcome.displayOdds(), outcome.bookmaker, modifier = Modifier.weight(1f))
                }
                if (extraCount > 0) {
                    Text(
                        text = "+$extraCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            rtpResult?.let { result ->
                Spacer(modifier = Modifier.height(10.dp))
                RtpDisplay(result = result)
            }
        }
    }
}

@Composable
private fun HeaderArea(
    date: String,
    time: String,
    eventName: String,
    betType: String,
    onDelete: () -> Unit
) {
    val hasDate    = date.isNotBlank()
    val hasTime    = time.isNotBlank()
    val hasBetType = betType.isNotBlank()
    val dateLabel  = if (hasDate && hasTime) "$date — $time" else date
    val noDateLabel = stringResource(R.string.no_date)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = eventName,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(2.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val line2 = remember(hasDate, hasBetType, dateLabel, noDateLabel, betType) {
                buildAnnotatedString {
                    if (hasDate) {
                        withStyle(DatePresentStyle) { append(dateLabel) }
                    } else {
                        withStyle(DateBlankStyle) { append(noDateLabel) }
                    }
                    if (hasBetType) {
                        withStyle(BetTypeStyle) { append(" · ") }
                        withStyle(BetTypeStyle) { append(betType) }
                    }
                }
            }
            Text(
                text = line2,
                textAlign = TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.cd_delete),
                    tint = DangerRed,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private val Line2FontSize = 13.sp

private val DatePresentStyle = SpanStyle(
    color      = TextSecondary,
    fontSize   = Line2FontSize,
    fontWeight = FontWeight.SemiBold
)
private val DateBlankStyle = SpanStyle(
    color     = TextTertiary,
    fontSize  = Line2FontSize,
    fontStyle = FontStyle.Italic
)
private val BetTypeStyle = SpanStyle(
    color    = TextTertiary,
    fontSize = Line2FontSize
)

@Composable
private fun OddsChip(label: String, oddsText: String, bookmaker: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, OverlayWhite30),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (bookmaker.isNotBlank()) {
                Text(
                    text = bookmaker,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Light,
                    color = TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = oddsText,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = TextPrimary
            )
            if (label.isNotBlank()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
