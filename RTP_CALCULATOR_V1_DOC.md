# RTP Calculator — V1 Technical & Functional Documentation

This document is the authoritative reference for the V1 codebase of the RTP
Calculator Android app. It captures every file, every formula, every value and
every decision known at V1, so that a future Claude development session can
resume work **without access to the source code**.

Document regenerated against the codebase state on **2026-05-17** (versionName
`1.0.2`, versionCode `3`).

> Accuracy notes for this revision (verified against source, not the prior doc):
> * The app now ships **four locales** (English, French, Spanish, German), not two.
> * The RTP color gradient's top threshold in code is **95%** (`rtp < 95 → lerp;
>   else → green`). There is **no 97% stop** anywhere in `RtpGradient.kt`. The
>   "92–97% / ≥97%" wording from the brief does not match the code; this document
>   describes the code as written. See §6.3.
> * `strings.xml` contains **35** keys (one of them, `new_analysis`, is unused).
> * `OutcomeRow` column weights are **1.7 / 1.3 / 1.5** (+28dp icon slot), not 2/1/1.5.

---

## 1. APP IDENTITY

| Field | Value |
| --- | --- |
| App name (English) | `RTP Calculator` |
| App name (French) | `Calculateur de TRJ` |
| App name (Spanish) | `Calculadora de RTP` |
| App name (German) | `RTP-Rechner` |
| Package / `applicationId` | `com.rtpcalculator` |
| Android namespace | `com.rtpcalculator` |
| Version name | `1.0.2` |
| Version code | `3` |
| Min SDK | `24` (Android 7.0 Nougat) |
| Target SDK | `35` (Android 15) |
| Compile SDK | `35` |
| Supported languages | English (default, `values/`), French (`values-fr/`), Spanish (`values-es/`), German (`values-de/`) |
| Target platform | Android phones & tablets. Layout designed for **portrait**; orientation is **not locked** in the manifest. |
| Theme | Dark theme only (no light theme defined) |
| Play Store status | Published. Prior doc recorded V1 as live; this build is an update (`versionCode = 3`, `versionName = 1.0.2`). **Published countries: TO BE CONFIRMED — not derivable from the repo.** |
| Monetization | Single Google AdMob banner (50dp), one production unit. No interstitial/rewarded/native. |
| AdMob App ID | `ca-app-pub-2303541955549784~7410006753` |
| AdMob Banner Unit ID (prod) | `ca-app-pub-2303541955549784/4795362417` |
| AdMob Banner Unit ID (test, debug only) | `ca-app-pub-3940256099942544/6300978111` (Google sample unit) |
| Single Activity | `com.rtpcalculator.MainActivity` |
| In-app review | Google Play Core In-App Review, triggered on the **5th** launch |
| Required permissions | **None** declared in `AndroidManifest.xml` (no `<uses-permission>`); `play-services-ads` injects its own via its manifest |

---

## 2. TECHNICAL ARCHITECTURE

### 2.1 Tech stack — exact versions

All library/plugin versions are centralised in `gradle/libs.versions.toml`.

| Component | Version | Source |
| --- | --- | --- |
| Android Gradle Plugin (AGP) | `8.13.2` | `libs.versions.toml` `agp` |
| Kotlin | `1.9.25` | `libs.versions.toml` `kotlin` |
| Kotlin Compose Compiler extension | `1.5.15` | `app/build.gradle.kts` `composeOptions` |
| Compose BOM | `2024.09.03` | `libs.versions.toml` `composeBom` |
| `androidx.core:core-ktx` | `1.13.1` | `coreKtx` |
| `androidx.lifecycle:lifecycle-runtime-ktx` | `2.8.3` | `lifecycleRuntimeKtx` |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | `2.8.3` | `lifecycleViewmodelCompose` |
| `androidx.activity:activity-compose` | `1.9.1` | `activityCompose` |
| `androidx.compose.ui:ui` / `ui-graphics` / `ui-tooling` / `ui-tooling-preview` / `ui-test-manifest` / `ui-test-junit4` | managed by Compose BOM `2024.09.03` | `libs.versions.toml` |
| `androidx.compose.material3:material3` | managed by Compose BOM `2024.09.03` | — |
| `androidx.compose.material:material-icons-extended` | managed by Compose BOM `2024.09.03` | — |
| `com.google.code.gson:gson` | `2.10.1` | `gson` |
| `com.google.android.play:review` | `2.0.2` | `playReview` |
| `com.google.android.play:review-ktx` | `2.0.2` | `playReview` (same ref) |
| `com.google.android.gms:play-services-ads` | `23.6.0` | `playServicesAds` |
| JUnit | `4.13.2` | `junit` |
| AndroidX Test JUnit ext | `1.2.1` | `junitVersion` |
| Espresso Core | `3.6.1` | `espressoCore` |
| Gradle distribution | `8.13` (`gradle-8.13-bin.zip`) | `gradle/wrapper/gradle-wrapper.properties` |
| Gradle daemon JVM toolchain | Java `21` (auto-provisioned via Foojay) | `gradle/gradle-daemon-jvm.properties` (`toolchainVersion=21`) |
| `sourceCompatibility` / `targetCompatibility` | Java `11` | `app/build.gradle.kts` |
| Kotlin `jvmTarget` | `11` | `app/build.gradle.kts` |
| Build features | `compose = true`, `buildConfig = true` | `app/build.gradle.kts` |
| Release minification | `isMinifyEnabled = false` | `app/build.gradle.kts` |
| Gradle JVM args | `-Xmx2048m -Dfile.encoding=UTF-8` | `gradle.properties` |
| Kotlin code style | `official` | `gradle.properties` |
| AndroidX | enabled (`android.useAndroidX=true`) | `gradle.properties` |

External Node tooling (icon generation only, **not** part of the APK):

| Component | Version | Source |
| --- | --- | --- |
| `sharp` | `^0.34.5` | `package.json` |
| Node script | `generate_icons.mjs` (ES module, uses `sharp`) | project root |

### 2.2 Project folder structure (role of every file/folder)

```
C:\RTP-Calculator\
├─ build.gradle.kts          Root Gradle. Declares android-application + kotlin-android plugins, apply false.
├─ settings.gradle.kts       rootProject.name="RTP Calculator"; include(":app"); pluginManagement
│                            (google content filters, mavenCentral, gradlePluginPortal);
│                            dependencyResolutionManagement = FAIL_ON_PROJECT_REPOS (google, mavenCentral).
├─ gradle.properties         JVM args (-Xmx2048m, UTF-8), kotlin.code.style=official, android.useAndroidX=true.
├─ gradlew / gradlew.bat     Gradle wrapper launchers.
├─ local.properties          SDK path (NOT committed; machine-specific). Prior machine: C:\Users\Julien\AppData\Local\Android\Sdk.
├─ generate_icons.mjs        Node/sharp script. Regenerates legacy + adaptive-foreground launcher PNGs from
│                            "Logo - RTP Calculator.png". BG_COLOR = #111111. Densities m/h/xh/xxh/xxxhdpi
│                            (legacy 48/72/96/144/192, foreground 108/162/216/324/432).
├─ package.json              Node deps: sharp ^0.34.5 only.
├─ node_modules/             Local node deps (sharp + transitive).
├─ Logo - RTP Calculator.png Master logo consumed by generate_icons.mjs.
├─ build/                    Root Gradle output (transient).
├─ gradle/
│  ├─ libs.versions.toml            Single source of truth for ALL library + plugin versions.
│  ├─ gradle-daemon-jvm.properties  Foojay JDK 21 toolchain URLs (toolchainVersion=21).
│  └─ wrapper/                      Gradle 8.13 wrapper jar + gradle-wrapper.properties.
└─ app/
   ├─ build.gradle.kts       Module Gradle. namespace/applicationId com.rtpcalculator, compileSdk 35,
   │                         minSdk 24, targetSdk 35, versionCode 3, versionName "1.0.2",
   │                         Java 11 / jvmTarget 11, compose + buildConfig true,
   │                         composeCompilerExtension 1.5.15, release isMinifyEnabled=false
   │                         (NO signingConfig declared — see §9 keystore note).
   ├─ proguard-rules.pro     Only the default commented template lines. No active rules (minify disabled).
   └─ src/
      ├─ main/
      │  ├─ AndroidManifest.xml   Single MainActivity (LAUNCHER, exported=true, windowSoftInputMode=adjustResize,
      │  │                        theme @style/Theme.RTPCalculator). supportsRtl=true, allowBackup=true,
      │  │                        data_extraction_rules + backup_rules referenced. AdMob APPLICATION_ID meta-data
      │  │                        at <application> level. NO <uses-permission>, NO screenOrientation lock.
      │  ├─ assets/adi-registration.properties   AdMob/AdId registration metadata (auto-generated).
      │  ├─ java/com/rtpcalculator/
      │  │  ├─ MainActivity.kt        Entry point + root Composables (RtpApp, SplashScreen, AdBanner). See §3.1.
      │  │  ├─ data/HistoryRepository.kt   SharedPreferences("rtp_history") + Gson persistence. See §3.2.
      │  │  ├─ model/BetAnalysis.kt        Outcome / BetAnalysis / RtpResult data classes + calculateRtp(). See §3.3.
      │  │  ├─ ui/
      │  │  │  ├─ BetAnalysisCard.kt   Editable analysis card, pickers, OutcomeRow, CompactField, RtpDisplay. §3.4.
      │  │  │  ├─ HistoryScreen.kt     History list, sort toggle, HistoryCard, HeaderArea, OddsChip. §3.5.
      │  │  │  ├─ HistorySorting.kt    Pure sortHistory(history, newestFirst). §3.6 / §6.4.
      │  │  │  ├─ DateUtils.kt         parseDateToMillis / parseDateTimeToMillis (9 formats). §3.7.
      │  │  │  ├─ Formatters.kt        normalizeOddsOrNull / formatOddsTwoDecimals / formatOddsForDisplay. §3.8 / §6.5.
      │  │  │  ├─ RtpGradient.kt       Pure rtpGradientColor(rtp). §3.9 / §6.3.
      │  │  │  └─ theme/
      │  │  │     ├─ Color.kt          ALL hex tokens, semantically named. §4.2 / §7.1.
      │  │  │     ├─ Theme.kt          RTPCalculatorTheme: dark-only Material3 colorScheme. §7.5.
      │  │  │     └─ Type.kt           Typography: only bodyLarge customised. §7.2.
      │  │  └─ viewmodel/BetViewModel.kt   AndroidViewModel: analyses + history StateFlows. §3.10.
      │  └─ res/
      │     ├─ drawable/ic_launcher_background.xml   Solid #111111 shape (adaptive icon background).
      │     ├─ mipmap-anydpi-v26/ic_launcher.xml + ic_launcher_round.xml   Adaptive icon (bg drawable + fg @mipmap/ic_launcher_fg).
      │     ├─ mipmap-{m,h,xh,xxh,xxxh}dpi/          Generated launcher PNGs (legacy + foreground).
      │     ├─ values/strings.xml      35 English UI strings (see §4.1).
      │     ├─ values/colors.xml       Empty <resources/> (all colors live in Color.kt).
      │     ├─ values/themes.xml       Theme.RTPCalculator (parent android:Theme.Material.Light.NoActionBar,
      │     │                          only windowBackground=@android:color/black overridden).
      │     ├─ values-fr/strings.xml   French translation (35 keys).
      │     ├─ values-es/strings.xml   Spanish translation (35 keys).
      │     ├─ values-de/strings.xml   German translation (35 keys).
      │     └─ xml/{backup_rules.xml, data_extraction_rules.xml}   Default empty rule files.
      ├─ test/java/com/rtpcalculator/ExampleUnitTest.kt           Default template test (JUnit wired, not real coverage).
      └─ androidTest/java/com/rtpcalculator/ExampleInstrumentedTest.kt   Default template instrumented test.
