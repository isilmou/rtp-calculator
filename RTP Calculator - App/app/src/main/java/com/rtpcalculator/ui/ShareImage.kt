package com.rtpcalculator.ui

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.rtpcalculator.R
import com.rtpcalculator.model.BetAnalysis
import com.rtpcalculator.ui.theme.AppBackground
import com.rtpcalculator.ui.theme.RTPCalculatorTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Output is rendered at a FIXED density so a phone with any pixel density produces the same
// dp layout (1080px at density 3.0 ≈ 360dp wide — typical phone card width).
private const val SHARE_IMAGE_WIDTH_PX = 1080
private const val SHARE_IMAGE_DENSITY = 3.0f

private const val SHARES_SUBDIR = "shares"
// FileProvider authority — keep in sync with the <provider> in AndroidManifest.xml.
private const val FILE_PROVIDER_AUTHORITY_SUFFIX = ".fileprovider"
// Lean cache: drop share PNGs older than this on each share (the receiving app has had time
// to consume the URI by then; failed/abandoned shares get cleaned up too).
private const val SHARE_CACHE_MAX_AGE_MS = 6L * 60L * 60L * 1000L  // 6 hours

// Defensive ceiling on capture height — well above any realistic card (a 50-outcome card is
// nowhere near this), just to guarantee we never allocate a runaway bitmap if something
// upstream returns an absurd measured height. NOT the cause of any current truncation.
private const val MAX_CAPTURE_HEIGHT_PX = 10_000

/**
 * Renders [analysis] as a [ShareableHistoryCard] PNG into the app cache and launches the system
 * share sheet for it. Throws on rendering / I/O failure — callers should catch and surface a
 * friendly error (Snackbar) to the user.
 */
suspend fun renderAndShareAnalysis(activity: Activity, analysis: BetAnalysis) {
    val bitmap = renderComposableToBitmap(
        activity,
        SHARE_IMAGE_WIDTH_PX,
        SHARE_IMAGE_DENSITY
    ) {
        RTPCalculatorTheme {
            ShareableCardCanvas(analysis = analysis)
        }
    }
    val file = try {
        withContext(Dispatchers.IO) { writePngToShareCache(activity, bitmap, analysis.id) }
    } finally {
        bitmap.recycle()
    }
    launchShareIntent(activity, file)
}

private fun writePngToShareCache(activity: Activity, bitmap: Bitmap, analysisId: String): File {
    val dir = File(activity.cacheDir, SHARES_SUBDIR).apply { mkdirs() }
    cleanOldShares(dir)
    val out = File(dir, "share_${System.currentTimeMillis()}_${analysisId.take(8)}.png")
    FileOutputStream(out).use { stream ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    }
    return out
}

private fun cleanOldShares(dir: File) {
    val cutoff = System.currentTimeMillis() - SHARE_CACHE_MAX_AGE_MS
    dir.listFiles()?.forEach { file ->
        if (file.lastModified() < cutoff) file.delete()
    }
}

private fun launchShareIntent(activity: Activity, file: File) {
    val uri = FileProvider.getUriForFile(
        activity,
        activity.packageName + FILE_PROVIDER_AUTHORITY_SUFFIX,
        file
    )
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        // ClipData on the intent is what makes the share sheet render a preview thumbnail
        // for the image; EXTRA_STREAM alone is enough for delivery but not for the preview.
        clipData = ClipData.newUri(activity.contentResolver, "Image", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(send, activity.getString(R.string.share)).apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    activity.startActivity(chooser)
}

/**
 * The share canvas: the off-screen card on a uniform [AppBackground] with a small breathing
 * margin. The bitmap is captured at the card's exact natural size (width × height), so there are
 * no large background bands around it — only this 12.dp padding.
 */
@Composable
private fun ShareableCardCanvas(analysis: BetAnalysis) {
    Box(
        modifier = Modifier
            .background(AppBackground)
            .padding(12.dp)
    ) {
        ShareableHistoryCard(analysis = analysis)
    }
}

/**
 * Composes [content] in an invisible, fixed-density [ComposeView] attached to the activity's
 * content root, waits for the first non-zero layout pass, then draws the view to a Bitmap.
 * The view is removed immediately after the draw, so nothing visible flashes on screen.
 *
 * The density override is what makes the output stable across devices: regardless of the
 * screen's native density, this composition behaves as if it were rendered on a density-3.0
 * device, so a [widthPx] of 1080 always means ~360dp of layout width.
 */
private suspend fun renderComposableToBitmap(
    activity: Activity,
    widthPx: Int,
    densityValue: Float,
    content: @Composable () -> Unit
): Bitmap = suspendCancellableCoroutine { cont ->
    val root = activity.findViewById<ViewGroup>(android.R.id.content)
    val view = ComposeView(activity).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(density = densityValue, fontScale = 1.0f)
            ) { content() }
        }
        alpha = 0f
        isClickable = false
        isFocusable = false
        layoutParams = ViewGroup.LayoutParams(widthPx, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
    var captured = false
    val listener = object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            if (captured) return
            // Wait until the initial composition has produced a real layout.
            if (view.width <= 0 || view.height <= 0) return
            captured = true
            try {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)

                // The parent FrameLayout (android.R.id.content) feeds children a heightSpec
                // of AT_MOST = screen height. When the composable's natural height exceeds
                // that (e.g. a card with 10 outcomes + signature), Compose clamps the
                // returned height — and the bottom (signature) is silently truncated when
                // we draw. Re-measure with UNSPECIFIED height to recover the true natural
                // height, then re-layout to it before drawing.
                val widthSpec = View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY)
                val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                view.measure(widthSpec, heightSpec)
                val naturalHeight = view.measuredHeight.coerceAtMost(MAX_CAPTURE_HEIGHT_PX)
                view.layout(0, 0, widthPx, naturalHeight)

                // Capture at the card's exact natural size — no square padding bands.
                val bitmap = Bitmap.createBitmap(widthPx, naturalHeight, Bitmap.Config.ARGB_8888).also {
                    view.draw(Canvas(it))
                }
                root.removeView(view)
                cont.resume(bitmap)
            } catch (t: Throwable) {
                runCatching { root.removeView(view) }
                if (cont.isActive) cont.resumeWithException(t)
            }
        }
    }
    view.viewTreeObserver.addOnGlobalLayoutListener(listener)
    cont.invokeOnCancellation {
        runCatching {
            view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
            root.removeView(view)
        }
    }
    root.addView(view)
}
