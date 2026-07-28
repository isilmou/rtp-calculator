# RTP Calculator — Documentation V2.0.1

> Generated 20/06/2026 from the actual source tree (read-only). This document supersedes
> `RTP_CALCULATOR_V1_DOC.md` (kept for archive). It is intended to let a developer — human or
> a future Claude session — resume the project **without** access to the source.

---

## Table of Contents
1. [App Identity](#1-app-identity)
2. [Technical Architecture](#2-technical-architecture)
3. [All Kotlin Files — Detailed Description](#3-all-kotlin-files--detailed-description)
4. [All Resource Files](#4-all-resource-files)
5. [Implemented Features](#5-implemented-features)
6. [Business Logic](#6-business-logic)
7. [Design System](#7-design-system)
8. [AdMob Configuration](#8-admob-configuration)
9. [Development Environment](#9-development-environment)
10. [Key Decisions Log — V2](#10-key-decisions-log--v2)
11. [Known Limitations V2](#11-known-limitations-v2)
12. [V3 Backlog](#12-v3-backlog)

---

## 1. App Identity

| Field | Value |
|---|---|
| App name (EN) | RTP Calculator |
| App name (FR) | Calculateur de TRJ |
| App name (ES) | Calculadora de RTP |
| App name (DE) | RTP-Rechner |
| `applicationId` / `namespace` | `com.rtpcalculator` |
| versionName / versionCode | **2.0.1 / 7** |
| minSdk | 24 |
| targetSdk / compileSdk | 35 |
| Locales | English (default), French, Spanish, German |
| Orientation / theme | Portrait, **dark only** (no light theme) |
| Platform | 100% Jetpack Compose, single Activity |
| Monetisation | AdMob banner ads |
| AdMob App ID | `ca-app-pub-2303541955549784~7410006753` (AndroidManifest) |
| AdMob banner unit (prod) | `ca-app-pub-2303541955549784/4795362417` |
| AdMob banner unit (test) | `ca-app-pub-3940256099942544/6300978111` |
| Permissions | None declared explicitly (AdMob/Play pull `INTERNET`/`AD_ID` via manifest merge) |
| Android theme | `Theme.RTPCalculator` (parent `android:Theme.Material.Light.NoActionBar`, `windowBackground` = black) |
| Compose theme | `RTPCalculatorTheme` (`darkColorScheme`, see Theme.kt) |
| Launcher icon | **Adaptive icon** (new logo): foreground `drawable/ic_launcher_foreground.xml` (vector), background `@color/ic_launcher_background` = `#0A0A0B`, `mipmap-anydpi-v26/{ic_launcher,ic_launcher_round}.xml`, PNG fallbacks mdpi→xxxhdpi (see §4.5) |
| Backup | `allowBackup=true`, custom `backup_rules.xml` + `data_extraction_rules.xml` |
| Share | FileProvider authority `${applicationId}.fileprovider`, paths in `xml/file_paths` |
| `windowSoftInputMode` | `adjustResize` (drives the scroll-into-view above the IME) |

---

## 2. Technical Architecture

### 2.1 Stack (exact versions — `gradle/libs.versions.toml`)
| Component | Version |
|---|---|
| Android Gradle Plugin (AGP) | 8.13.2 |
| Kotlin | 1.9.25 |
| Compose Compiler extension | 1.5.15 |
| Compose BOM | 2024.09.03 |
| androidx.core:core-ktx | 1.13.1 |
| lifecycle-runtime-ktx / viewmodel-compose | 2.8.3 |
| activity-compose | 1.9.1 |
| Material 3 | via Compose BOM |
| material-icons-extended | via Compose BOM |
| Gson | 2.10.1 |
| Play review / review-ktx | 2.0.2 |
| play-services-ads | 23.6.0 |
| JUnit | 4.13.2 |
| androidx.test ext junit / espresso | 1.2.1 / 3.6.1 |
| Java / JVM target | 11 |

Build features: `compose = true`, `buildConfig = true`. Release: `isMinifyEnabled = false`
(no R8/ProGuard shrinking). No `signingConfig` in Gradle (signing handled externally).

**Dependency constraint (2.0.1)**: `app/build.gradle.kts` adds a `constraints {
implementation("androidx.fragment:fragment:1.8.6") }` block. `play-services-basement`
(pulled transitively by AdMob / Play review) drags in the ancient `androidx.fragment:fragment:1.1.0`,
which triggers a Play Console deprecation warning; the constraint forces the resolved version up to
`1.8.6` without adding a direct fragment dependency (see §10).

### 2.2 Project structure
```
RTP-Calculator/
├─ build.gradle.kts                 (root)
├─ settings.gradle.kts
├─ gradle/libs.versions.toml        (version catalog)
├─ RTP_CALCULATOR_V1_DOC.md         (archive)
├─ Documentation - V2.0.1 - 20_06_2026.md   (this file)
└─ app/
   ├─ build.gradle.kts
   └─ src/
      ├─ main/
      │  ├─ AndroidManifest.xml
      │  ├─ java/com/rtpcalculator/
      │  │  ├─ MainActivity.kt
      │  │  ├─ model/BetAnalysis.kt
      │  │  ├─ data/HistoryRepository.kt
      │  │  ├─ viewmodel/BetViewModel.kt
      │  │  ├─ viewmodel/UiMessage.kt
      │  │  └─ ui/
      │  │     ├─ HistoryScreen.kt
      │  │     ├─ OddsBlock.kt
      │  │     ├─ ShareableCard.kt
      │  │     ├─ BetAnalysisCard.kt
      │  │     ├─ OutcomeFields.kt
      │  │     ├─ RtpPill.kt
      │  │     ├─ RtpGradient.kt
      │  │     ├─ Formatters.kt
      │  │     ├─ OddsConversion.kt
      │  │     ├─ HistorySorting.kt
      │  │     ├─ DateUtils.kt
      │  │     ├─ PrimaryCtaButton.kt
      │  │     ├─ ShareImage.kt
      │  │     └─ theme/{Color,Shapes,Type,Theme}.kt
      │  └─ res/ (values + values-fr/-es/-de, mipmaps, xml/, drawables)
      ├─ test/java/com/rtpcalculator/
      │  ├─ OddsConversionTest.kt
      │  └─ HistoryRetroCompatTest.kt
      └─ androidTest/java/com/rtpcalculator/  (empty after V2 cleanup)
```

### 2.3 Architectural pattern
- **Single Activity** (`MainActivity`), **single ViewModel** (`BetViewModel : AndroidViewModel`).
- **100% Compose**, **no Jetpack Navigation** — screens are toggled with `AnimatedVisibility`
  (`showForm`, `showSplash` booleans hoisted in `RtpApp`).
- **State** held in the ViewModel as `StateFlow` (`history`, `analyses`, `focusOutcomeId`) and
  `SharedFlow` (`messages`, `scrollToHistoryId`).
- **Persistence**: `SharedPreferences` + Gson (`HistoryRepository`). History **load is async**
  (Dispatchers.IO) since V2; save stays synchronous (user-action driven).
- **Clean separation obtained in Lot 3** (file split):
  - Input screen: `BetAnalysisCard.kt` (orchestration + `OutcomeRow` + date/time pickers) +
    `OutcomeFields.kt` (the field composables) + `RtpPill.kt`.
  - History screen: `HistoryScreen.kt` (list + card + header + action bar + expand toggle) +
    `OddsBlock.kt` (odds blocks + `OutcomeRows`) + `ShareableCard.kt` (off-screen share card).
  - Pure logic in dedicated files: `OddsConversion.kt`, `HistorySorting.kt`, `RtpGradient.kt`,
    `Formatters.kt`, `DateUtils.kt`.

---

## 3. All Kotlin Files — Detailed Description

### `MainActivity.kt` (com.rtpcalculator)
Application entry point and top-level Compose scaffolding.
- **`class MainActivity : ComponentActivity`** — `private val viewModel: BetViewModel by viewModels()`.
  - **`onCreate`**: `enableEdgeToEdge()`; `trackOpenCountAndRequestReview()`; **defers**
    `MobileAds.initialize(this@MainActivity)` onto `lifecycleScope.launch(Dispatchers.IO)`
    (cold-start optimisation); then `setContent { RTPCalculatorTheme { RtpApp(viewModel) } }`.
  - **`trackOpenCountAndRequestReview()`** (private): reads/writes `SharedPreferences("rtp_prefs")`
    key `open_count`; at exactly the 5th open, launches the Play in-app review flow
    (`ReviewManagerFactory`).
- **`RtpApp(viewModel: BetViewModel)`** (`@Composable`, `@OptIn(ExperimentalMaterial3Api)`):
  - State: `history by viewModel.history.collectAsState()`, `showForm`, `showSplash` (remember).
  - `LaunchedEffect(Unit){ delay(500); showSplash=false }` — the **500ms splash** (unchanged).
  - **`closeForm`** (remembered on `viewModel`): the single exit handler shared by BOTH exit
    doors. Reads `current = viewModel.analyses.value.firstOrNull()`; if `current != null` **and**
    (`current.hasMeaningfulContent()` **or** `viewModel.history.value.any { it.id == current.id }`)
    it **silently AUTO-SAVES** using the exact same path as the SAVE button
    (`saveToHistory(current.id)` then `deleteAnalysis(current.id)`); otherwise (pristine/blank,
    never-saved form) it **discards** (`deleteAnalysis`). Then `showForm = false`. The `history`
    id check guarantees a reopened entry is updated **in place** (no duplicate) even if the user
    partially cleared it, since `saveToHistory` keeps `savedAt` stable. Reading `.value` at call
    time means `remember(viewModel)` always sees current data.
  - `BackHandler(enabled = showForm, onBack = closeForm)` — Android back and the TopAppBar close
    icon (`IconButton(onClick = closeForm)`) behave identically.
  - Snackbar host + `LaunchedEffect(viewModel){ viewModel.messages.collect {...} }` mapping
    `UiMessage.ShareFailed` → `R.string.share_error`.
  - Share pipeline: `onShareAnalysis: (String)->Unit` (remembered, `isSharing` in-flight guard)
    → `renderAndShareAnalysis(activity, target)`.
  - **`onReopenAnalysis: (String)->Unit`** (remembered, stable) → clear analyses + reopen + open form.
  - Layout: `Box` containing
    1. `Scaffold { HistoryScreen(history, onReopen=onReopenAnalysis, onDelete=viewModel::deleteFromHistory,
       onDuplicate=viewModel::duplicateInHistory, onShare=onShareAnalysis, scrollToHistoryId,
       bottomContentPadding=82.dp, banner={ AdBanner() }) + PrimaryCtaButton("CREATE A BET ANALYSIS") }`.
    2. `AnimatedVisibility(showForm, slideInVertically/slideOutVertically)` → the form Scaffold:
       `topBar` = TopAppBar (event name or "New Bet Analysis", close icon) + **`AdBanner(loadAds = bannerReady)`**
       where `bannerReady` flips true after `delay(300)` (defer ad during the slide animation);
       `bottomBar` = sticky `PrimaryCtaButton("SAVE TO HISTORY")` **hidden while the IME is up**
       (`WindowInsets.ime.getBottom(...) > 0`); content = `LazyColumn` (state `formListState`,
       `CompositionLocalProvider(LocalFormScrollState, LocalFormViewportCoords)`) with a single
       `BetAnalysisCard(...)` item wired to all the VM update callbacks. The LazyColumn applies
       `.padding(innerPadding).imePadding()` (the Scaffold `innerPadding` already reserves the full
       bottomBar height = the functional clearance for the sticky button) and `contentPadding =
       PaddingValues(start/end/top = 16.dp, bottom = 8.dp)`. A redundant `.navigationBarsPadding()`
       (double-counting the nav-bar inset already consumed by the bottomBar) plus a uniform 16dp
       bottom were the cause of a tall black band above the SAVE button — fixed in V2 (see §10).
    3. `AnimatedVisibility(showSplash, fadeIn/fadeOut)` → `SplashScreen()`.
- **`SplashScreen()`**: black full-screen Box, centered `app_name` 36sp Bold white.
- **`AdBanner(loadAds: Boolean = true)`**: `AndroidView` hosting an `AdView` (BANNER, 50dp height).
  `factory` calls `loadAd` only if `loadAds`; `update` triggers `loadAd` when `loadAds` becomes true
  and no request is in-flight/served (`!adView.isLoading && adView.responseInfo == null`).
- Top-level consts: `PROD_AD_UNIT_ID`, `TEST_AD_UNIT_ID`, `AD_UNIT_ID = if (BuildConfig.DEBUG) TEST else PROD`.

### `model/BetAnalysis.kt`
Domain model + RTP computation. All data classes are `@Immutable`.
- **`enum class OddsFormat { DECIMAL, FRACTIONAL, AMERICAN }`**.
- **`data class Outcome`**: `id` (UUID), `label`, `odds` (canonical decimal string used by
  `calculateRtp`), `bookmaker`, `oddsFormat`, `fracNumerator`, `fracDenominator`,
  `americanNegative`, `americanValue`, `decimalRaw` (exact EU display string the user typed),
  `refDecimal` (decimal of the last MANUAL entry — conversion reference).
- **`data class BetAnalysis`**: `id`, `date`, `time`, `eventName`, `betType`,
  `outcomes: List<Outcome>` (default 2 empty), `savedAt: Long`.
- **`data class RtpResult`**: `rtp: Double`, `margin: Double`, `isSurebet: Boolean`.
- **`fun calculateRtp(outcomes): RtpResult?`** — see §6.1.
- **`fun BetAnalysis.hasMeaningfulContent(): Boolean`** — "generous" guard for the silent
  auto-save on form exit (see §6.9). Returns true as soon as `eventName` or `betType` is non-blank,
  or ANY outcome has a non-blank `label`/`bookmaker`/`decimalRaw`/`fracNumerator`/`fracDenominator`/
  `americanValue`, or an `odds` parsing to > 0.0 — i.e. the slightest real input in any odds format.

### `data/HistoryRepository.kt`
SharedPreferences + Gson persistence.
- **`class HistoryRepository(context)`** — prefs `"rtp_history"`, key `"saved_analyses"`, `Gson()`.
  - **`load(): List<BetAnalysis>`**: reads JSON, deserialises with `TypeToken<List<BetAnalysis>>`,
    applies `withLegacyOddsDefaults()`; returns `emptyList()` on any exception.
  - **`save(analyses)`**: `prefs.edit().putString(key, gson.toJson(...)).apply()`.
- **`List<BetAnalysis>.withLegacyOddsDefaults()`** (internal) + **`Outcome.withLegacyOddsDefaults()`**
  (private): coerce null fields from pre-format-model JSON to safe defaults (Gson skips the Kotlin
  constructor), seed `decimalRaw`/`refDecimal` from the stored decimal `odds`. (Backward compat;
  covered by `HistoryRetroCompatTest`.)

### `viewmodel/BetViewModel.kt`
`class BetViewModel(app) : AndroidViewModel`. Single source of truth.
- State: `analyses` / `_analyses`, `history` / `_history` (StateFlow), `messages` (SharedFlow,
  extraBufferCapacity 1), `scrollToHistoryId` (SharedFlow, extraBufferCapacity 4), `focusOutcomeId`
  (StateFlow).
- **`init`**: `viewModelScope.launch { val loaded = withContext(Dispatchers.IO){ repo.load() }; _history.value = loaded }`
  — **async cold-start load**, published back on the main thread.
- CRUD: `addAnalysis`, `clearAnalyses`, `deleteAnalysis`, `saveToHistory(id)`, `reopenFromHistory(id)`,
  `deleteFromHistory(id)`, `duplicateInHistory(id, newestFirst)`, `notifyShareFailed`,
  `consumeFocusOutcomeId`.
- Per-field updaters: `updateDate/Time/EventName/BetType`, `updateOutcomeLabel/Odds/Bookmaker`,
  `updateOutcomeFracNumerator/Denominator`, `updateOutcomeAmericanValue`, `toggleOutcomeAmericanSign`,
  `updateOutcomeFormat` (CONVERTs via `convertedTo`), `addOutcome` (sets `focusOutcomeId`),
  `removeOutcome` (min 2).
- Private helpers: `update`, `updateOutcome`, `Outcome.afterManualInput()` (recompute `refDecimal`
  + `odds`), `Outcome.convertedTo(target)` (convert display from `refDecimal`), `newOutcome`,
  `newAnalysis`, `lastFormat`/`saveLastFormat` (prefs `"rtp_prefs"`, key `last_odds_format`),
  top-level `BetAnalysis.withNormalizedOdds()` (recompute exact `odds` + pad EU to ≥2dp on save).
- `saveToHistory`: preserves existing `savedAt` on edit (only new entries get `now()`); emits
  `scrollToHistoryId`. `duplicateInHistory`: see §6.5.

### `viewmodel/UiMessage.kt`
**`enum class UiMessage { ShareFailed }`** — one-shot UI events (VM stays Android-string-free).

### `ui/HistoryScreen.kt`
History list screen + its card composables.
- **`HistoryScreen(history, onReopen:(String)->Unit, onDelete:(String)->Unit, onDuplicate:(String,Boolean)->Unit, onShare:(String)->Unit, scrollToHistoryId: Flow<String>, bottomContentPadding: Dp=0.dp, banner: @Composable ()->Unit={})`**
  - State: `newestFirst` (remember, default true), `sortedEntries = remember(history,newestFirst){ sortHistory(...) }`,
    `listState`, `sortedEntriesState = rememberUpdatedState(sortedEntries)`,
    `scrollTopPaddingPx = with(LocalDensity.current){16.dp.roundToPx()}`,
    `newestFirstState = rememberUpdatedState(newestFirst)`,
    **`onDuplicateCard = remember(onDuplicate){ id -> onDuplicate(id, newestFirstState.value) }`** (stable).
  - `LaunchedEffect(newestFirst)`: animate to top on sort toggle (guarded).
  - **`LaunchedEffect(scrollToHistoryId)`** scroll consumer (rewritten in 2.0.1, see §6.6): waits for
    the id to appear in the **up-to-date** sorted list (`snapshotFlow{…}.first{ it.any{id} }` — no more
    `listBefore`/`drop(1)`), settles via `withFrameNanos{} + delay(300)` then a layout-stability loop
    (≤90 frames) keyed on `(firstVisibleItemIndex, firstVisibleItemScrollOffset, target offset)`, reads
    the CURRENT index, and — judging visibility by INDEX via the `fullyVisibleRun()` helper — does
    `animateScrollToItem(index, -scrollTopPaddingPx)` with a non-animated `scrollToItem` fallback if the
    target still isn't in the fully-visible run. Fixes the no-refocus after a date edit (esp. upward moves).
  - Empty state → "no analyses yet"; else sort toggle Row + `banner()` + `LazyColumn` of `HistoryCard`
    (keyed by id, `Modifier.animateItem()`, **stable callbacks passed directly**).
- **`HistoryCard(analysis, onReopen:(String)->Unit, onDelete, onDuplicate:(String)->Unit, onShare, modifier)`**
  (private, **skippable** — id-based stable callbacks; per-id binding done internally):
  `rtpResult = remember(analysis.outcomes){ calculateRtp(...) }`; `overflow = outcomes.size>3`;
  `expanded` (rememberSaveable); `sideBarColor = rtpGradientColor(rtp) ?: Color.Transparent`.
  Layout: `Card`(clickable→onReopen(id)) → `Row(IntrinsicSize.Min)` → 3dp side bar + content Column
  (padding 9/5) → `CardHeader` + `Spacer(5)` + `OutcomeRows` (collapsed `fillIncompleteRows=false`,
  expanded `=true` with `animateContentSize` + `ExpandToggle`) + `Spacer(4)` + `HorizontalDivider` +
  `Spacer(2)` + `CardActionBar`.
- **`CardHeader(eventName, date, time, betType, rtpResult)`** (internal): line 1 = metadata (date·time·betType,
  10sp) + `RtpPill` right; line 2 = event name (13sp Bold, centered, conditional, `Spacer(4)` above).
- **`CardActionBar(onDuplicate, onShare, onDelete)`** (private): right-aligned Duplicate + Share
  (`CardAction`) + standalone red trash `IconButton(32dp)`.
- **`CardAction(icon, label, onClick)`** (private): clip+clickable Row, 14dp icon + 11sp label, `ActionIcon`.
- **`ExpandToggle(expanded, extraCount, onClick)`** (private): centered pill, "+N"/"Show less",
  10sp Medium, `heightIn(min=22.dp)`, padding 8/3, `FieldBackground`.

### `ui/OddsBlock.kt`
Odds rendering shared by the on-screen card and the share card.
- **`OutcomeRows(outcomes: List<Outcome>, rowSpacing: Dp, fillIncompleteRows: Boolean, modifier)`**
  (internal): `Column(spacedBy(rowSpacing))` of `outcomes.chunked(3)` rows; per-row flags
  `rowHasAnyLabel`/`rowHasAnyBookmaker`; each row a `Row(spacedBy(rowSpacing))` of `OddsBlock`
  (weight 1f); if `fillIncompleteRows`, pads the trailing row with `Spacer(weight 1f)` cells.
  (`rowSpacing` = 4.dp on screen, 3.dp on the shared PNG.)
- **`OddsBlock(label, oddsText, bookmaker, modifier, reserveLabelSpace=true, reserveBookmakerSpace=true)`**
  (internal): `Surface(OutcomeBlockBg)` → Column(padding 2h/1v) → optional label Text (9sp,
  `lineHeight=9.sp`, minLines=1) + cote frame `Surface(AppBackground)` Box(padding v=1) →
  `AutoSizeOddsText` + optional bookmaker Text (8sp, `lineHeight=8.sp`, minLines=1). The reserve
  flags are computed per ROW by the caller (see §5 card).
- **`AutoSizeOddsText(text, modifier, maxFontSize=16.sp, minFontSize=11.sp)`** (private):
  `Box(heightIn(min=20.dp))`, single-line Text, `fontSize` state starts at 16sp and steps down 1sp
  on `onTextLayout` overflow down to 11sp (OddsYellow, ExtraBold).

### `ui/ShareableCard.kt`
Off-screen card rendered to the shared PNG.
- **`ShareableHistoryCard(analysis: BetAnalysis)`** (public): same Card+side-bar structure as
  `HistoryCard` but tighter padding (8/5), always expanded, no action bar; `CardHeader` +
  `OutcomeRows(rowSpacing=3.dp, fillIncompleteRows = analysis.outcomes.size > 3)` + `Spacer(6.dp)` +
  `ShareSignature()`. The `> 3` guard (2.0.1) drops the phantom empty column for ≤3 outcomes so the
  share image matches the on-screen card (2 issues → 50/50, 3 → 33% each), instead of always padding.
- **`ShareSignature()`** (private): a thin top rule — `HorizontalDivider(thickness = 0.5.dp,
  color = DividerSubtle)` (between 6dp/8dp spacers) — then a Row with the app icon (the new adaptive
  launcher logo, `R.mipmap.ic_launcher` rasterised to a 72px ImageBitmap via
  `ContextCompat.getDrawable(...).toBitmap(72,72).asImageBitmap()`, shown 24dp, `clip(AppShapeSmall)`)
  + a single-line `buildAnnotatedString` (10sp, `maxLines=1`, ellipsis): `app_name` in **TextPrimary
  Bold**, a `" · "` separator in TextTertiary, then `share_signature_available_on` in **TextTertiary
  Italic**. `painterResource` can't be used here (API ≥ 26 resolves the adaptive-icon XML, unsupported
  by Compose's loader) — hence the platform-drawable rasterisation.

### `ui/BetAnalysisCard.kt`
Input (saisie) screen orchestration. `@file:OptIn(ExperimentalMaterial3Api, ExperimentalFoundationApi)`.
- Private dimension tokens: `OutcomeFieldHeight=36.dp`, `OutcomeLineTwoHeight=32.dp`,
  `OutcomeRemoveSize=32.dp`, `OutcomeRowSpacing=5.dp`; **internal** `OutcomeFieldContentInset=8.dp`.
- **`internal val LocalFormScrollState: CompositionLocal<LazyListState?>`** /
  **`LocalFormViewportCoords: CompositionLocal<State<LayoutCoordinates?>?>`** — plumbed by MainActivity.
- **`internal fun Modifier.scrollIntoViewOnFocus()`**: on focus (`hasFocus`), after `delay(300)`,
  drives `LazyListState.animateScrollBy` so the focused field's bottom sits 1.2×field-height above
  the IME-shrunk viewport bottom.
- **`internal fun Modifier.selectAllOnFirstFocus(tfv, onFocusLost)`**: select-all on first focus
  (via a `LaunchedEffect(focused)` to land after the IME's own selection); runs `onFocusLost` on blur.
- **`BetAnalysisCard(analysis, onDateChange, onTimeChange, onEventNameChange, onBetTypeChange,
  onOutcomeLabelChange, onOutcomeOddsChange, onOutcomeFracNumeratorChange, onOutcomeFracDenominatorChange,
  onOutcomeAmericanValueChange, onOutcomeAmericanSignToggle, onOutcomeBookmakerChange, onOutcomeFormatChange,
  onAddOutcome, onRemoveOutcome, focusOutcomeId, onFocusOutcomeConsumed, modifier)`** (public):
  Material3 date/time pickers (state-driven); `rtpResult = remember(outcomes){ calculateRtp }`;
  `allOddsFilled = remember(outcomes){ outcomes.all { odds.toDoubleOrNull() > 0 } }`. Column of:
  event `SectionCard` (name + date/time row + bet type), one `SectionCard` per outcome
  (`OutcomeRow`, keyed), and a bottom Row = **`RtpPill`** (only if `allOddsFilled && rtpResult!=null`,
  else a 1dp Spacer) + "Add outcome" pill.
- Private composables: **`SectionCard`** (CardBg surface, padding 10/8), **`DateClickField`** /
  **`TimeClickField`** (read-only, open pickers; time disabled until a date is set),
  **`appDatePickerColors`** / **`appTimePickerColors`** (white-selection dark palette),
  **`OutcomeRow(outcome, canRemove, autoFocusIssue, onAutoFocusConsumed, …)`** (two lines:
  Issue+Book on line 1; EU/UK/US selector + cote field + trash on line 2; `scrollIntoViewOnFocus`
  on the outer Column; auto-focus via `LaunchedEffect(autoFocusIssue)`).
- Private: `parseTimeOrNull(time)`, `fieldColors()`.

### `ui/OutcomeFields.kt`
Field composables extracted from `BetAnalysisCard` (Lot 3). Private dim token `OddsFormatSegmentWidth=36.dp`.
- **`OddsFormatSelector(selected, onSelected, modifier)`** (internal): 3 side-by-side EU/UK/US
  segments; active = `FormatActiveSurface`/`FormatActiveText`, inactive = grayscale; 10sp.
  + private `OddsFormat.shortLabelRes()`.
- **`CompactField(value, onValueChange, label, modifier, keyboardOptions, textAlign, textColor,
  textFontSize, textFontWeight, focusRequester, scrollIntoViewOnFocus, valueTransform, onFocusLost)`**
  (internal): placeholder-inside `BasicTextField` in a `FieldBackground` box; used for event name,
  bet type, issue label, bookmaker, and the DECIMAL cote. Local `tfv` state synced via `LaunchedEffect(value)`.
- **`FractionalField(numerator, denominator, onNumeratorChange, onDenominatorChange, modifier)`**
  (internal): "num / den" digit fields, OddsYellow ExtraBold.
- **`AmericanField(value, negative, onValueChange, onToggleSign, label, modifier)`** (internal):
  tappable +/- sign + digit field.

### `ui/RtpPill.kt`
- **`RtpPill(rtp: Double, modifier: Modifier = Modifier)`** (public): solid pill, background
  `rtpGradientColor(rtp)`, text "`rtp_short` X.X%" (12sp ExtraBold, `CtaOnSurface`, padding 8/3,
  `AppShapeSmall`). Shared by the history card header and the input screen's live pill.

### `ui/RtpGradient.kt`
- **`fun rtpGradientColor(rtp: Double): Color`** — 4-stop interpolation (see §6.3).

### `ui/Formatters.kt`
- **`normalizeOddsOrNull(raw): String?`** — comma→dot, parse, `"%.2f"` or null.
- **`formatOddsTwoDecimals(raw): String`** — normalize or echo raw.
  (Note: `formatOddsForDisplay` was removed in Lot 1.)

### `ui/OddsConversion.kt`
Pure odds-conversion helpers (heavily unit-tested).
- **`convertToDecimalOdds(format, decimalRaw, fracNumerator, fracDenominator, americanNegative, americanValue): String?`**
- **`Outcome.displayOdds(): String`** — format-faithful display (FRACTIONAL "n/d", AMERICAN "+/-v",
  else decimal via private `decimalDisplay()`).
- **`Outcome.toDecimalOddsOrNull(): String?`** — convenience overload (test-only in production).
- **`rawToDecimalExactOrNull(...): Double?`** — full-precision decimal of raw input.
- **`decimalToAmerican(decimal): Pair<Boolean,String>?`**, **`decimalToFraction(decimal): Pair<String,String>?`**.
- **`formatDecimalUpTo3(decimal): String`**, **`exactDecimalString(decimal): String`**,
  **`padDecimalToMinTwo(raw): String`**, **`sanitizeDecimalInput(raw): String`**, private `gcd`.

### `ui/HistorySorting.kt`
- **`fun sortHistory(history, newestFirst): List<BetAnalysis>`** — see §6.4.

### `ui/DateUtils.kt`
- **`parseDateToMillis(date): Long?`** — tries 9 date formats (lenient=false).
- **`parseDateTimeToMillis(date, time): Long?`** — adds HH:mm to the date millis.

### `ui/PrimaryCtaButton.kt`
- **`PrimaryCtaButton(text, onClick, modifier)`** (public): off-white `CtaSurface` button,
  `CtaOnSurface` text, 46dp height, `AppShapeSmall`, 13sp Bold + 0.5sp letter spacing. Shared by
  "CREATE A BET ANALYSIS" and "SAVE TO HISTORY".

### `ui/ShareImage.kt`
Renders `ShareableHistoryCard` to a **rectangular** PNG (the card's exact natural size) and launches
the share sheet.
- Consts: `SHARE_IMAGE_WIDTH_PX=1080`, `SHARE_IMAGE_DENSITY=3.0f`, `SHARES_SUBDIR="shares"`,
  `FILE_PROVIDER_AUTHORITY_SUFFIX=".fileprovider"`, `SHARE_CACHE_MAX_AGE_MS=6h`,
  `MAX_CAPTURE_HEIGHT_PX=10_000`.
- **`suspend renderAndShareAnalysis(activity, analysis)`**: renders `ShareableCardCanvas(analysis)`
  to a bitmap, writes a PNG to the cache, launches the chooser.
- **`ShareableCardCanvas(analysis)`** (private): `Box(background(AppBackground).padding(12.dp)){ ShareableHistoryCard }`
  (tightened from 16dp → **12.dp** in V2).
- **`renderComposableToBitmap(activity, widthPx, densityValue, content)`**: off-screen fixed-density
  `ComposeView`; re-measures with an **UNSPECIFIED** height to recover the card's true natural height
  (the parent `AT_MOST` spec would otherwise clamp tall cards and truncate the signature), then
  captures a bitmap at the card's **exact `widthPx × naturalHeight`** — no square compositing, no
  background bands (the square `max(w,h)` canvas was removed in V2; see §10 / §5 share).
- Private I/O helpers: `writePngToShareCache`, `cleanOldShares`, `launchShareIntent`.

### `ui/theme/*`
- **`Color.kt`** — all palette tokens (see §4.2).
- **`Shapes.kt`** — `AppCornerLarge=8.dp`, `AppCornerSmall=6.dp`, `AppShapeLarge`, `AppShapeSmall`.
- **`Type.kt`** — `Typography` with `bodyLarge` (FontFamily.Default, 16sp, lineHeight 24sp,
  letterSpacing 0.5sp). No custom font family.
- **`Theme.kt`** — `RTPCalculatorTheme` wrapping `MaterialTheme(darkColorScheme(...), Typography)`.

### Test files (`app/src/test`)
- **`OddsConversionTest.kt`** — conversion + `displayOdds` coverage.
- **`HistoryRetroCompatTest.kt`** — `withLegacyOddsDefaults` backward-compat.

---

## 4. All Resource Files

### 4.1 strings.xml — 35 keys (after Lot 1 removed 18 dead keys; identical key set across 4 locales)
| Key | EN | FR | ES | DE |
|---|---|---|---|---|
| app_name | RTP Calculator | Calculateur de TRJ | Calculadora de RTP | RTP-Rechner |
| create_bet_analysis | CREATE A BET ANALYSIS | CRÉER UNE ANALYSE DE PARI | CREAR UN ANÁLISIS DE APUESTA | WETTANALYSE ERSTELLEN |
| new_bet_analysis | New Bet Analysis | Nouvelle analyse de pari | Nuevo análisis de apuesta | Neue Wettanalyse |
| event_name_optional | Event Name (optional) | Nom évènement (facultatif) | Nombre del evento (opcional) | Event-Name (optional) |
| event_date_ph | Date (optional) | Date (facultatif) | Fecha (opcional) | Datum (optional) |
| event_time_ph | Time (optional) | Heure (facultatif) | Hora (opcional) | Uhrzeit (optional) |
| bet_type_placeholder | Bet type (optional): 1X2, … | Type de pari (facultatif) : 1X2, … | Tipo de apuesta (opcional): 1X2, … | Wettart (optional): 1X2, … |
| outcome | Outcome | Issue | Resultado | Tipp |
| odds | Odds | Cote | Cuota | Quote |
| book | Book | Book | Casa | Buki |
| odds_format_decimal_short | EU | EU | EU | EU |
| odds_format_fractional_short | UK | UK | UK | UK |
| odds_format_american_short | US | US | US | US |
| add_outcome | Add Outcome | Ajouter une issue | Añadir resultado | Tipp hinzufügen |
| save_to_history | SAVE TO HISTORY | ENREGISTRER | GUARDAR | SPEICHERN |
| rtp_return_to_player | RTP (Return to Player) | TRJ (Taux Retour Joueur) | RTP (Retorno al Jugador) | RTP (Auszahlungsquote) |
| books_margin | Book's Margin | Marge Bookmakers | Margen de la casa | Marge des Buchmachers |
| sort_by_event_date | Sort by event date | Trier par date d'évènement | Ordenar por fecha del evento | Nach Event-Datum sortieren |
| no_analyses_yet | No analyses yet. Tap below… | Aucune analyse. Appuyez… | Aún no hay análisis… | Noch keine Analysen… |
| action_ok | OK | OK | OK | OK |
| action_cancel | Cancel | Annuler | Cancelar | Abbrechen |
| cd_close | Close | Fermer | Cerrar | Schließen |
| cd_clear_date | Clear date | Effacer la date | Borrar la fecha | Datum löschen |
| cd_pick_date | Pick date | Choisir une date | Elegir una fecha | Datum wählen |
| cd_clear_time | Clear time | Effacer l'heure | Borrar la hora | Uhrzeit löschen |
| cd_pick_time | Pick time | Choisir une heure | Elegir una hora | Uhrzeit wählen |
| duplicate | Duplicate | Dupliquer | Duplicar | Duplizieren |
| share | Share | Partager | Compartir | Teilen |
| rtp_short | RTP | TRJ | RTP | RTP |
| show_less | Show less | Voir moins | Mostrar menos | Weniger anzeigen |
| share_signature_available_on | Available on Google Play | Disponible sur le Play Store | Disponible en Google Play | Verfügbar bei Google Play |
| share_error | Couldn't generate share image | Impossible de générer l'image | No se pudo generar la imagen | Bild konnte nicht erstellt werden |
| cd_remove_outcome | Remove | Supprimer | Eliminar | Entfernen |
| cd_delete | Delete | Supprimer | Eliminar | Löschen |
| cd_toggle_sort_order | Toggle sort order | Inverser l'ordre de tri | Invertir el orden de clasificación | Sortierreihenfolge umkehren |

> Note: `fill_odds_to_calculate_rtp` was kept in V1 audit but **removed** in Lot 1 along with 17
> other unused keys (`new_analysis`, `bet_type_optional`, `*_title`, `tap_to_pick_*`, `outcomes`,
> `odds_format_decimal/fractional/american` long forms, `cd_select_odds_format`, `no_date`,
> `unnamed`, `margin_label`, `duplicate_toast`).

### 4.2 Color.kt — full palette (dark, "sportive-moderne")
| Token | Hex | Role |
|---|---|---|
| AppBackground | `#0A0A0A` | app-wide background (near-black); also input field bg |
| CardBg | `#141414` | section / history cards |
| FieldBackground | `#0A0A0A` | input fields (= AppBackground) |
| OutcomeBlockBg | `#1E1E1E` | per-issue odds-block surface (slight lift above CardBg) |
| DarkSurface | `#141414` | Material3 surface for dialogs/pickers |
| OddsYellow | `#FFD600` | odds value accent |
| RtpRed | `#E53935` | TRJ gradient stop (lowest) |
| RtpOrange | `#FB8C00` | TRJ gradient stop |
| RtpYellow | `#FDD835` | TRJ gradient stop |
| RtpGreen | `#43A047` | TRJ gradient stop (highest) |
| TextPrimary | `#FFFFFF` | event name, dominant values |
| TextSecondary | `#AAAAAA` | metadata, labels |
| TextTertiary | `#666666` | micro-info (bookmaker) |
| CtaSurface | `#FAFAFA` | CREATE/SAVE button bg (off-white) |
| CtaOnSurface | `#0A0A0A` | CTA text / pill text |
| ActionIcon | `#BBBBBB` | Duplicate/Share icons |
| DangerRed | `#EF5350` | trash icon / error |
| FormatActiveSurface | `#E8E8E8` | active EU/UK/US segment bg |
| FormatActiveText | `#0A0A0A` | active segment text |
| FormatInactiveSurface | `#1F1F1F` | inactive segment bg |
| FormatInactiveText | `#888888` | inactive segment text |
| DividerSubtle | `#1F1F1F` | dividers / outline |
| FieldFocus | `#FFFFFF` | cursor + focused border |
| OverlayWhite30 | `#4DFFFFFF` | ~30% white overlay (RtpDisplay-era divider; trash-disabled tint) |

### 4.3 Shapes.kt
- `AppCornerLarge = 8.dp` → `AppShapeLarge` (cards, dialogs).
- `AppCornerSmall = 6.dp` → `AppShapeSmall` (fields, pills/badges, CTA buttons, format selector, sign toggle).

### 4.4 Type.kt
`Typography.bodyLarge` = FontFamily.Default, Normal, 16sp, lineHeight 24sp, letterSpacing 0.5sp.
All other text styles use ad-hoc `fontSize`/`fontWeight` per composable (see §7.2). **No custom font.**

### 4.5 Other resources
- **Launcher icon (new V2 logo, adaptive)**:
  - `drawable/ic_launcher_foreground.xml` — `VectorDrawable` (108×108 viewport); the artwork sits in
    a `<group>` scaled `0.65` (`pivotX/Y = 50`, `translateX/Y = 4`) so it stays inside the adaptive-icon
    visible/safe zone (~72/108) — tightened from 0.85 in 2.0.1 to stop the masked launcher shapes from
    clipping the ring. Paths: grey base ring `strokeColor #3A3B40` (width 16), the value arc
    `strokeColor #FFD600` (the odds yellow, width 16, `strokeLineCap="butt"` — changed from a darker tone
    to the odds yellow in 2.0.1), and two thin tick lines `strokeColor #0A0A0B` (width 4).
  - `values/colors.xml` → `<color name="ic_launcher_background">#0A0A0B</color>` (the adaptive background).
  - `mipmap-anydpi-v26/ic_launcher.xml` **and** `ic_launcher_round.xml` — both `<adaptive-icon>` with
    `background=@color/ic_launcher_background` + `foreground=@drawable/ic_launcher_foreground`.
  - PNG fallbacks (`ic_launcher.png` + `ic_launcher_round.png`) for API < 26 across mdpi→xxxhdpi.
  - The same logo is rasterised at runtime for the share signature (see §3 `ShareSignature`).
- `xml/file_paths.xml` (FileProvider paths for the `shares/` cache), `xml/backup_rules.xml`,
  `xml/data_extraction_rules.xml`.
- `values/themes.xml`: `Theme.RTPCalculator` (Material.Light.NoActionBar parent, black windowBackground).

---

## 5. Implemented Features

- **Splash (500ms)**: black screen with the app name (36sp Bold). `delay(500)` then fades out.
  Deliberately preserved for brand identity.
- **History screen**: sort toggle ("Sort by event date", 12sp + chevron, ≥36dp tap target),
  optional ad banner, `LazyColumn` of cards, sticky "CREATE A BET ANALYSIS" CTA at the bottom.
  Empty state shows "No analyses yet".
- **History card**: colored 3dp vertical side bar (TRJ color, transparent if no RTP); header line 1
  = "date · time · betType" (left) + RTP pill (right); line 2 = centered event name (only if set);
  odds blocks via `OutcomeRows`; **per-row reservation** (`reserveLabelSpace`/`reserveBookmakerSpace`):
  a row lays out the label/bookmaker line only if ANY outcome in that row uses it, so empty rows
  collapse while mixed rows stay column-aligned. >3 outcomes → "+N" / "Show less" toggle
  (`animateContentSize`). Bottom: divider + action bar (Duplicate, Share, Delete). Tap card = reopen.
- **Input (saisie) screen**: TopAppBar (title = event name or "New Bet Analysis", close button) +
  ad banner (loaded after 300ms); event `SectionCard` (name; date+time read-only pickers; bet type);
  one `SectionCard` per outcome — line 1 Issue + Book, line 2 EU/UK/US selector + cote + trash;
  bottom Row = **live RTP pill** (only when every odds is filled) + "Add outcome"; sticky "SAVE TO
  HISTORY" button (hidden while the keyboard is open). Auto-focus + scroll-into-view of newly added
  / focused fields above the IME.
- **Auto-save on form exit (V2)**: leaving the form via the Android back button OR the TopAppBar
  close icon no longer discards the work. If the analysis has any real content
  (`hasMeaningfulContent()`, §6.9) — or was reopened from an existing history entry — it is
  **silently saved/updated** in history (same path as the SAVE button; a reopened entry is updated
  in place, never duplicated). A pristine/blank form is silently discarded. No dialog, no snackbar,
  no confirm-on-discard.
- **EU/UK/US conversion**: switching the format CONVERTS the value from `refDecimal` (last manual
  entry), never from the displayed value (no rounding drift). `displayOdds()` shows the value in its
  entered format; the canonical decimal `odds` always feeds the TRJ.
- **TRJ display**: at the input screen, the pill appears only when ALL outcomes have a valid (>0)
  odds; in the history card, `calculateRtp` returns a value as soon as ≥2 valid odds exist.
- **TRJ color**: red→orange→yellow→green gradient by value (thresholds in §6.3).
- **Duplication**: fresh UUIDs (analysis + each outcome), full content copy; `savedAt` computed so
  the copy lands JUST BELOW the original in the CURRENT sort mode (see §6.5); appended to history +
  persisted; emits `scrollToHistoryId`.
- **Deletion**: trash tap = immediate delete + persist (no confirmation, no undo).
- **Share**: Compose-renders `ShareableHistoryCard` off-screen at 1080px/density-3 and captures it at
  the card's **exact natural size** (rectangular `width × height`, 12dp breathing margin — no square
  compositing/bands), writes a PNG to the `shares/` cache, and launches `ACTION_SEND` (image/png) with
  `ClipData` + FileProvider URI. The share card pads incomplete rows only for **>3** outcomes
  (`fillIncompleteRows = outcomes.size > 3`), so a 2- or 3-issue card fills the width like the app
  (no phantom empty column). The system share-sheet preview may crop the rectangular image in some
  apps (accepted trade-off; the delivered image is always intact — see §10/§11).
- **Auto-scroll**: on create/update/duplicate the VM emits `scrollToHistoryId`; the screen waits for
  the re-sorted list, then scrolls the item into view with a 16dp top padding only if it isn't
  already fully visible (see §6.6).
- **Persistence**: SharedPreferences `"rtp_history"` + Gson; **async load** (Dispatchers.IO) on VM init.
- **In-app review**: Play Core, triggered on the 5th app open.
- **Localisation**: EN / FR / ES / DE.
- **AdMob**: history banner loads immediately on screen show; form banner loads after a 300ms delay
  to keep the open animation smooth; SDK init deferred off the main thread.

---

## 6. Business Logic

### 6.1 RTP formula (`calculateRtp`, BetAnalysis.kt)
```
odds = outcomes.mapNotNull { it.odds.toDoubleOrNull() }.filter { it > 0 }
if (odds.size < 2) return null
impliedProbSum = Σ (1 / o)
if (impliedProbSum <= 0) return null
rtp    = (1 / impliedProbSum) * 100
margin = 100 - rtp
isSurebet = rtp > 100
```
Returns null until ≥2 valid odds exist.

### 6.2 Surebet
`RtpResult.isSurebet = rtp > 100.0`. Visual: at the detailed display level the RTP text was prefixed
with "⭐" — but that detailed view (`RtpDisplay`) was **removed in V2**; currently surebet is only
reflected by the gradient saturating to green (RTP ≥ 95 → `RtpGreen`). No dedicated surebet badge today.

### 6.3 TRJ gradient (`rtpGradientColor`)
```
rtp < 78          -> RtpRed
78 ≤ rtp < 87     -> lerp(RtpRed,    RtpOrange, (rtp-78)/9)
87 ≤ rtp < 92     -> lerp(RtpOrange, RtpYellow, (rtp-87)/5)
92 ≤ rtp < 95     -> lerp(RtpYellow, RtpGreen,  (rtp-92)/3)
rtp ≥ 95          -> RtpGreen
```

### 6.4 Sort (`sortHistory(history, newestFirst)`)
Partition into `noDate` (date blank) and `withDate`.
- **No-date group** mirrors with the toggle: `newestFirst` → savedAt **DESC**; `oldestFirst` → savedAt **ASC**.
- **newestFirst**: `withDate` by event date+time **DESC**, then savedAt **ASC** → result =
  `sortedWithDate + sortedNoDate` (dated on top, no-date below).
- **oldestFirst**: `withDate` by date+time **ASC**, then savedAt **DESC** → result =
  `sortedNoDate + sortedWithDate` (no-date on top, dated below).
- Date+time parsed by `parseDateTimeToMillis`; unparseable dates fall to `Long.MIN_VALUE`
  (newestFirst) / `Long.MAX_VALUE` (oldestFirst).

### 6.5 Duplication savedAt (`duplicateInHistory(id, newestFirst)`)
Group = analyses with the same `date` string. "Below the original on screen" maps to a smaller or
larger `savedAt` depending on the group's displayed direction:
```
seekSmaller = (source.date.isBlank() == newestFirst)
if (seekSmaller) {
    w = group.filter { savedAt < source.savedAt }.maxOrNull()
    copySavedAt = if (w != null) (source.savedAt + w) / 2 else source.savedAt - 1
} else {
    v = group.filter { savedAt > source.savedAt }.minOrNull()
    copySavedAt = if (v != null) (source.savedAt + v) / 2 else System.currentTimeMillis()
}
```
| group | newestFirst | oldestFirst |
|---|---|---|
| dated | seek larger | seek smaller |
| no-date | seek smaller | seek larger |

Midpoint placement makes repeated duplications stack right under the original. The target is computed
for the CURRENT sort mode; flipping the sort afterward may flip the relative position (accepted).

### 6.6 Auto-scroll (`HistoryScreen` scroll consumer — rewritten in 2.0.1)
The old `listBefore` + `.drop(1)` "wait for a different list instance" approach **broke on date edits**:
`saveToHistory` sets `_history.value` BEFORE emitting the id, so the re-sort had often already landed by
the time the collector ran — the wait for a `!==` instance then never resolved and the moved card was
never followed (no refocus, worst when the card moved upward). The rewrite drops that snapshot
comparison entirely:
```
// 1) wait until the id is in the UP-TO-DATE sorted list (a brand-new entry's id appears
//    only after the recomposition runs).
snapshotFlow { sortedEntriesState.value }.first { list -> list.any { it.id == id } }

// 2) settle: one frame + delay(300) to outlast the form's slide-out and let the re-sort
//    recomposition + LazyColumn relayout begin.
withFrameNanos { }; delay(300)

// 3) wait for REAL layout stability (Modifier.animateItem() keeps reporting an interpolated,
//    still-in-viewport offset for a card gliding to its new slot). Loop ≤90 frames on the
//    signature (firstVisibleItemIndex, firstVisibleItemScrollOffset, target's reported offset);
//    break once it stops changing.
var lastSig; var settleFrames = 0
while (settleFrames < 90) { withFrameNanos{}; sig = Triple(idx, off, targetOffset)
                            if (sig == lastSig) break; lastSig = sig; settleFrames++ }

// 4) read the CURRENT index; bail if gone.
targetIndex = sortedEntriesState.value.indexOfFirst { it.id == id }; if (targetIndex < 0) return

// 5) judge visibility by INDEX (animation-proof), never by visibleItemsInfo offsets, via the
//    fullyVisibleRun() helper: build the contiguous run of fully-visible indices starting at
//    firstVisibleItemIndex (skip it if its top edge is clipped). Target below/above the run = off-screen.
if (targetIndex !in fullyVisibleRun()) {
    animateScrollToItem(targetIndex, -scrollTopPaddingPx)        // smooth glide, 16dp top padding
    if (targetIndex !in fullyVisibleRun()) scrollToItem(targetIndex, -scrollTopPaddingPx)  // hard snap fallback
}
```
Why INDEX, not offsets: under `Modifier.animateItem()` a card's reported `offset` is its *animated*
visual position, not its logical one (logs showed `firstVisibleIndex=5` yet the target reported
`offset=110`), which fooled the old offset-based "fully visible" guard. The scroll anchor
`firstVisibleItemIndex` is authoritative; `fullyVisibleRun()` derives the visible window from it.

### 6.7 Auto-conversion (EU/UK/US)
- `decimalRaw` = the EU string the user typed (trailing zeros preserved, sanitised to ≤3 decimals).
- `refDecimal` = exact decimal of the last MANUAL entry (the conversion reference).
- `odds` = high-precision decimal feeding `calculateRtp` (via `exactDecimalString`).
- On manual entry → `afterManualInput()` recomputes `refDecimal` + `odds` (or clears both if invalid).
- On format change → `convertedTo(target)` rebuilds the new format's raw fields from `refDecimal`.
- Display: `formatDecimalUpTo3` (≤3 decimals, 3rd only if significant); `padDecimalToMinTwo` pads EU
  display to ≥2 decimals at blur/save/reopen.

### 6.8 Persistence & migration
Gson serialises `List<BetAnalysis>`. `withLegacyOddsDefaults()` repairs pre-format-model entries
(null `oddsFormat`/raw fields), seeding `decimalRaw`/`refDecimal` from the stored decimal `odds`.
`withNormalizedOdds()` (on save) recomputes exact `odds` and pads the EU display.

### 6.9 Auto-save guard (`BetAnalysis.hasMeaningfulContent()`)
Decides whether leaving the form auto-saves or discards. Intentionally **generous** — the slightest
real input keeps the analysis:
```
hasMeaningfulContent() =
    eventName.isNotBlank() || betType.isNotBlank() ||
    outcomes.any {
        label.isNotBlank() || bookmaker.isNotBlank() ||
        decimalRaw.isNotBlank() || fracNumerator.isNotBlank() ||
        fracDenominator.isNotBlank() || americanValue.isNotBlank() ||
        (odds.toDoubleOrNull()?.let { it > 0.0 } == true)
    }
```
`closeForm` saves when `hasMeaningfulContent()` **OR** the id already exists in `history` (so a
reopened-then-emptied entry is still updated in place, not lost); otherwise it discards. `saveToHistory`
keeps `savedAt` stable, so no duplicate is created on re-exit.

---

## 7. Design System

### 7.1 Charte
"Sportive modern", bookmaker-inspired dark UI. Yellow odds accent, off-white CTAs, grayscale format
pills, a 4-stop TRJ gradient used both for the RTP pill and the card's left side bar.

### 7.2 Typography (per-element sizes)
| Element | Size / weight / color |
|---|---|
| Event name (card line 2) | 13sp Bold, TextPrimary |
| Cote (odds) | up to 16sp ExtraBold, OddsYellow (auto-shrinks to 11sp) |
| RTP pill | 12sp ExtraBold, CtaOnSurface |
| Issue label | 9sp, TextSecondary, lineHeight 9sp |
| Bookmaker | 8sp, TextTertiary, lineHeight 8sp |
| Metadata (card line 1) | 10sp, TextSecondary |
| Action bar labels | 11sp Normal, ActionIcon (icons 14dp) |
| Sort toggle | 12sp (labelMedium), TextSecondary (chevron 15dp) |
| Expand toggle (+N) | 10sp Medium, TextSecondary |
| EU/UK/US segment | 10sp (Bold active / Normal inactive) |
| CTA buttons | 13sp Bold, 0.5sp letter spacing, CtaOnSurface |
| Splash | 36sp Bold, white |
| TopAppBar title | 15sp Bold, TextPrimary |
| Share signature | 10sp single line — app name **TextPrimary Bold** + `" · "` (TextTertiary) + tagline **TextTertiary Italic**; preceded by a 0.5dp `DividerSubtle` rule |

### 7.3 Shapes
`AppShapeLarge` (8dp): cards, dialogs. `AppShapeSmall` (6dp): fields, pills, CTA buttons, selector, sign toggle.

### 7.4 Key spacings
- History card content padding: 9dp h / 5dp v. Share card: 8dp h / 5dp v.
- Side bar width: 3dp. Inter-card spacing: 6dp. List content padding: 12dp h, 8dp v (+ bottom 82dp CTA inset).
- Header line1↔line2 spacer: 4dp. Header↔odds: 5dp. Odds↔divider: 4dp. Divider↔action bar: 2dp.
- Odds block: column padding 2h/1v; cote frame inner padding 1dp v; `OutcomeRows` rowSpacing 4dp (screen) / 3dp (share).
- Outcome field heights: line 1 = 36dp, line 2 = 32dp; field content inset 8dp; format segment width 36dp.
- CTA button height 46dp. Ad banner height 50dp.
- Form `LazyColumn` content padding: 16dp (start/end/top), **8dp bottom** (small intentional margin;
  the functional clearance above the sticky SAVE button comes from the Scaffold `innerPadding`). The
  earlier tall black band came from a redundant `.navigationBarsPadding()` + a uniform 16dp bottom
  stacked on top of `innerPadding` — removed in V2 (see §10).
- Share canvas (`ShareableCardCanvas`) outer padding: **12dp** (tightened from 16dp in V2).

### 7.5 Reusable components (post-factorisation)
`PrimaryCtaButton`, `RtpPill`, `OutcomeRows` (+ `OddsBlock`/`AutoSizeOddsText`), `CompactField`/
`FractionalField`/`AmericanField`/`OddsFormatSelector` (OutcomeFields), `ShareableHistoryCard`.

---

## 8. AdMob Configuration
- **App ID** (manifest meta-data): `ca-app-pub-2303541955549784~7410006753`.
- **Banner unit (prod)**: `ca-app-pub-2303541955549784/4795362417`.
- **Banner unit (test)**: `ca-app-pub-3940256099942544/6300978111`.
- Selection: `AD_UNIT_ID = if (BuildConfig.DEBUG) TEST_AD_UNIT_ID else PROD_AD_UNIT_ID`.
- **V2 specifics**: `MobileAds.initialize` runs on `Dispatchers.IO` (deferred off the first frame);
  the **form** banner defers `loadAd` by ~300ms (`bannerReady`) to keep the open animation smooth;
  the **history** banner loads immediately (`AdBanner()` default `loadAds=true`).
- **Banner auto-refresh (~60s)**: enabled on the **AdMob console** side for the banner ad unit. This
  is a **console configuration, NOT code** — there is no refresh timer in the app; the SDK rotates the
  banner on its own once enabled in the dashboard.

---

## 9. Development Environment
- **Machines**: MSI laptop / Surface / Desktop (per maintainer; build verified here with the
  Android Studio JBR — OpenJDK 21 — set as `JAVA_HOME`).
- **Test devices**: Samsung Galaxy S9 (modest) and S23+ (high-end).
- **Tools**: AGP 8.13.2, Kotlin 1.9.25, Compose Compiler 1.5.15, Compose BOM 2024.09.03, JDK 11 target.
- **Build/run**: `./gradlew compileDebugKotlin testDebugUnitTest` (verified green). On Windows set
  `JAVA_HOME` to Android Studio's JBR if the wrapper can't find a JDK.
- **Keystore**: external (not in Gradle). _Path / alias / passwords: TO BE COMPLETED by maintenance —
  not stored in this document._
- **Fresh-machine resume**: install Android Studio (matching AGP/JDK), open the project, let Gradle
  sync (version catalog pins everything), run on a device/emulator. No Git repo (see limitations).

---

## 10. Key Decisions Log — V2
- Direction "sportive modern" / bookmaker visual; odds in `OddsYellow` (#FFD600).
- TRJ as an always-visible badge (`RtpPill`) top-right of the card + colored left side bar by TRJ.
- Per-row reservation flags for empty-column alignment (`reserveLabelSpace`/`reserveBookmakerSpace`).
- **Rectangular share image** at the card's exact natural size (re-measure with UNSPECIFIED height,
  12dp margin) — the earlier square `max(w,h)` raster compositing was **removed** in V2: it added
  large empty bands and didn't reliably prevent share-sheet preview cropping anyway. The delivered
  PNG is now tight to the card; a possibly-cropped *preview* in some apps is an accepted trade-off.
- **New adaptive launcher icon**: foreground as a `VectorDrawable`, solid `#0A0A0B` background; PNG
  fallbacks for API < 26. Same logo rasterised at runtime for the share signature (adaptive XML can't
  be loaded by `painterResource`). **2.0.1 tweaks**: the `<group>` scale was reduced `0.85 → 0.65`
  (`translate 4/4`) to keep the ring inside the adaptive-icon visible/safe zone (~72/108) on circular/
  squircle launcher masks that were clipping it; and the value arc was recoloured to the **odds yellow
  `#FFD600`** (matching the in-app cote accent).
- **Generous silent auto-save on form exit** (back button or close icon) rather than a confirm-on-
  discard pop-up or a blocked back gesture: anything with real content (`hasMeaningfulContent`) is
  saved/updated in place via `saveToHistory`; only a blank form is dropped. Chosen to never lose
  in-progress work while keeping the UX frictionless (no dialog/snackbar).
- **Form bottom-padding fix**: removed the redundant `.navigationBarsPadding()` on the form LazyColumn
  (double-counted the nav-bar inset already in `innerPadding`) and reduced the bottom `contentPadding`
  16dp → 8dp, killing the tall black band above the SAVE button while preserving the functional
  clearance and IME scroll-into-view.
- `savedAt` frozen on edit (preserves relative order); `savedAt` computed on duplication to place the
  copy below the original in the active sort mode (`seekSmaller = isNoDate == newestFirst`).
- Auto-scroll (**rewritten in 2.0.1**, §6.6): the `listBefore`/`drop(1)` "wait for a different list
  instance" approach broke on date edits (the re-sort had already landed, so the wait never resolved
  and the moved card wasn't followed). Replaced by: wait for the id in the up-to-date sorted list →
  `withFrameNanos` + `delay(300)` + a layout-stability loop → **index-based** visibility via the
  `fullyVisibleRun()` helper (offsets are unreliable under `Modifier.animateItem()`) →
  `animateScrollToItem` with a non-animated `scrollToItem` snap fallback. 16dp top padding kept.
- **Share card `fillIncompleteRows = outcomes.size > 3`** (2.0.1): for ≤3 outcomes the trailing row
  is no longer padded, so the shared image matches the on-screen card (blocks fill the width) instead
  of leaving a phantom empty column.
- **Fragment dependency constraint** (2.0.1): forced `androidx.fragment:fragment:1.8.6` via a Gradle
  `constraints {}` block. `play-services-basement` (transitive from AdMob / Play review) pulled
  `fragment:1.1.0`, raising a Play Console deprecation warning; the constraint bumps the resolved
  version without adding a direct dependency.
- Cold start: Gson load async (Dispatchers.IO), `MobileAds.initialize` deferred, form `AdBanner`
  deferred 300ms. **Splash 500ms kept** (brand identity).
- Compose skipping: `HistoryCard` made skippable via id-based stable callbacks (+ remembered
  `onReopen`/`onDuplicateCard`).
- File split (Lot 3): `BetAnalysisCard.kt` → `OutcomeFields.kt` + (formerly) `RtpDisplay.kt`;
  `HistoryScreen.kt` → `OddsBlock.kt` + `ShareableCard.kt`; `RtpPill.kt` extracted.
- Code cleanup (Lot 1): removed 18 dead strings, 3 dead functions, dead imports, 2 template tests,
  fixed stale comments/KDoc.
- **Rejected**: light theme; AutoSizeOddsText optimisation (risk/gain unfavourable); mainstream pivot
  (target stays sharp bettors).

## 11. Known Limitations V2
- No light theme; no landscape-adapted layout.
- No search/filter in history; no undo on delete.
- The in-progress form is auto-saved only when you **exit** it; if the OS kills the app while you are
  still typing, the unsaved edits are lost (no persistent draft — see V3 backlog).
- Tests cover only odds conversion + retro-compat (no Compose UI tests).
- `isMinifyEnabled = false` (no R8/ProGuard on release).
- No Crashlytics / GA4; no `signingConfig` in Gradle (external signing); no Git repo (manual backups).
- **`RtpDisplay` was removed in V2** (detailed RTP/margin surface) — it was unused; only the compact
  `RtpPill` remains. (Noted in case its absence is questioned later.)
- Android share-sheet preview: the shared image is rectangular (card-sized), so some apps may crop
  the *preview* thumbnail. Accepted trade-off — the delivered PNG is always intact.

## 12. V3 Backlog
- Persist the sort preference (DataStore).
- Per-card "+N" → full-screen "all outcomes".
- Search/filter history; confirm-on-delete.
- Persistent in-progress draft (survive an OS process kill, e.g. DataStore / `SavedStateHandle`).
- Light theme + dynamic color; responsive landscape.
- Compose UI tests.
- Migrate Gson → kotlinx.serialization (lighter cold start).
- Enable R8/ProGuard; add Crashlytics + GA4.
- Surebet history view; Stake / Kelly / EV calculator; export/import history.
- More locales (IT, PT, …).
- Optimise `AutoSizeOddsText` if scroll ever becomes a problem.
- Kotlin 2.x + Compose Compiler Gradle plugin.
```