```

### 2.3 Architecture pattern

* **Single Activity, single ViewModel, 100% Jetpack Compose.** No Fragments,
  no XML layouts (only theme/strings/icon resources).
* **No Navigation-Compose / NavHost.** Screen switching is a `Boolean showForm`
  state in `RtpApp` plus an `AnimatedVisibility` that slides the form vertically
  over the always-present history.
* **Unidirectional data flow.** Composables read `StateFlow`s; every input emits
  a high-level event (`onDateChange`, `onOutcomeOddsChange`, …) → `BetViewModel`
  applies an immutable `copy()`.
* **State holder:** `BetViewModel : AndroidViewModel(app)`, constructed by the
  default `ViewModelProvider` (no DI framework). Two `MutableStateFlow`s exposed
  read-only as `StateFlow`: `analyses` (in-progress; effectively ≤1 entry) and
  `history`.
* **Persistence:** `SharedPreferences("rtp_history")` key `"saved_analyses"`
  holding one Gson-serialised `List<BetAnalysis>`. Loaded once in
  `BetViewModel.init {}`, fully rewritten on every save/delete. **No Room, no
  DataStore.** A separate `SharedPreferences("rtp_prefs")` key `"open_count"`
  tracks launches for the review prompt.
* **No network** other than the AdMob SDK.
* **Immutability:** `Outcome`, `BetAnalysis`, `RtpResult` are `@Immutable` data
  classes with `UUID` string ids.

---

## 3. ALL KOTLIN FILES

Every production Kotlin file, its path, responsibility, and every
function/composable in it.

### 3.1 `app/src/main/java/com/rtpcalculator/MainActivity.kt`
Entry point and root composables.

* **`class MainActivity : ComponentActivity`** — field `viewModel: BetViewModel by viewModels()`.
  * **`onCreate(savedInstanceState)`** — `enableEdgeToEdge()`, `MobileAds.initialize(this)`,
    `trackOpenCountAndRequestReview()`, then `setContent { RTPCalculatorTheme { RtpApp(viewModel) } }`.
  * **`trackOpenCountAndRequestReview()`** — reads `SharedPreferences("rtp_prefs")`,
    `open_count` (default 0) +1, persists it. **When `count == 5` exactly**,
    `ReviewManagerFactory.create(this)` → `requestReviewFlow()`; on success
    `launchReviewFlow`. Failures silently ignored. Side effect: writes prefs every launch.
* **`RtpApp(viewModel: BetViewModel)`** `@Composable` `@OptIn(ExperimentalMaterial3Api)`.
  State: `history` (collected), `showForm` (Boolean, default false),
  `showSplash` (Boolean, default true). `LaunchedEffect(Unit)`: `delay(500)` then
  `showSplash = false`. `closeAndDiscard`: deletes the first entry in
  `viewModel.analyses` and sets `showForm = false`; bound to `BackHandler(enabled = showForm)`.
  Renders a `Box`: `Scaffold` → `HistoryScreen` (banner = `AdBanner()`,
  `bottomContentPadding = 82.dp`) with the sticky "CREATE A BET ANALYSIS"
  `Button` (`onClick`: `clearAnalyses()` → `addAnalysis()` → `showForm = true`);
  an `AnimatedVisibility` (slide-in/out vertical) hosting the form
  `Scaffold(contentWindowInsets = WindowInsets(0))` with `CenterAlignedTopAppBar`
  (title = event name or `R.string.new_bet_analysis`, ExtraBold 26sp, ellipsis;
  navigation icon = Close → `closeAndDiscard`) + `AdBanner()` + a `LazyColumn`
  containing `BetAnalysisCard`; and an `AnimatedVisibility` (fade) `SplashScreen`.
  The form binds to `analyses.firstOrNull()`; `onSave` = `saveToHistory(id)` →
  `deleteAnalysis(id)` → `showForm = false`.
* **`SplashScreen()`** `@Composable` — full-size `Box`, `Color.Black`, centered
  `Text(app_name)` white Bold 36sp.
* **`AdBanner()`** `@Composable` — `AndroidView` wrapping an `AdView`, full width,
  height 50dp, `AdSize.BANNER`, `adUnitId = AD_UNIT_ID`, `loadAd(AdRequest.Builder().build())`.
* File-level: `PROD_AD_UNIT_ID = "ca-app-pub-2303541955549784/4795362417"`,
  `TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"`,
  `AD_UNIT_ID = if (BuildConfig.DEBUG) TEST_AD_UNIT_ID else PROD_AD_UNIT_ID`.

