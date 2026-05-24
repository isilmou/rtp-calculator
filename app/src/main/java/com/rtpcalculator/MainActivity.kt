package com.rtpcalculator

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.play.core.review.ReviewManagerFactory
import com.rtpcalculator.ui.BetAnalysisCard
import com.rtpcalculator.ui.HistoryScreen
import com.rtpcalculator.ui.theme.ActionAccent
import com.rtpcalculator.ui.theme.ButtonLabel
import com.rtpcalculator.ui.theme.OverlayWhite30
import com.rtpcalculator.ui.theme.RTPCalculatorTheme
import com.rtpcalculator.viewmodel.BetViewModel
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val viewModel: BetViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MobileAds.initialize(this)
        trackOpenCountAndRequestReview()
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
    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(500)
        showSplash = false
    }

    val closeAndDiscard: () -> Unit = remember(viewModel) {
        {
            viewModel.analyses.value.firstOrNull()?.let { viewModel.deleteAnalysis(it.id) }
            showForm = false
        }
    }

    BackHandler(enabled = showForm, onBack = closeAndDiscard)

    Box(modifier = Modifier.fillMaxSize()) {

        Scaffold { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                HistoryScreen(
                    history  = history,
                    onReopen = { id ->
                        viewModel.clearAnalyses()
                        viewModel.reopenFromHistory(id)
                        showForm = true
                    },
                    onDelete = viewModel::deleteFromHistory,
                    bottomContentPadding = 82.dp,
                    banner = { AdBanner() }
                )
                Button(
                    onClick = {
                        viewModel.clearAnalyses()
                        viewModel.addAnalysis()
                        showForm = true
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, OverlayWhite30),
                    colors = ButtonDefaults.buttonColors(containerColor = ActionAccent)
                ) {
                    Text(
                        text = stringResource(R.string.create_bet_analysis),
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Bold,
                        color = ButtonLabel,
                        fontSize = 18.sp
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showForm,
            enter = slideInVertically(initialOffsetY = { it }),
            exit  = slideOutVertically(targetOffsetY = { it })
        ) {
            val analyses by viewModel.analyses.collectAsState()
            val activeAnalysis = analyses.firstOrNull()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (activeAnalysis != null) {
                    Scaffold(
                        contentWindowInsets = WindowInsets(0),
                        topBar = {
                            Column {
                                CenterAlignedTopAppBar(
                                    title = {
                                        val newBetAnalysisLabel = stringResource(R.string.new_bet_analysis)
                                        Text(
                                            text = activeAnalysis.eventName.ifBlank { newBetAnalysisLabel },
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 26.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    navigationIcon = {
                                        IconButton(onClick = closeAndDiscard) {
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
                                AdBanner()
                            }
                        }
                    ) { innerPadding ->
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .navigationBarsPadding()
                                .imePadding(),
                            contentPadding = PaddingValues(16.dp)
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
                                    onOutcomeBookmakerChange = { outcomeId, bm ->
                                        viewModel.updateOutcomeBookmaker(activeAnalysis.id, outcomeId, bm)
                                    },
                                    onOutcomeFormatChange = { outcomeId, format ->
                                        viewModel.updateOutcomeFormat(activeAnalysis.id, outcomeId, format)
                                    },
                                    onOutcomeFracNumeratorChange = { outcomeId, v ->
                                        viewModel.updateOutcomeFracNumerator(activeAnalysis.id, outcomeId, v)
                                    },
                                    onOutcomeFracDenominatorChange = { outcomeId, v ->
                                        viewModel.updateOutcomeFracDenominator(activeAnalysis.id, outcomeId, v)
                                    },
                                    onOutcomeAmericanValueChange = { outcomeId, v ->
                                        viewModel.updateOutcomeAmericanValue(activeAnalysis.id, outcomeId, v)
                                    },
                                    onOutcomeAmericanSignToggle = { outcomeId ->
                                        viewModel.toggleOutcomeAmericanSign(activeAnalysis.id, outcomeId)
                                    },
                                    onAddOutcome    = { viewModel.addOutcome(activeAnalysis.id) },
                                    onRemoveOutcome = { viewModel.removeOutcome(activeAnalysis.id, it) },
                                    onSave = {
                                        viewModel.saveToHistory(activeAnalysis.id)
                                        viewModel.deleteAnalysis(activeAnalysis.id)
                                        showForm = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showSplash,
            enter = androidx.compose.animation.fadeIn(),
            exit  = androidx.compose.animation.fadeOut()
        ) {
            SplashScreen()
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.app_name),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp
        )
    }
}

private const val PROD_AD_UNIT_ID = "ca-app-pub-2303541955549784/4795362417"
private const val TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
private val AD_UNIT_ID = if (BuildConfig.DEBUG) TEST_AD_UNIT_ID else PROD_AD_UNIT_ID

@Composable
fun AdBanner() {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = AD_UNIT_ID
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
