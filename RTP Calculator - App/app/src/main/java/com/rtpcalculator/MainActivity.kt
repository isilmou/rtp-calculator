package com.rtpcalculator

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.play.core.review.ReviewManagerFactory
import com.rtpcalculator.model.hasMeaningfulContent
import com.rtpcalculator.ui.BetAnalysisCard
import com.rtpcalculator.ui.HistoryScreen
import com.rtpcalculator.ui.LocalFormScrollState
import com.rtpcalculator.ui.LocalFormViewportCoords
import com.rtpcalculator.ui.PrimaryCtaButton
import com.rtpcalculator.ui.renderAndShareAnalysis
import com.rtpcalculator.ui.theme.AppShapeSmall
import com.rtpcalculator.ui.theme.CtaOnSurface
import com.rtpcalculator.ui.theme.CtaSurface
import com.rtpcalculator.ui.theme.OverlayWhite30
import com.rtpcalculator.ui.theme.TextPrimary
import com.rtpcalculator.ui.theme.RTPCalculatorTheme
import com.rtpcalculator.viewmodel.BetViewModel
import com.rtpcalculator.viewmodel.UiMessage
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: BetViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Single splash: the app icon on its background, handled by core-splashscreen on every
        // API level (system splash on 12+, backported window background below). Kept on screen
        // only until the history has been read from disk, then the app appears directly.
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { !viewModel.historyLoaded.value }
        enableEdgeToEdge()
        trackOpenCountAndRequestReview()
        // Julien's two test phones are declared HERE (SDK-hashed device ids, not secrets, printed by
        // the SDK itself in logcat). Independent of the AdMob console registration, which relies on
        // the advertising id and silently stops matching when that id is reset — this is what made
        // release test builds generate real impressions. On any other device these ids match
        // nothing, so production traffic is unaffected. Must be set before the first loadAd.
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setTestDeviceIds(TEST_DEVICE_IDS)
                .build()
        )
        // Initialize the Ads SDK OFF the main thread so its disk I/O + SDK bootstrap no longer
        // delays the first frame. AdBanner's loadAd calls issued before init completes are queued
        // by the SDK and served once initialization finishes.
        lifecycleScope.launch(Dispatchers.IO) {
            MobileAds.initialize(this@MainActivity)
        }
        setContent {
            RTPCalculatorTheme {
                RtpApp(viewModel = viewModel)
            }
        }
    }

    private fun trackOpenCountAndRequestReview() {
        val prefs = getSharedPreferences("rtp_prefs", Context.MODE_PRIVATE)
        val count = prefs.getInt("open_count", 0) + 1
        prefs.edit().putInt("open_count", count).apply()
        if (count == 5) {
            val manager = ReviewManagerFactory.create(this)
            manager.requestReviewFlow().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    manager.launchReviewFlow(this, task.result)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RtpApp(viewModel: BetViewModel) {
    val history  by viewModel.history.collectAsState()
    var showForm by remember { mutableStateOf(false) }

    // Leaving the form (back button OR the TopAppBar close icon) no longer silently throws away
    // an in-progress analysis. If the form holds any real input — or it was reopened from an
    // existing history entry (matched by id) — we AUTO-SAVE silently using the exact same path as
    // the SAVE TO HISTORY button (saveToHistory keeps savedAt stable and updates in place, so a
    // reopened analysis never duplicates). Only a pristine/blank form is discarded.
    // Reads .value off the ViewModel at call time, so remember(viewModel) always sees current data.
    val closeForm: () -> Unit = remember(viewModel) {
        {
            val current = viewModel.analyses.value.firstOrNull()
            if (current != null &&
                (current.hasMeaningfulContent() ||
                    viewModel.history.value.any { it.id == current.id })
            ) {
                // SAVE branch — identical to the SAVE TO HISTORY button.
                viewModel.saveToHistory(current.id)
                viewModel.deleteAnalysis(current.id)
            } else {
                // DISCARD branch — fresh, empty form: drop it (previous behaviour).
                current?.let { viewModel.deleteAnalysis(it.id) }
            }
            showForm = false
        }
    }

    BackHandler(enabled = showForm, onBack = closeForm)

    // One-shot UI messages from the VM (e.g. "Analysis duplicated") -> brief Snackbar on the
    // history Scaffold. Mapping enum -> localised string happens here so the VM stays
    // Android-string-free.
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { msg ->
            val text = when (msg) {
                UiMessage.ShareFailed -> context.getString(R.string.share_error)
            }
            snackbarHostState.showSnackbar(text, duration = SnackbarDuration.Short)
        }
    }

    // Share pipeline: capture an off-screen ShareableHistoryCard PNG, then launch the system
    // share sheet. A simple in-flight guard ignores extra rapid taps while a share is in
    // progress (image generation is fast but not instant).
    val activity = context as? android.app.Activity
    val shareScope = rememberCoroutineScope()
    var isSharing by remember { mutableStateOf(false) }
    val onShareAnalysis: (String) -> Unit = remember(activity, viewModel) {
        { id ->
            val target = viewModel.history.value.find { it.id == id }
            if (!isSharing && activity != null && target != null) {
                isSharing = true
                shareScope.launch {
                    try {
                        renderAndShareAnalysis(activity, target)
                    } catch (t: Throwable) {
                        // Diagnostic log — filter logcat by "RtpShare" to see the real exception.
                        android.util.Log.e("RtpShare", "Capture failed", t)
                        viewModel.notifyShareFailed()
                    } finally {
                        isSharing = false
                    }
                }
            }
        }
    }

    // Stable callback (remembered) so HistoryCard stays skippable — an inline lambda here would
    // be reallocated on every RtpApp recomposition and force all visible cards to recompose.
    val onReopenAnalysis: (String) -> Unit = remember(viewModel) {
        { id ->
            viewModel.clearAnalyses()
            viewModel.reopenFromHistory(id)
            showForm = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                HistoryScreen(
                    history  = history,
                    onReopen = onReopenAnalysis,
                    onDelete = viewModel::deleteFromHistory,
                    onDuplicate = viewModel::duplicateInHistory,
                    onShare = onShareAnalysis,
                    scrollToHistoryId = viewModel.scrollToHistoryId,
                    bottomContentPadding = 82.dp,
                    banner = { AdBanner() }
                )
                PrimaryCtaButton(
                    text = stringResource(R.string.create_bet_analysis),
                    onClick = {
                        viewModel.clearAnalyses()
                        viewModel.addAnalysis()
                        showForm = true
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = showForm,
            enter = slideInVertically(initialOffsetY = { it }),
            exit  = slideOutVertically(targetOffsetY = { it })
        ) {
            val analyses by viewModel.analyses.collectAsState()
            val activeAnalysis = analyses.firstOrNull()
            val focusOutcomeId by viewModel.focusOutcomeId.collectAsState()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (activeAnalysis != null) {
                    // Defer the form banner's ad load until the slide-in animation has settled, so
                    // the first frames of the opening animation aren't sharing the main thread with
                    // an AdView inflate + network loadAd. ~300 ms ≈ the slide duration + a buffer.
                    var bannerReady by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        delay(300)
                        bannerReady = true
                    }
                    Scaffold(
                        contentWindowInsets = WindowInsets(0),
                        topBar = {
                            Column {
                                CenterAlignedTopAppBar(
                                    title = {
                                        val newBetAnalysisLabel = stringResource(R.string.new_bet_analysis)
                                        Text(
                                            text = activeAnalysis.eventName.ifBlank { newBetAnalysisLabel },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    navigationIcon = {
                                        IconButton(onClick = closeForm) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = stringResource(R.string.cd_close),
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    },
                                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        titleContentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                AdBanner(loadAds = bannerReady)
                            }
                        },
                        bottomBar = {
                            // STICKY SAVE — outside the LazyColumn so it stays visible at the
                            // bottom while the user scrolls. HIDDEN while the IME is up to
                            // avoid accidental taps that save & close the analysis mid-typing.
                            // (.navigationBarsPadding keeps it clear of the system nav bar
                            // when it IS shown.)
                            val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
                            if (!imeVisible) {
                                PrimaryCtaButton(
                                    text = stringResource(R.string.save_to_history),
                                    onClick = {
                                        viewModel.saveToHistory(activeAnalysis.id)
                                        viewModel.deleteAnalysis(activeAnalysis.id)
                                        showForm = false
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .navigationBarsPadding()
                                        .padding(start = 12.dp, end = 12.dp, bottom = 10.dp, top = 6.dp)
                                )
                            }
                        }
                    ) { innerPadding ->
                        // Hoisted state for scrollIntoViewOnFocus: it needs to drive the
                        // LazyColumn's scroll directly to actively reposition any focused field
                        // at a fixed offset above the IME (no-op-when-visible was the old
                        // BringIntoView behaviour that left high-on-screen fields stranded).
                        val formListState = rememberLazyListState()
                        val formViewportCoords = remember { mutableStateOf<LayoutCoordinates?>(null) }
                        CompositionLocalProvider(
                            LocalFormScrollState provides formListState,
                            LocalFormViewportCoords provides formViewportCoords
                        ) {
                            LazyColumn(
                                state = formListState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    // innerPadding already reserves the FULL bottomBar height
                                    // (button + its paddings + the nav-bar inset the bottomBar
                                    // consumes via its own navigationBarsPadding). That IS the
                                    // functional clearance, so the last field can scroll above
                                    // the sticky button — keep it. A second navigationBarsPadding()
                                    // here double-counted the nav-bar inset and was the main cause
                                    // of the tall black band above the button.
                                    .padding(innerPadding)
                                    .imePadding()
                                    .onGloballyPositioned { formViewportCoords.value = it },
                                // bottom = small intentional margin only; the real clearance comes
                                // from innerPadding above (a full 16dp here re-stacked on top of it).
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                            ) {
                                item {
                                    BetAnalysisCard(
                                    analysis              = activeAnalysis,
                                    onDateChange          = { viewModel.updateDate(activeAnalysis.id, it) },
                                    onTimeChange          = { viewModel.updateTime(activeAnalysis.id, it) },
                                    onEventNameChange     = { viewModel.updateEventName(activeAnalysis.id, it) },
                                    onBetTypeChange       = { viewModel.updateBetType(activeAnalysis.id, it) },
                                    onOutcomeLabelChange  = { outcomeId, label ->
                                        viewModel.updateOutcomeLabel(activeAnalysis.id, outcomeId, label)
                                    },
                                    onOutcomeOddsChange   = { outcomeId, odds ->
                                        viewModel.updateOutcomeOdds(activeAnalysis.id, outcomeId, odds)
                                    },
                                    onOutcomeFracNumeratorChange = { outcomeId, value ->
                                        viewModel.updateOutcomeFracNumerator(activeAnalysis.id, outcomeId, value)
                                    },
                                    onOutcomeFracDenominatorChange = { outcomeId, value ->
                                        viewModel.updateOutcomeFracDenominator(activeAnalysis.id, outcomeId, value)
                                    },
                                    onOutcomeAmericanValueChange = { outcomeId, value ->
                                        viewModel.updateOutcomeAmericanValue(activeAnalysis.id, outcomeId, value)
                                    },
                                    onOutcomeAmericanSignToggle = { outcomeId ->
                                        viewModel.toggleOutcomeAmericanSign(activeAnalysis.id, outcomeId)
                                    },
                                    onOutcomeBookmakerChange = { outcomeId, bm ->
                                        viewModel.updateOutcomeBookmaker(activeAnalysis.id, outcomeId, bm)
                                    },
                                    onOutcomeFormatChange = { outcomeId, format ->
                                        viewModel.updateOutcomeFormat(activeAnalysis.id, outcomeId, format)
                                    },
                                    onAddOutcome    = { viewModel.addOutcome(activeAnalysis.id) },
                                    onRemoveOutcome = { viewModel.removeOutcome(activeAnalysis.id, it) },
                                    focusOutcomeId  = focusOutcomeId,
                                    onFocusOutcomeConsumed = viewModel::consumeFocusOutcomeId
                                )
                            }
                        }
                        }
                    }
                }
            }
        }
    }
}

private const val PROD_AD_UNIT_ID = "ca-app-pub-2303541955549784/4795362417"
private const val TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
// Test devices (Samsung Galaxy S9 and S23+): hashed ids as reported by the Ads SDK in logcat.
private val TEST_DEVICE_IDS = listOf(
    "489C9D693BD433E3908C6AE730885490",  // S9
    "15E99F91C172FCDBD7E1C9C53B318BDF"   // S23+
)
private val AD_UNIT_ID = if (BuildConfig.DEBUG) TEST_AD_UNIT_ID else PROD_AD_UNIT_ID

@Composable
fun AdBanner(loadAds: Boolean = true) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = AD_UNIT_ID
                if (loadAds) loadAd(AdRequest.Builder().build())
            }
        },
        update = { adView ->
            // When the banner starts with loadAds=false (form opening), trigger the load once
            // it flips true — but only if no request is in flight / already served.
            if (loadAds && !adView.isLoading && adView.responseInfo == null) {
                adView.loadAd(AdRequest.Builder().build())
            }
        }
    )
}