### 3.2 `app/src/main/java/com/rtpcalculator/data/HistoryRepository.kt`
Persistence layer.

* **`class HistoryRepository(context)`** — `prefs = getSharedPreferences("rtp_history", MODE_PRIVATE)`,
  `gson = Gson()`, `key = "saved_analyses"`.
  * **`load(): List<BetAnalysis>`** — reads the JSON string; null → `emptyList()`;
    Gson `fromJson` with `TypeToken<List<BetAnalysis>>`; any exception → `emptyList()`.
  * **`save(analyses)`** — `prefs.edit().putString(key, gson.toJson(analyses)).apply()`.

### 3.3 `app/src/main/java/com/rtpcalculator/model/BetAnalysis.kt`
Domain model + the core RTP function.

* **`data class Outcome`** `@Immutable` — `id` (UUID), `label=""`, `odds=""`, `bookmaker=""`.
* **`data class BetAnalysis`** `@Immutable` — `id` (UUID), `date=""`, `time=""`,
  `eventName=""`, `betType=""`, `outcomes = listOf(Outcome(), Outcome())`, `savedAt=0L`.
* **`data class RtpResult`** `@Immutable` — `rtp: Double`, `margin: Double`, `isSurebet: Boolean=false`.
* **`calculateRtp(outcomes): RtpResult?`** — see §6.1 for the exact formula.

### 3.4 `app/src/main/java/com/rtpcalculator/ui/BetAnalysisCard.kt`
The full editable analysis card. `@file:OptIn(ExperimentalMaterial3Api, ExperimentalFoundationApi)`.

* File-level `CardBorder = BorderStroke(1.dp, CardOutlineSoft)`.
* **`fieldColors()`** `@Composable` private — `OutlinedTextFieldDefaults.colors`
  with container `FieldBackground`, focused border/label/cursor `FieldFocus`.
* **`Modifier.scrollIntoViewOnFocus()`** private `composed` — on focus, `delay(300)`
  then `BringIntoViewRequester.bringIntoView()`.
* **`parseTimeOrNull(time): Pair<Int,Int>?`** private — splits `"HH:mm"`, validates
  `h∈0..23`, `m∈0..59`.
* **`BetAnalysisCard(...)`** `@Composable` — params: `analysis`, the 8 change/add/remove
  callbacks, `onSave`, `modifier`. State: `rtpResult` (recomputed on `analysis.outcomes`;
  only non-null when **all** outcomes parse `>0.0`, then `calculateRtp`);
  `showDatePicker`/`showTimePicker` Booleans; `datePickerState`
  (`initialSelectedDateMillis = parseDateToMillis(analysis.date)`). Renders:
  `DatePickerDialog` (confirm formats selected millis to `dd/MM/yyyy` Locale.ENGLISH
  → `onDateChange`); a time `AlertDialog` containing `TimePicker` (24h, seeded from
  `parseTimeOrNull` or 12:00; confirm → `onTimeChange("%02d:%02d")`); the `Card`
  (24dp corners, `CardBorder`, 2dp elevation, `CardBg`) with column padding
  `horizontal 16dp / vertical 12dp` containing: Event Name field; 6dp spacer; Date
  field (readOnly + transparent click overlay, overlay `end`-padded 48dp when a
  date is set so the clear "×" stays tappable; trailing icon = Close→clear when set,
  else DateRange); if `date.isNotBlank()` a Time field (same overlay pattern,
  AccessTime icon); Bet Type field (placeholder `bet_type_placeholder`); 8dp spacer;
  `OUTCOMES` header (16sp Bold, letterSpacing 1.5sp, `TextSecondary`, centered);
  10dp spacer; the `analysis.outcomes` list (each wrapped in `key(outcome.id)`,
  4dp spacers); the Add-Outcome `Row` (clipped 20dp, AddCircle + `add_outcome`
  label); 6dp spacer; `RtpDisplay(rtpResult)` or the `fill_odds_to_calculate_rtp`
  helper (12sp `TextTertiary` centered); 10dp spacer; the SAVE button
  (56dp, 12dp corners, `OverlayWhite30` border, `ActionAccent`, label Bold 18sp
  `ButtonLabel`). Event Name and Bet Type fields use `scrollIntoViewOnFocus()`;
  date/time fields do not.
* **`OutcomeRow(outcome, canRemove, onLabelChange, onOddsChange, onBookmakerChange, onRemove)`**
  `@Composable` private — `Row`, `spacedBy(4.dp)`. Three `CompactField`s with
  weights **`1.7f` (label, `R.string.outcome`)**, **`1.3f` (odds, `R.string.odds`,
  `KeyboardType.Decimal`, `onFocusLost` → `formatOddsTwoDecimals` if changed)**,
  **`1.5f` (bookmaker, `R.string.book`)**, then either a 28dp delete `IconButton`
  (error-colored, shown only when `canRemove`) or a 28dp `Spacer`.
* **`CompactField(value, onValueChange, label, modifier, keyboardOptions, onFocusLost)`**
  `@Composable` private — `BasicTextField` with `OutlinedTextFieldDefaults.DecorationBox`,
  14dp container corners, content padding `horizontal 8dp / vertical 16dp`, text
  `TextPrimary`, cursor `FieldFocus`. Tracks `wasFocused` to fire `onFocusLost`
  on focus loss. Uses `scrollIntoViewOnFocus()`.
* **`RtpDisplay(result: RtpResult)`** `@Composable` — `rtpColor = rtpGradientColor(result.rtp)`;
  `rtpText = "%.2f%%"` (Locale.ENGLISH), prefixed `"⭐ "` when `isSurebet`;
  `marginText = "%.2f%%"`. `Surface` 12dp corners, `Color.Black`, `Row` (vertical
  padding 12dp): left column `rtp_return_to_player` label (labelSmall, `TextSecondary`)
  + value (22sp ExtraBold, `rtpColor`); `VerticalDivider` 40dp `OverlayWhite30`;
  right column `books_margin` label + margin (14sp Medium, `rtpColor`).

### 3.5 `app/src/main/java/com/rtpcalculator/ui/HistoryScreen.kt`
Main always-visible screen and its cards.

* File-level `CardBorder = BorderStroke(1.5.dp, OverlayWhite30)`.
* **`tintedCardBg(rtpResult): Color`** private — `null` → `CardBg`; else
  `lerp(CardBg, rtpGradientColor(rtp), 0.15f)`.
* **`HistoryScreen(history, onReopen, onDelete, bottomContentPadding=0.dp, banner={})`**
  `@Composable` `@OptIn(ExperimentalFoundationApi)`. State `newestFirst`
  (Boolean, default true, **not persisted**). `sortedEntries = sortHistory(history, newestFirst)`.
  If `history.isEmpty()`: column with `banner()` + centered `no_analyses_yet`
  (14sp `TextTertiary`) and returns. Else: a top `Row` (End-aligned) with a
  clickable sort toggle (label `sort_by_event_date` labelMedium `TextSecondary`
  + `KeyboardArrowDown` if newestFirst else `KeyboardArrowUp`, 12dp clip);
  `banner()`; then a `LazyColumn` (contentPadding start/end 12dp, top 8dp,
  bottom `8.dp + bottomContentPadding`, `spacedBy(6.dp)`) of `HistoryCard`s
  keyed by id with `Modifier.animateItem()`.
* **`HistoryCard(analysis, onReopen, onDelete, modifier)`** `@Composable` private —
  `rtpResult = calculateRtp(analysis.outcomes)` (note: **no all-filled guard
  here**, unlike the form); `cardColor = tintedCardBg(rtpResult)`;
  `previewOdds = outcomes.take(3)`, `extraCount = size - previewOdds.size`.
  `Card` (24dp, `CardBorder` 1.5dp, 2dp elevation, `cardColor`, clickable→onReopen),
  column padding `16dp/12dp`: `HeaderArea`; 10dp spacer; a `Row` (`spacedBy(8.dp)`)
  of up to 3 `OddsChip`s (`weight(1f)`) + a `"+$extraCount"` label when >0; if
  `rtpResult != null` a 10dp spacer + `RtpDisplay`.
