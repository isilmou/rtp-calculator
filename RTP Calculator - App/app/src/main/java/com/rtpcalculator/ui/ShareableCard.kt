package com.rtpcalculator.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.rtpcalculator.R
import com.rtpcalculator.model.BetAnalysis
import com.rtpcalculator.model.calculateRtp
import com.rtpcalculator.ui.theme.AppShapeLarge
import com.rtpcalculator.ui.theme.AppShapeSmall
import com.rtpcalculator.ui.theme.CardBg
import com.rtpcalculator.ui.theme.DividerSubtle
import com.rtpcalculator.ui.theme.TextPrimary
import com.rtpcalculator.ui.theme.TextTertiary

/**
 * Off-screen variant of [HistoryCard] used to generate the PNG that gets shared. Reuses the
 * same building blocks (header, RTP pill, OddsBlock, etc.) so the image stays visually faithful
 * to what the user sees on screen, but:
 *   - it is ALWAYS expanded (every outcome shown, in chunked rows of 3 with empty cells in
 *     incomplete trailing rows so column widths stay aligned with line 1),
 *   - the action bar (Duplicate / Share / Delete) is removed (no sense outside the app),
 *   - a discrete signature row (logo + app name + "rtpcalculator.com") is appended.
 */
@Composable
fun ShareableHistoryCard(analysis: BetAnalysis) {
    val rtpResult = remember(analysis.outcomes) { calculateRtp(analysis.outcomes) }
    val sideBarColor = rtpResult?.let { rtpAccentColor(it.rtp) } ?: Color.Transparent  // same colour as the pill (sky blue for a surebet).
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapeLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        // Same side-bar + content structure as the on-screen card, but TIGHTER paddings.
        // The shared PNG is rendered rectangular at the card's exact natural size (no square
        // compositing), so these compact paddings keep the captured image tight to the content.
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
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                CardHeader(
                    eventName = analysis.eventName,
                    date      = analysis.date,
                    time      = analysis.time,
                    betType   = analysis.betType,
                    rtpResult = rtpResult
                )

                Spacer(modifier = Modifier.height(5.dp))

                // Always expanded: rows of 3, trailing partial row padded for column alignment.
                // Tighter 3.dp spacing than the on-screen card so the PNG stays compact/near-square.
                // Pad incomplete rows ONLY when there are multiple rows (4+ outcomes), matching the
                // on-screen expanded card. With ≤3 outcomes it's a single row, so padding would
                // leave a phantom empty column and shrink the blocks — instead they fill the width
                // (2 → 50/50, 3 → 33% each), exactly like the app.
                OutcomeRows(
                    outcomes = analysis.outcomes,
                    rowSpacing = 3.dp,
                    fillIncompleteRows = analysis.outcomes.size > 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // Drop the divider in compact mode — a 6 dp spacer is enough to detach the
                // signature from the odds without burning extra vertical space.
                Spacer(modifier = Modifier.height(6.dp))
                ShareSignature()
            }
        }
    }
}

@Composable
private fun ShareSignature() {
    // We can't use painterResource(R.mipmap.ic_launcher) here: on API >= 26 the resource
    // resolver picks mipmap-anydpi-v26/ic_launcher.xml (an <adaptive-icon>), which Compose's
    // painterResource doesn't support (it only loads <vector> or bitmap drawables). Instead,
    // load the drawable through the platform (which DOES understand adaptive icons) and
    // rasterise it ourselves to an ImageBitmap.
    val context = LocalContext.current
    val iconBitmap = remember(context) {
        ContextCompat.getDrawable(context, R.mipmap.ic_launcher)!!
            // 72px = 24dp at the share-image density of 3.0; native pixel size avoids upscaling.
            .toBitmap(width = 72, height = 72)
            .asImageBitmap()
    }
    // stringResource must be read in composable scope (buildAnnotatedString's lambda is not).
    val appName = stringResource(R.string.app_name)
    val site = stringResource(R.string.share_signature_site)  // not translated: the web calculator's address.
    Column(modifier = Modifier.fillMaxWidth()) {
        // Thin rule detaching the signature from the analysis above.
        Spacer(modifier = Modifier.height(6.dp))
        HorizontalDivider(thickness = 0.5.dp, color = DividerSubtle)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                bitmap = iconBitmap,
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .clip(AppShapeSmall)
            )
            Spacer(modifier = Modifier.width(8.dp))
            // Single-line styled signature: "<app name> · rtpcalculator.com" — bold app name +
            // italic site address, separated by a mid-dot (2.2.0: replaces "Available on Google
            // Play", the two together did not fit on the 10 sp line). Ellipsis is a safety net.
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = TextPrimary, fontWeight = FontWeight.Bold)) {
                        append(appName)
                    }
                    withStyle(SpanStyle(color = TextTertiary)) {
                        append(" · ")
                    }
                    withStyle(SpanStyle(color = TextTertiary, fontStyle = FontStyle.Italic)) {
                        append(site)
                    }
                },
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