* **`HeaderArea(date, time, eventName, betType, onDelete)`** `@Composable` private —
  `dateLabel = "$date — $time"` when both present else `date`; event name 19sp
  Bold `TextPrimary` centered ellipsis; line 2 = `buildAnnotatedString`:
  `DatePresentStyle` (date) or `DateBlankStyle` (`no_date`) then optionally
  `" · "` + betType in `BetTypeStyle`; trailing 32dp delete `IconButton`
  (`DangerRed`, 18dp icon).
* SpanStyles: `Line2FontSize = 13.sp`; `DatePresentStyle` `TextSecondary`/13sp/SemiBold;
  `DateBlankStyle` `TextTertiary`/13sp/Italic; `BetTypeStyle` `TextTertiary`/13sp.
* **`OddsChip(label, odds, bookmaker, modifier)`** `@Composable` private — `Surface`
  20dp corners, `surfaceVariant`, `OverlayWhite30` 1dp border. Column (padding
  `horizontal 8dp / vertical 2dp`): bookmaker (11sp Light `TextTertiary`, if
  non-blank) / `formatOddsForDisplay(odds)` (18sp Bold `TextPrimary`) / label
  (labelSmall `TextSecondary`, if non-blank).

### 3.6 `app/src/main/java/com/rtpcalculator/ui/HistorySorting.kt`
* **`sortHistory(history, newestFirst): List<BetAnalysis>`** — pure. See §6.4.

### 3.7 `app/src/main/java/com/rtpcalculator/ui/DateUtils.kt`
* `DATE_FORMATS` (9): `yyyy-MM-dd`, `dd/MM/yyyy`, `MM/dd/yyyy`, `dd-MM-yyyy`,
  `dd MMM yyyy`, `MMM dd, yyyy`, `d/M/yyyy`, `d-M-yyyy`, `d MMM yyyy`.
* **`parseDateToMillis(date): Long?`** — trims; tries each format with
  `Locale.ENGLISH`, `isLenient=false`; returns first parse `.time`, else `null`.
* **`parseDateTimeToMillis(date, time): Long?`** — `parseDateToMillis(date)` or
  null; if `time` is valid `HH:mm` (h∈0..23, m∈0..59) adds
  `h*3_600_000 + m*60_000`, else returns the date millis alone.

### 3.8 `app/src/main/java/com/rtpcalculator/ui/Formatters.kt`
* **`normalizeOddsOrNull(raw): String?`** — `replace(',', '.')`, `toDoubleOrNull()`;
  `"%.2f".format(Locale.ENGLISH, d)` or `null`.
* **`formatOddsTwoDecimals(raw): String`** — `normalizeOddsOrNull(raw) ?: raw`.
* **`formatOddsForDisplay(raw): String`** — blank → `"—"`; else normalised or raw.

### 3.9 `app/src/main/java/com/rtpcalculator/ui/RtpGradient.kt`
* **`rtpGradientColor(rtp): Color`** — the 4-stop gradient. See §6.3 (exact code).

### 3.10 `app/src/main/java/com/rtpcalculator/viewmodel/BetViewModel.kt`
* **`BetAnalysis.withNormalizedOdds()`** private ext — copies, mapping every
  outcome's `odds` through `formatOddsTwoDecimals`.
* **`class BetViewModel(app) : AndroidViewModel(app)`** — `repo = HistoryRepository(app)`;
  `_analyses`/`analyses` and `_history`/`history` StateFlows; `init` loads history.
  * `addAnalysis()` appends a new `BetAnalysis()`. `clearAnalyses()` → empty.
  * `deleteAnalysis(id)` removes from `_analyses`.
  * `saveToHistory(id)` — finds in `_analyses`; `withNormalizedOdds()` + `savedAt =
    System.currentTimeMillis()`; replaces any history entry with same id; persists.
  * `reopenFromHistory(id)` — finds in `_history`; if not already open, appends
    `withNormalizedOdds()` copy to `_analyses`.
  * `deleteFromHistory(id)` — removes from `_history`, persists.
  * `updateDate` (blank date also clears time), `updateTime`, `updateEventName`,
    `updateBetType`, `updateOutcomeLabel/Odds/Bookmaker`, `addOutcome`,
    `removeOutcome` (no-op when `outcomes.size <= 2`) — all via private
    **`update(analysisId, block)`** which maps `_analyses` applying `block`.

### 3.11 Test source sets
`app/src/test/.../ExampleUnitTest.kt` and `app/src/androidTest/.../ExampleInstrumentedTest.kt`
are the default Android Studio templates — **no real coverage**.

---

## 4. ALL RESOURCE FILES

### 4.1 String keys — English value + fr / es / de translations

35 keys (identical key set in all four locales). `\'` is an escaped apostrophe.

| key | en (`values/`) | fr (`values-fr/`) | es (`values-es/`) | de (`values-de/`) |
| --- | --- | --- | --- | --- |
| `app_name` | RTP Calculator | Calculateur de TRJ | Calculadora de RTP | RTP-Rechner |
| `create_bet_analysis` | CREATE A BET ANALYSIS | CRÉER UNE ANALYSE PARI | CREAR UN ANÁLISIS DE APUESTA | WETTANALYSE ERSTELLEN |
| `new_bet_analysis` | New Bet Analysis | Nouvelle analyse de pari | Nuevo análisis de apuesta | Neue Wettanalyse |
| `new_analysis` *(defined, **unused** in code)* | New Analysis | Nouvelle analyse | Nuevo análisis | Neue Analyse |
| `event_name_optional` | Event Name (optional) | Nom évènement (facultatif) | Nombre del evento (opcional) | Event-Name (optional) |
| `event_date_optional` | Event Date (optional) | Date évènement (facultatif) | Fecha del evento (opcional) | Event-Datum (optional) |
| `event_time_optional` | Event Time (optional) | Heure évènement (facultatif) | Hora del evento (opcional) | Event-Uhrzeit (optional) |
| `bet_type_optional` | Bet Type (optional) | Type de pari (facultatif) | Tipo de apuesta (opcional) | Wettart (optional) |
| `bet_type_placeholder` | 1X2, Over/Under, etc. | 1X2, Les deux équipes marquent, etc. | 1X2, Más/Menos, etc. | 1X2, Über/Unter, usw. |
| `tap_to_pick_date` | Tap to pick a date | Appuyez pour choisir une date | Toca para elegir una fecha | Tippen, um ein Datum zu wählen |
| `tap_to_pick_time` | Tap to pick a time | Appuyez pour choisir une heure | Toca para elegir una hora | Tippen, um eine Uhrzeit zu wählen |
| `outcomes` | OUTCOMES | ISSUES | RESULTADOS | TIPPS |
| `outcome` | Outcome | Issue | Resultado | Tipp |
| `odds` | Odds | Cote | Cuota | Quote |
| `book` | Book | Book | Casa | Buki |
| `add_outcome` | Add Outcome | Ajouter une issue | Añadir resultado | Tipp hinzufügen |
| `fill_odds_to_calculate_rtp` | Fill in all odds to calculate RTP | Remplissez toutes les cotes pour calculer le TRJ | Rellena todas las cuotas para calcular el RTP | Alle Quoten eingeben, um den RTP zu berechnen |
| `save_to_history` | SAVE TO HISTORY | ENREGISTRER | GUARDAR | SPEICHERN |
| `rtp_return_to_player` | RTP (Return to Player) | TRJ (Taux Retour Joueur) | RTP (Retorno al Jugador) | RTP (Auszahlungsquote) |
| `books_margin` | Book's Margin | Marge Bookmakers | Margen de la casa | Marge des Buchmachers |
| `sort_by_event_date` | Sort by event date | Trier par date d'évènement | Ordenar por fecha del evento | Nach Event-Datum sortieren |
| `no_date` | No date | Sans date | Sin fecha | Kein Datum |
| `no_analyses_yet` | No analyses yet. Tap below to get started! | Aucune analyse. Appuyez ci-dessous pour commencer ! | Aún no hay análisis. ¡Toca abajo para empezar! | Noch keine Analysen. Unten tippen, um zu starten! |
| `unnamed` | Unnamed | Sans nom | Sin nombre | Unbenannt |
| `action_ok` | OK | OK | OK | OK |
| `action_cancel` | Cancel | Annuler | Cancelar | Abbrechen |
| `cd_close` | Close | Fermer | Cerrar | Schließen |
| `cd_clear_date` | Clear date | Effacer la date | Borrar la fecha | Datum löschen |
| `cd_pick_date` | Pick date | Choisir une date | Elegir una fecha | Datum wählen |
| `cd_clear_time` | Clear time | Effacer l'heure | Borrar la hora | Uhrzeit löschen |
| `cd_pick_time` | Pick time | Choisir une heure | Elegir una hora | Uhrzeit wählen |
| `cd_remove_outcome` | Remove | Supprimer | Eliminar | Entfernen |
| `cd_delete` | Delete | Supprimer | Eliminar | Löschen |
| `cd_toggle_sort_order` | Toggle sort order | Inverser l'ordre de tri | Invertir el orden de clasificación | Sortierreihenfolge umkehren |

All user-visible text is in `strings.xml` (no hardcoded UI strings in Kotlin).
`cd_*` keys are accessibility content descriptions.

### 4.2 Colors (`ui/theme/Color.kt`) — hex + semantic role

`colors.xml` is intentionally **empty**; all colors are Compose `Color(...)` in `Color.kt`.

| Token | Hex (ARGB literal) | Role |
| --- | --- | --- |
| `DarkBackground` | `0xFF0D0D0D` | Material3 `colorScheme.background` |
| `DarkSurface` | `0xFF1E1E1E` | `colorScheme.surface` (top app bar, dialogs) |
| `DarkSurfaceVariant` | `0xFF141414` | `colorScheme.surfaceVariant` (OddsChip bg) |
| `DarkPrimary` | `0xFFFF7A5C` | `colorScheme.primary` (picker confirm button) |
| `DarkOnPrimary` | `0xFF000000` | text on primary |
| `DarkOnBackground` | `0xFFFFFFFF` | text on background |
| `DarkOnSurface` | `0xFFFFFFFF` | text on surface |
| `DarkOnSurfaceVar` | `0xFFCCCCCC` | text on surfaceVariant |
| `DarkOutline` | `0xFF2E2E2E` | generic outline |
| `DarkError` | `0xFFEF9A9A` | Material error (delete icon in OutcomeRow uses `colorScheme.error`) |
| `TextPrimary` | `0xFFFFFFFF` | field text, card titles, RTP value |
| `TextSecondary` | `0xFFCCCCCC` | labels, sort label, RtpDisplay sub-labels, dated line |
| `TextTertiary` | `0xFFAAAAAA` | placeholders, "No date", "+N", helper text |
| `CardBg` | `0xFF2A2520` | analysis card bg / history card tint base |
| `CardOutlineSoft` | `0xFF2E2E2E` | 1dp analysis-card border |
| `FieldBackground` | `0xFF141414` | text-field container |
| `FieldFocus` | `0xFFFFFFFF` | focused border/label/cursor |
| `OverlayWhite30` | `0x4DFFFFFF` (white ~30%) | button/chip/divider/history-card borders |
| `ActionAccent` | `0xFF1B3482` | CTA & SAVE button background (deep indigo) |
| `ButtonLabel` | `0xFFF0F0F0` | primary-button label |
| `DangerRed` | `0xFFEF5350` | history-card trash icon |
| `RtpRed` | `0xFFEF5350` | gradient stop (lower) |
| `RtpOrange` | `0xFFFB8C00` | gradient stop |
| `RtpYellow` | `0xFFFDD835` | gradient stop |
| `RtpGreen` | `0xFF66BB6A` | gradient stop (upper & surebet) |
| (literal) `Color.Black` | `#000000` | splash bg, RtpDisplay container |
| (drawable) adaptive icon bg | `#111111` | `ic_launcher_background.xml` + `BG_COLOR` in `generate_icons.mjs` |

### 4.3 Theme configuration
* `values/themes.xml`: `Theme.RTPCalculator`, parent
  `android:Theme.Material.Light.NoActionBar`, only override
  `android:windowBackground = @android:color/black` (pre-Compose splash bg; the
  inherited `Light` name is cosmetic — Compose uses Material3 dark, §7.5).
* `Theme.kt`: `RTPCalculatorTheme` wraps `MaterialTheme(colorScheme = AppColorScheme,
  typography = Typography)`. `AppColorScheme = darkColorScheme(primary=DarkPrimary,
  onPrimary=DarkOnPrimary, background=DarkBackground, onBackground=DarkOnBackground,
  surface=DarkSurface, onSurface=DarkOnSurface, surfaceVariant=DarkSurfaceVariant,
  onSurfaceVariant=DarkOnSurfaceVar, outline=DarkOutline, error=DarkError,
  onError=DarkBackground)`.

### 4.4 Drawable & mipmap
* `drawable/ic_launcher_background.xml`: `<shape>` solid `#111111`.
* `mipmap-anydpi-v26/ic_launcher.xml` & `ic_launcher_round.xml`: `<adaptive-icon>`
  background `@drawable/ic_launcher_background`, foreground `@mipmap/ic_launcher_fg`.
* `mipmap-{m,h,xh,xxh,xxxh}dpi/`: generated legacy + foreground PNGs (from
  `generate_icons.mjs`; legacy 48/72/96/144/192 px, foreground 108/162/216/324/432 px).
* `xml/backup_rules.xml`, `xml/data_extraction_rules.xml`: default empty rule files.

---

## 5. IMPLEMENTED FEATURES

### 5.1 Splash screen
Pure-Compose `SplashScreen()`: full-screen black `Box`, centered app-name
(white, Bold, 36sp). Shown from cold start; `RtpApp`'s `LaunchedEffect(Unit)`
runs `delay(500)` (ms) then hides it, with `fadeIn`/`fadeOut`. The activity
window background is also `@android:color/black` so the pre-Compose frame matches.

### 5.2 History screen
Always the background content. Empty state: banner + centered `no_analyses_yet`.
Non-empty: End-aligned sort toggle row, banner, then a `LazyColumn` of cards
(`animateItem()` for reorder animation). Each `HistoryCard`: background tinted
`lerp(CardBg, rtpGradientColor(rtp), 0.15f)` (only if RTP computable);
header (event name or `unnamed`, bold 19sp centered; line 2 = date/`no_date` ·
betType); up to 3 `OddsChip`s + `+N` overflow; `RtpDisplay` if `calculateRtp`
is non-null. Tap card → `onReopen`; trash icon → immediate `onDelete` (no confirm).

### 5.3 New Bet Analysis form
Full-screen sheet sliding in vertically over an opaque background. Top app bar
(title = event name or `new_bet_analysis`, ExtraBold 26sp, ellipsised; Close ×
discards via `closeAndDiscard`; system back does the same via `BackHandler`).
AdBanner directly under the bar. Card fields:
* **Event Name** — free text, optional.
* **Event Date** — readOnly field + transparent click overlay opening a
  Material3 `DatePickerDialog`. Saved/displayed `dd/MM/yyyy` (Locale.ENGLISH).
  Trailing × clears it (also clears time, see ViewModel `updateDate`).
* **Event Time** — only rendered when date is non-blank. Same overlay pattern;
  opens a `TimePicker` (24h) inside an `AlertDialog`, seeded from current value
  or `12:00`. Saved `HH:mm`.
* **Bet Type** — free text, placeholder `bet_type_placeholder`.
* **Outcomes** — `OUTCOMES` header; dynamic `OutcomeRow` list. Always ≥2
  (default constructor); delete icon only shown when `outcomes.size > 2`;
  `removeOutcome` is a no-op at 2. "Add Outcome" pill appends an empty `Outcome`.
* **Validation rules:** there is **no blocking validation**. The live `RtpDisplay`
  appears only when **every** outcome's odds parse to `> 0.0` (`allOddsFilled`
  guard); otherwise the helper text shows. SAVE is always enabled and persists
  whatever is filled.
* **SAVE TO HISTORY** → `saveToHistory(id)` (normalises odds, stamps `savedAt`)
  → `deleteAnalysis(id)` → close.
* Fields use `scrollIntoViewOnFocus()` (300 ms delayed `BringIntoViewRequester`)
  with `windowSoftInputMode=adjustResize`.

### 5.4 RTP & margin calculation
See §6.1. Displayed as `"%.2f%%"` (Locale.ENGLISH) for both RTP and margin.

### 5.5 Color gradient
See §6.3 — exact thresholds and stops as in `RtpGradient.kt`.

### 5.6 Surebet detection & visual treatment
`isSurebet = rtp > 100.0` (`RtpResult`, set by `calculateRtp`). Sole visual
treatment: `RtpDisplay` prepends `"⭐ "` to the RTP text. No separate entity,
no list/badge elsewhere. Color is **not** changed by surebet (it follows the
gradient, which returns `RtpGreen` for any rtp ≥ 95, surebets included).

### 5.7 History sorting
See §6.4. Toggle state is per-composable and **not persisted**.

### 5.8 Odds formatting
See §6.5. Two decimals, `.` separator, Locale.ENGLISH; enforced on focus loss,
on save, and on reopen.

### 5.9 AdMob banner
`AdBanner()` full-width 50dp `AdSize.BANNER`. Appears: above the empty-state
text; under the sort row above the history list; and under the form's top app
bar. Unit ID switched at compile time via `BuildConfig.DEBUG` (see §8).
`MobileAds.initialize(this)` once in `onCreate`.

### 5.10 In-app review prompt
`trackOpenCountAndRequestReview()` increments `rtp_prefs/open_count` every
launch; **exactly** when it reaches 5, the Play in-app review flow is requested
and launched (failures ignored, fires at most once).

### 5.11 Localization
English (default) + French + Spanish + German, full string parity (35 keys).
App name localized per locale. DatePicker/TimePicker weekday/month labels follow
the **device** locale, which can differ from the app UI locale.

---

## 6. BUSINESS LOGIC

### 6.1 RTP & bookmaker-margin formula
`model/BetAnalysis.kt::calculateRtp(outcomes): RtpResult?`:
```kotlin
val odds = outcomes.mapNotNull { it.odds.toDoubleOrNull() }.filter { it > 0 }
if (odds.size < 2) return null
val impliedProbSum = odds.sumOf { 1.0 / it }
if (impliedProbSum <= 0) return null
val rtp = (1.0 / impliedProbSum) * 100.0
val margin = 100.0 - rtp
return RtpResult(rtp, margin, isSurebet = rtp > 100.0)
```
* Needs **≥2** strictly-positive parseable odds, else `null`.
* `RTP% = 100 / Σ(1/oddsᵢ)`; `Margin% = 100 − RTP` (can be negative on surebets).
* The **form** additionally requires *all* outcomes filled (`allOddsFilled`)
  before computing; the **history card** calls `calculateRtp` directly.

### 6.2 Surebet rule
`isSurebet = rtp > 100.0` (strictly). Equivalent to `Σ(1/oddsᵢ) < 1`. Visual:
`"⭐ "` prefix only (§5.6).

### 6.3 Color-gradient interpolation — EXACT code
`ui/RtpGradient.kt`:
```kotlin
fun rtpGradientColor(rtp: Double): Color = when {
    rtp < 78.0 -> RtpRed
    rtp < 87.0 -> lerp(RtpRed,    RtpOrange, ((rtp - 78.0) / 9.0).toFloat())
    rtp < 92.0 -> lerp(RtpOrange, RtpYellow, ((rtp - 87.0) / 5.0).toFloat())
    rtp < 95.0 -> lerp(RtpYellow, RtpGreen,  ((rtp - 92.0) / 3.0).toFloat())
    else       -> RtpGreen
}
```

| RTP range (%) | Result |
| --- | --- |
| `rtp < 78` | `RtpRed` `#EF5350` (flat) |
| `78 ≤ rtp < 87` | `lerp(RtpRed, RtpOrange, (rtp−78)/9)` |
| `87 ≤ rtp < 92` | `lerp(RtpOrange, RtpYellow, (rtp−87)/5)` |
| `92 ≤ rtp < 95` | `lerp(RtpYellow, RtpGreen, (rtp−92)/3)` |
| `rtp ≥ 95` (incl. surebet > 100) | `RtpGreen` `#66BB6A` (flat) |

`lerp` = `androidx.compose.ui.graphics.lerp` (linear RGBA).
Stops: `RtpRed #EF5350`, `RtpOrange #FB8C00`, `RtpYellow #FDD835`, `RtpGreen #66BB6A`.

> ⚠️ **Discrepancy vs the spec brief.** The brief described "92–97% green,
> ≥97% full green". The code has **no 97% threshold**: the yellow→green lerp
> spans **92–95**, and `rtp ≥ 95` is flat `RtpGreen`. There is no distinct
> ">100% surebet" color branch — surebets fall in the `else` (RtpGreen) and are
> marked only by the ⭐. This document follows the code. If the 95 vs 97 split
> is a bug, it is logged in §11; treat it as a known item, not as documented
> behavior to "fix silently".

### 6.4 Card background tint
`HistoryScreen.kt::tintedCardBg`: `rtpResult == null → CardBg`; else
`lerp(CardBg #2A2520, rtpGradientColor(rtp), 0.15f)` (15% toward the RTP color).
Used as the `HistoryCard` container color. The analysis-form card is always
flat `CardBg`.

### 6.5 History sorting algorithm
`ui/HistorySorting.kt`:
```kotlin
val (noDate, withDate) = history.partition { it.date.isBlank() }
val sortedNoDate = noDate.sortedByDescending { it.savedAt }
return if (newestFirst) {
    val s = withDate.sortedWith(compareByDescending { parseDateTimeToMillis(it.date,it.time) ?: Long.MIN_VALUE })
    s + sortedNoDate
} else {
    val s = withDate.sortedWith(compareBy { parseDateTimeToMillis(it.date,it.time) ?: Long.MAX_VALUE })
    sortedNoDate + s
}
```
* Undated entries are **always** ordered by `savedAt` descending, regardless of
  `newestFirst`.
* Dated entries sort by parsed date+time; unparseable dates sink to the bottom
  in both directions (`MIN_VALUE` when newestFirst, `MAX_VALUE` otherwise).
* Concatenation: newestFirst → `dated + undated` (undated at bottom);
  oldestFirst → `undated + dated` (undated at top).

### 6.6 Odds formatting rules
`ui/Formatters.kt` (§3.8). Two-decimal `"%.2f"` Locale.ENGLISH, `,`→`.`.
Enforced (1) on odds-field focus loss in `OutcomeRow`, (2) in
`saveToHistory` via `withNormalizedOdds()`, (3) in `reopenFromHistory` via
`withNormalizedOdds()`. Display uses `formatOddsForDisplay` (blank → `"—"`).

---

## 7. DESIGN SYSTEM

### 7.1 Color tokens
See the full table in §4.2 (hex + role). Gradient stops in §6.3.

### 7.2 Typography
`Type.kt` customises only Material3 `bodyLarge` (FontFamily.Default, Normal,
16sp, lineHeight 24sp, letterSpacing 0.5sp). Everything else is inline:

| Element | Family | Weight | Size | Color / other |
| --- | --- | --- | --- | --- |
| Splash app name | default | Bold | 36sp | white |
| Form top-bar title | default | ExtraBold | 26sp | onSurface, maxLines 1, ellipsis |
| CTA & SAVE label | FontFamily.Default | Bold | 18sp | `ButtonLabel` |
| History card event name | default | Bold | 19sp | `TextPrimary`, centered, ellipsis |
| History line 2 — date | default | SemiBold | 13sp | `TextSecondary` |
| History line 2 — "No date" | default | Italic | 13sp | `TextTertiary` |
| History line 2 — " · betType" | default | Normal | 13sp | `TextTertiary` |
| `OUTCOMES` header | default | Bold | 16sp | `TextSecondary`, letterSpacing 1.5sp, centered |
| OddsChip bookmaker | default | Light | 11sp | `TextTertiary`, ellipsis |
| OddsChip odds | default | Bold | 18sp | `TextPrimary` |
| OddsChip label | labelSmall | — | — | `TextSecondary`, ellipsis |
| Add-Outcome label | labelMedium | SemiBold | — | `TextSecondary` |
| Sort label | labelMedium | — | — | `TextSecondary` |
| "+N" overflow | labelSmall | — | — | `TextSecondary` |
| RtpDisplay sub-labels | labelSmall | Normal | — | `TextSecondary` |
| RtpDisplay RTP value | default | ExtraBold | 22sp | gradient color |
| RtpDisplay margin value | default | Medium | 14sp | gradient color |
| Helper "Fill in all odds…" | default | Normal | 12sp | `TextTertiary`, centered |
| "No analyses yet" | default | Normal | 14sp | `TextTertiary`, centered |
| Text fields (text/label/placeholder) | inherited (bodyLarge 16sp) | Normal | 16sp | `TextPrimary` / `TextSecondary` / `TextTertiary` |

### 7.3 Corner radius
| Element | Radius |
| --- | --- |
| Analysis `Card` | 24dp |
| History `Card` | 24dp |
| OddsChip `Surface` | 20dp |
| Add-Outcome pill clip | 20dp |
| Sort-toggle clip | 12dp |
| Event/Date/Time/BetType `OutlinedTextField` | 16dp |
| `CompactField` container | 14dp |
| CTA / SAVE buttons | 12dp |
| RtpDisplay `Surface` | 12dp |

### 7.4 Spacing & padding
* Card inner padding: `horizontal 16dp, vertical 12dp` (both analysis & history).
* Analysis form vertical rhythm: 6dp between Event→Date→Time→BetType; 8dp before
  `OUTCOMES`; 10dp after it; 4dp between OutcomeRows; 6dp before RtpDisplay/helper;
  10dp before SAVE.
* OutcomeRow: `Arrangement.spacedBy(4.dp)`, weights **1.7 / 1.3 / 1.5**, +28dp
  delete/spacer slot (icon 16dp inside 28dp button).
* CompactField content padding: `horizontal 8dp, vertical 16dp`.
* History `LazyColumn` contentPadding: start/end 12dp, top 8dp, bottom
  `8dp + 82dp` (82dp = sticky CTA clearance via `bottomContentPadding`);
  `spacedBy(6.dp)` between cards.
* History card: 10dp between header and chip row; chip row `spacedBy(8.dp)`;
  10dp before RtpDisplay; chip inner padding `horizontal 8dp / vertical 2dp`.
* Sort row: `horizontal 12dp / vertical 4dp`; inner clickable `8dp / 4dp`.
* Bottom CTA: `start=end=12dp, bottom=10dp`, height 56dp; SAVE height 56dp.
* RtpDisplay row vertical padding 12dp; divider 40dp tall.

### 7.5 Dark theme rules
* Dark-only via `darkColorScheme(...)` (§4.3). No light theme, no dynamic color;
  system light/dark setting has no effect.
* Pre-Compose `windowBackground` forced black in `Theme.RTPCalculator`.
* `enableEdgeToEdge()` in `onCreate`; the form `Scaffold` uses
  `contentWindowInsets = WindowInsets(0)` so the banner sits flush under the bar.
* Date/Time dialogs inherit Material3 dark colors; confirm button =
  `colorScheme.primary` (`#FF7A5C`), cancel = `TextSecondary`.

---

## 8. ADMOB CONFIGURATION

| Item | Value |
| --- | --- |
| AdMob App ID (manifest meta-data `com.google.android.gms.ads.APPLICATION_ID`) | `ca-app-pub-2303541955549784~7410006753` |
| Production Banner Unit ID | `ca-app-pub-2303541955549784/4795362417` |
| Test Banner Unit ID (Google sample) | `ca-app-pub-3940256099942544/6300978111` |
| SDK | `com.google.android.gms:play-services-ads:23.6.0` |
| Format | Single `AdSize.BANNER`, 50dp height, full width |
| Init | `MobileAds.initialize(this)` once in `MainActivity.onCreate` |

**Switching implementation** (`MainActivity.kt`, file scope):
```kotlin
private const val PROD_AD_UNIT_ID = "ca-app-pub-2303541955549784/4795362417"
private const val TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
private val AD_UNIT_ID = if (BuildConfig.DEBUG) TEST_AD_UNIT_ID else PROD_AD_UNIT_ID
```
`buildConfig = true` in `app/build.gradle.kts` generates `BuildConfig`. The
`release` build type does **not** set `isDebuggable`, so AGP sets
`BuildConfig.DEBUG = false` → release uses the **production** unit; debug builds
use the Google test unit. The App ID and the production unit share publisher
`2303541955549784` (same AdMob account); the test unit uses Google's sample
publisher `3940256099942544`. **Never publish a build carrying the test unit.**

---

## 9. DEVELOPMENT ENVIRONMENT

> Items in this section are not derivable from source code. Machine roles,
> tools, devices and the resume procedure are carried forward from the prior
> V1 documentation; **bracketed `[TO BE CONFIRMED]` items must be filled in by
> the maintainer and were not invented.**

### 9.1 Machines
| Machine | Role | Project path | Specs |
| --- | --- | --- | --- |
| MSI laptop | Primary dev: coding, Gradle builds, Android Studio, ADB to device | `C:\RTP-Calculator\` | `[TO BE CONFIRMED]` |
| Surface tablet | Mobile coding, light edits/reviews | `C:\RTP-Calculator\` | `[TO BE CONFIRMED]` |
| Desktop PC | Secondary coding workstation | `C:\RTP-Calculator\` | `[TO BE CONFIRMED]` |

`local.properties` is machine-specific and not committed (prior MSI value:
`sdk.dir=C:\\Users\\Julien\\AppData\\Local\\Android\\Sdk`).

### 9.2 Physical test devices
| Device | Notes |
| --- | --- |
| Samsung Galaxy S9 | Lower-end / older Android baseline. |
| Samsung Galaxy S23+ | Modern high-end baseline. |

AdMob and the Play in-app review flow are validated on physical devices (no
emulator used for QA).

### 9.3 Tools (with versions)
| Tool | Version |
| --- | --- |
| Android Studio | Compatible with AGP 8.13.x (Ladybug Feature Drop or newer) — `[exact build TO BE CONFIRMED]` |
| Android SDK Platform | API 35 |
| Android SDK Build-Tools | As required by AGP 8.13.2 (Studio-managed) |
| Android Gradle Plugin | 8.13.2 |
| Gradle | 8.13 (wrapper-provisioned) |
| Kotlin | 1.9.25 |
| Compose Compiler ext | 1.5.15 |
| JDK (Gradle daemon) | 21 (Foojay auto-provisioned) |
| Java compile toolchain | 11 |
| Node.js | Any LTS, only for `generate_icons.mjs` (`npm install` → `sharp ^0.34.5`) |
| Git | Source control. No remote URL recorded in V1 (manual machine-to-machine moves). |
| AdMob console | Account `ca-app-pub-2303541955549784` |
| Google Play Console | For the published listing |

### 9.4 Keystore
**Not present in the repository and not recorded in prior docs.** No
`signingConfig` is declared in `app/build.gradle.kts`; release signing is done
externally (Play App Signing / Android Studio "Generate Signed Bundle/APK").

| Field | Value |
| --- | --- |
| Keystore file location | `[TO BE FILLED IN — not in repo]` |
| Key alias | `[TO BE FILLED IN — not in repo]` |
| Passwords | **Never store in this doc or the repo.** |

> Action for the maintainer: record the keystore path and alias here (passwords
> excluded) so release builds are reproducible across machines.

### 9.5 Resume a session on a fresh machine
1. Install Android Studio (latest stable, AGP 8.13.x-compatible).
2. SDK Manager: Android SDK Platform **35**, latest Build-Tools, Platform-Tools (`adb`).
3. Install JDK 21 (Adoptium/Foojay) to avoid first-run provisioning delay.
4. Install Git; clone/copy the project to `C:\RTP-Calculator` (no remote in V1).
5. Create `C:\RTP-Calculator\local.properties` with `sdk.dir=<your SDK path>`.
6. Open in Android Studio; let Gradle sync (wrapper fetches Gradle 8.13 + deps).
7. (Optional) Install Node.js + `npm install` to run `node generate_icons.mjs`.
8. Plug in a physical device (USB debugging) — sanctioned test target (S9 / S23+).
   Run `app`: splash → empty history with banner + CTA.
9. Debug builds use the AdMob **test** unit automatically. Never publish a debug/test-unit build.
10. In-app review only works via Play internal testing (`requestReviewFlow()` is a no-op otherwise).
11. Release: `Build > Generate Signed Bundle / APK` with the external keystore (§9.4).
    Confirm `BuildConfig.DEBUG=false` ⇒ production ad unit (§8).

---

## 10. KEY DECISIONS LOG

* **No Navigation-Compose.** Two visible "screens" only → `showForm` Boolean +
  vertical-slide `AnimatedVisibility`. Reopen = clear `analyses`, push entry,
  `showForm=true`; discard via `closeAndDiscard`.
* **Single Activity, single `BetViewModel`.** ViewModel owns both `analyses`
  and `history`; splitting added no V1 value.
* **One full-screen analysis at a time.** UI only surfaces `analyses.firstOrNull()`;
  CTA and reopen both `clearAnalyses()` first — deliberate predictability constraint.
* **Gson + SharedPreferences over Room/DataStore.** Single list, no schema
  evolution needed for V1; whole model serialises for free.
* **Dark-only theme.** Simplifies design, matches betting-app aesthetic.
* **Live RTP: all-filled in the form, but ≥2 valid in history.** Avoids flashing
  partial calculations while typing; saved entries are already normalised.
* **Odds normalised at 3 layers** (focus-loss + save + reopen) so persisted JSON
  reliably holds `"X.XX"`.
* **In-app review on exactly the 5th launch.** Standard Play recommendation for
  utilities; earlier feels spammy, later loses reviews.
* **AdMob unit via `BuildConfig.DEBUG`.** Zero risk of shipping the test unit
  (release ⇒ DEBUG=false); zero risk of loading real ads while debugging.
* **Transparent overlay on date/time fields.** `OutlinedTextField(readOnly=true)`
  still consumes touches/shows a cursor; an indication-less `clickable` `Box`
  opens the picker; overlay shrinks 48dp to keep the clear "×" tappable.
* **`scrollIntoViewOnFocus()` (300 ms `BringIntoViewRequester`).** Compose's
  auto-IME behavior is unreliable for fields deep in long cards with `adjustResize`.
* **`isMinifyEnabled = false`.** Avoids R8/reflection issues with AdMob/Gson;
  APK size not a V1 concern. No ProGuard rules written/tested.
* **Adaptive-icon bg `#111111` vs app bg `#0D0D0D`.** Slightly lighter icon
  background so the logo reads on pure-black home screens; darker app interior
  for OLED contrast.
* **Sort state not persisted.** Kept in-composable to avoid touching prefs for a
  pure UX preference; flagged for V2.
* **Four locales (en/fr/es/de).** Spanish and German added after the original
  two-locale V1; full 35-key parity maintained.
* **Physical-device QA over emulator.** AdMob + Play review validate best on
  real hardware (Galaxy S9 + S23+).

### 10.1 Decisions needing review (code/spec mismatches)
* **RTP gradient top stop at 95, not 97.** `RtpGradient.kt` lerps yellow→green
  over 92–95 and is flat green ≥95; the spec brief said 92–97 / ≥97. Decide
  whether 95 is intended or a bug (see §11). Documented as-is per code.
* **`new_analysis` string unused.** Defined in all 4 locales but never
  referenced (the form title uses `new_bet_analysis`). Keep or remove in V2.

---

## 11. KNOWN LIMITATIONS

* **RTP gradient threshold mismatch** — code uses 95 as the green cutoff, not
  97; no explicit ">100% surebet" color (falls into flat green). Unverified
  whether intended.
* **Sort preference not persisted** — resets to newest-first on relaunch.
* **No history search / filter / pagination** — `LazyColumn` just grows; entire
  list deserialised at startup.
* **No undo on delete** — history trash icon deletes immediately, rewrites JSON,
  no confirmation/Snackbar.
* **No undo on form-discard** — × / back drops the in-progress entry silently;
  reopening also clears any other in-progress entry.
* **No empty-form protection** — SAVE always enabled; an all-blank entry can be
  persisted (RtpDisplay just won't show).
* **Light theme unsupported**; **no portrait/landscape adaptation** (orientation
  not locked; landscape works but is cramped, untested).
* **Accessibility gaps** — transparent `clickable` overlays on date/time fields
  are invisible to TalkBack; some `Box`es lack `contentDescription`.
* **DatePicker/TimePicker localization** follows the *device* locale, which can
  differ from the app's UI locale.
* **`readOnly` field + overlay** is a workaround; some OEM skins may briefly
  flash a cursor.
* **`colors.xml` empty**; **`Theme.RTPCalculator` parent says `Light`** (cosmetic
  only — only `windowBackground` overridden).
* **No real automated tests** — only Android Studio template test classes.
* **`isMinifyEnabled = false`** — larger APK; no tested ProGuard rules.
* **No analytics / crash reporting** (no Crashlytics, no GA4).
* **No `signingConfig` in Gradle** — release signing is entirely external/manual;
  keystore path & alias are not recorded anywhere in the project (see §9.4).
* **`new_analysis` string is dead** (defined ×4 locales, unused).

---

## 12. V2 BACKLOG

* [ ] Resolve the RTP gradient 95-vs-97 question; consider an explicit
      surebet (>100%) color and not just the ⭐.
* [ ] Persist sort-order preference (DataStore).
* [ ] Per-card "+N" expansion to view all outcomes inline.
* [ ] History search/filter by event name, bet type, bookmaker.
* [ ] Confirm-on-delete (Snackbar + undo) for history entries.
* [ ] Confirm-on-discard for unsaved analyses with ≥1 filled field.
* [ ] Validate/disable SAVE when RTP cannot be computed.
* [ ] Light theme + system-theme follow.
* [ ] Landscape-aware layout.
* [ ] Accessibility pass: contentDescriptions on overlay targets, semantics on
      date/time triggers, larger touch targets.
* [ ] More locales (Italian, Portuguese, …); review device-vs-app locale on pickers.
* [ ] Real unit tests: `calculateRtp`, `sortHistory`, `parseDateToMillis`,
      `parseDateTimeToMillis`, `formatOddsTwoDecimals`, `rtpGradientColor`;
      Compose UI tests for form + history.
* [ ] Replace SharedPreferences+Gson with Room/Proto-DataStore when schema must
      evolve (stake/EV fields).
* [ ] Kotlin 2.x + Compose Compiler Gradle plugin.
* [ ] Enable R8 + write/test ProGuard rules (AdMob, Gson, Play Review).
* [ ] Add a `signingConfig` (or document Play App Signing) and record keystore
      path/alias in §9.4.
* [ ] Firebase Crashlytics + GA4 events (analysis saved, surebet detected, …).
* [ ] Surebet history/aggregator view.
* [ ] Stake / Kelly / EV calculator on top of the RTP engine.
* [ ] Export/import history (JSON share-intent).
* [ ] Remove or wire up the dead `new_analysis` string.
* [ ] App-icon refresh & V2 marketing assets.
* [ ] Second monetization slot (interstitial on save / rewarded unlock) — weigh UX.
```
