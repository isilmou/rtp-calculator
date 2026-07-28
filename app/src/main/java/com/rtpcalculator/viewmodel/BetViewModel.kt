package com.rtpcalculator.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rtpcalculator.data.HistoryRepository
import com.rtpcalculator.model.BetAnalysis
import com.rtpcalculator.model.OddsFormat
import com.rtpcalculator.model.Outcome
import com.rtpcalculator.ui.decimalToAmerican
import com.rtpcalculator.ui.decimalToFraction
import com.rtpcalculator.ui.exactDecimalString
import com.rtpcalculator.ui.formatDecimalUpTo3
import com.rtpcalculator.ui.formatOddsTwoDecimals
import com.rtpcalculator.ui.padDecimalToMinTwo
import com.rtpcalculator.ui.rawToDecimalExactOrNull
import com.rtpcalculator.ui.sanitizeDecimalInput
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

private fun BetAnalysis.withNormalizedOdds(): BetAnalysis =
    copy(outcomes = outcomes.map { o ->
        // Recompute the calc `odds` from the active format's raw fields at full precision so
        // it stays consistent with what's displayed (and the TRJ keeps its precision across
        // save/reopen). Falls back to the legacy 2-dp value when raw fields are incomplete.
        val exact = rawToDecimalExactOrNull(
            o.oddsFormat, o.decimalRaw, o.fracNumerator, o.fracDenominator,
            o.americanNegative, o.americanValue
        )
        // Safety: also pad the EU display string to ≥ 2 decimals here, so saves never persist
        // a "2" or "1.5" (e.g. when Save is tapped before the field has been blurred).
        val paddedDecimalRaw =
            if (o.oddsFormat == OddsFormat.DECIMAL) padDecimalToMinTwo(o.decimalRaw)
            else o.decimalRaw
        o.copy(
            decimalRaw = paddedDecimalRaw,
            odds = exact?.let { exactDecimalString(it) } ?: formatOddsTwoDecimals(o.odds)
        )
    })

class BetViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = HistoryRepository(app)
    private val prefs = app.getSharedPreferences("rtp_prefs", Context.MODE_PRIVATE)

    private val _analyses = MutableStateFlow<List<BetAnalysis>>(emptyList())
    val analyses: StateFlow<List<BetAnalysis>> = _analyses.asStateFlow()

    private val _history = MutableStateFlow<List<BetAnalysis>>(emptyList())
    val history: StateFlow<List<BetAnalysis>> = _history.asStateFlow()

    // One-shot UI events (Snackbar messages, etc.). extraBufferCapacity = 1 so tryEmit never
    // drops a single event if the collector hasn't grabbed the previous one yet.
    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 1)
    val messages: SharedFlow<UiMessage> = _messages.asSharedFlow()

    // ID of an analysis that should be scrolled into view in the history list (after Save
    // or Duplicate). The collector (HistoryScreen) waits for the id to appear in the sorted
    // list, then animateScrollToItem if it isn't already visible.
    private val _scrollToHistoryId = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val scrollToHistoryId: SharedFlow<String> = _scrollToHistoryId.asSharedFlow()

    // ID of the outcome whose Issue field should auto-focus on next composition (e.g. just
    // after "Add outcome"). The matching OutcomeRow requests focus + IME, then calls
    // [consumeFocusOutcomeId] so the request fires exactly once.
    private val _focusOutcomeId = MutableStateFlow<String?>(null)
    val focusOutcomeId: StateFlow<String?> = _focusOutcomeId.asStateFlow()

    fun consumeFocusOutcomeId() {
        _focusOutcomeId.value = null
    }

    init {
        // Load history OFF the main thread (Gson deserialization + legacy-defaults mapping is the
        // main cold-start blocker). The list is empty for the first few ms (the "no analyses yet"
        // state may flash briefly with a large history) until the IO load completes and we publish
        // it back on the main thread.
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) { repo.load() }
            _history.value = loaded
        }
    }

    fun addAnalysis() {
        _analyses.value = _analyses.value + newAnalysis()
    }

    fun clearAnalyses() {
        _analyses.value = emptyList()
        // Avoid a stale focus request carrying over to a freshly-reopened analysis whose
        // outcome IDs might coincidentally collide (rare with UUIDs, but defensive).
        _focusOutcomeId.value = null
    }

    fun deleteAnalysis(id: String) {
        _analyses.value = _analyses.value.filter { it.id != id }
    }

    fun saveToHistory(id: String) {
        val analysis = _analyses.value.find { it.id == id } ?: return
        // savedAt = creation timestamp, kept STABLE across edits. It is only used by the
        // duplicate-group secondary sort, so editing an existing entry must NOT bump its
        // position. Only a brand-new entry gets a fresh System.currentTimeMillis().
        val existingSavedAt = _history.value.firstOrNull { it.id == id }?.savedAt
        val savedAt = existingSavedAt ?: System.currentTimeMillis()
        val saved = analysis.withNormalizedOdds().copy(savedAt = savedAt)
        val updated = _history.value.filter { it.id != id } + saved
        _history.value = updated
        repo.save(updated)
        _scrollToHistoryId.tryEmit(saved.id)
    }

    fun reopenFromHistory(id: String) {
        val analysis = _history.value.find { it.id == id } ?: return
        val alreadyOpen = _analyses.value.any { it.id == id }
        if (!alreadyOpen) {
            _analyses.value = _analyses.value + analysis.withNormalizedOdds()
        }
    }

    fun deleteFromHistory(id: String) {
        val updated = _history.value.filter { it.id != id }
        _history.value = updated
        repo.save(updated)
    }

    /** Reports that the share-image pipeline failed (raised by the UI layer). */
    fun notifyShareFailed() {
        _messages.tryEmit(UiMessage.ShareFailed)
    }

    /**
     * Instantly duplicates a saved analysis: same content (event, outcomes with all their raw
     * format fields), but fresh UUIDs for the analysis and EACH outcome (avoid Compose key
     * collisions and history-key clashes).
     *
     * savedAt placement: the copy must land JUST BELOW its original ON SCREEN, in whatever sort
     * mode is active at duplication time ([newestFirst]). The group is the analyses sharing the
     * same `date` string (blank groups together). Within a group the secondary sort by savedAt
     * MIRRORS with the toggle, AND differs between dated and no-date groups (see HistorySorting):
     *   - dated   : newestFirst → savedAt ASC (below = larger) ; oldestFirst → DESC (below = smaller)
     *   - no-date : newestFirst → savedAt DESC (below = smaller); oldestFirst → ASC  (below = larger)
     * So we seek the SMALLER-savedAt neighbour exactly when (isNoDate == newestFirst), and pick a
     * savedAt strictly between the original and that immediate on-screen-next neighbour:
     *   - seek smaller: neighbour = largest savedAt < original's → midpoint, or original.savedAt - 1.
     *   - seek larger : neighbour = smallest savedAt > original's → midpoint, or now().
     * Repeated duplications keep stacking right below the original (each new copy's neighbour is
     * the previous copy, so the midpoint shrinks toward the original).
     *
     * NOTE: the target is computed for the CURRENT sort mode. If the user flips the sort after
     * duplicating, the copy's relative position may flip too (it could land above the original).
     * That is an accepted trade-off.
     */
    fun duplicateInHistory(id: String, newestFirst: Boolean) {
        val source = _history.value.find { it.id == id } ?: return

        // Same-group analyses = same `date` string (blank groups with blank).
        val group = _history.value.filter { it.date == source.date }
        // On-screen "below" = smaller savedAt when the group is displayed savedAt-descending.
        // That happens for a no-date group in newestFirst and a dated group in oldestFirst.
        val seekSmaller = source.date.isBlank() == newestFirst
        val copySavedAt = if (seekSmaller) {
            val w = group.filter { it.savedAt < source.savedAt }.maxOfOrNull { it.savedAt }
            if (w == null) source.savedAt - 1 else (source.savedAt + w) / 2
        } else {
            val v = group.filter { it.savedAt > source.savedAt }.minOfOrNull { it.savedAt }
            if (v == null) System.currentTimeMillis() else (source.savedAt + v) / 2
        }

        val copy = source.copy(
            id = UUID.randomUUID().toString(),
            savedAt = copySavedAt,
            outcomes = source.outcomes.map { it.copy(id = UUID.randomUUID().toString()) }
        )
        val updated = _history.value + copy
        _history.value = updated
        repo.save(updated)
        // No Snackbar on duplication: the copy appearing right under the original in the
        // list is feedback enough.
        _scrollToHistoryId.tryEmit(copy.id)
    }

    fun updateDate(analysisId: String, date: String) =
        update(analysisId) {
            if (date.isBlank()) it.copy(date = "", time = "") else it.copy(date = date)
        }

    fun updateTime(analysisId: String, time: String) =
        update(analysisId) { it.copy(time = time) }

    fun updateEventName(analysisId: String, name: String) =
        update(analysisId) { it.copy(eventName = name) }

    fun updateBetType(analysisId: String, betType: String) =
        update(analysisId) { it.copy(betType = betType) }

    fun updateOutcomeLabel(analysisId: String, outcomeId: String, label: String) =
        update(analysisId) { a ->
            a.copy(outcomes = a.outcomes.map { o ->
                if (o.id == outcomeId) o.copy(label = label) else o
            })
        }

    /**
     * Manual DECIMAL (EU) entry. Stores the sanitised raw display string (up to 3 decimals,
     * trailing zeros kept) and refreshes the reference + calc odds from it.
     */
    fun updateOutcomeOdds(analysisId: String, outcomeId: String, odds: String) =
        updateOutcome(analysisId, outcomeId) { o ->
            o.copy(decimalRaw = sanitizeDecimalInput(odds)).afterManualInput()
        }

    fun updateOutcomeBookmaker(analysisId: String, outcomeId: String, bookmaker: String) =
        update(analysisId) { a ->
            a.copy(outcomes = a.outcomes.map { o ->
                if (o.id == outcomeId) o.copy(bookmaker = bookmaker) else o
            })
        }

    /**
     * Format change via the EU/UK/US selector: CONVERT (no longer clear). The new format's raw
     * display is computed from the decimal REFERENCE (last manual entry), never from the value
     * shown in the format being left — so rounding never accumulates. Selecting the already-active
     * format is a no-op.
     */
    fun updateOutcomeFormat(analysisId: String, outcomeId: String, format: OddsFormat) {
        updateOutcome(analysisId, outcomeId) { o ->
            if (o.oddsFormat == format) o else o.convertedTo(format)
        }
        saveLastFormat(format)
    }

    fun updateOutcomeFracNumerator(analysisId: String, outcomeId: String, value: String) =
        updateOutcome(analysisId, outcomeId) { it.copy(fracNumerator = value).afterManualInput() }

    fun updateOutcomeFracDenominator(analysisId: String, outcomeId: String, value: String) =
        updateOutcome(analysisId, outcomeId) { it.copy(fracDenominator = value).afterManualInput() }

    fun updateOutcomeAmericanValue(analysisId: String, outcomeId: String, value: String) =
        updateOutcome(analysisId, outcomeId) { it.copy(americanValue = value).afterManualInput() }

    fun toggleOutcomeAmericanSign(analysisId: String, outcomeId: String) =
        updateOutcome(analysisId, outcomeId) {
            it.copy(americanNegative = !it.americanNegative).afterManualInput()
        }

    fun addOutcome(analysisId: String) {
        val newOutcome = newOutcome()
        update(analysisId) { it.copy(outcomes = it.outcomes + newOutcome) }
        // Drive the auto-focus + IME open on the new Issue field via the matching OutcomeRow.
        _focusOutcomeId.value = newOutcome.id
    }

    fun removeOutcome(analysisId: String, outcomeId: String) =
        update(analysisId) { a ->
            if (a.outcomes.size <= 2) a
            else a.copy(outcomes = a.outcomes.filter { it.id != outcomeId })
        }

    private fun update(analysisId: String, block: (BetAnalysis) -> BetAnalysis) {
        _analyses.value = _analyses.value.map { if (it.id == analysisId) block(it) else it }
    }

    private fun updateOutcome(analysisId: String, outcomeId: String, block: (Outcome) -> Outcome) =
        update(analysisId) { a ->
            a.copy(outcomes = a.outcomes.map { o -> if (o.id == outcomeId) block(o) else o })
        }

    /**
     * Called after a MANUAL edit in the active format. Recomputes:
     *  - refDecimal = exact decimal of this entry (the new conversion reference),
     *  - odds       = high-precision decimal that feeds calculateRtp (matches what's shown).
     * Incomplete/invalid input clears both (no calc), like the previous decimal behaviour.
     */
    private fun Outcome.afterManualInput(): Outcome {
        val exact = rawToDecimalExactOrNull(
            format = oddsFormat,
            decimalRaw = decimalRaw,
            fracNumerator = fracNumerator,
            fracDenominator = fracDenominator,
            americanNegative = americanNegative,
            americanValue = americanValue
        )
        return if (exact == null) copy(refDecimal = "", odds = "")
        else copy(refDecimal = exact.toString(), odds = exactDecimalString(exact))
    }

    /**
     * Converts this outcome's display to [target], starting from [refDecimal] (the last manual
     * entry). `odds` is recomputed from the freshly produced display so the TRJ reflects what is
     * shown (American is rounded to a conventional integer; EU keeps the precise reference for
     * the TRJ while showing up to 3 decimals). The reference itself is never altered here.
     */
    private fun Outcome.convertedTo(target: OddsFormat): Outcome {
        val ref = refDecimal.toDoubleOrNull()
            ?: return copy(
                oddsFormat = target,
                decimalRaw = "", fracNumerator = "", fracDenominator = "",
                americanValue = "", americanNegative = false, odds = ""
            )
        return when (target) {
            OddsFormat.DECIMAL -> copy(
                oddsFormat = target,
                decimalRaw = formatDecimalUpTo3(ref),
                odds = exactDecimalString(ref)
            )
            OddsFormat.FRACTIONAL -> {
                val f = decimalToFraction(ref)
                if (f == null) copy(oddsFormat = target, fracNumerator = "", fracDenominator = "", odds = "")
                else copy(
                    oddsFormat = target,
                    fracNumerator = f.first,
                    fracDenominator = f.second,
                    odds = exactDecimalString(f.first.toDouble() / f.second.toDouble() + 1.0)
                )
            }
            OddsFormat.AMERICAN -> {
                val a = decimalToAmerican(ref)
                if (a == null) copy(oddsFormat = target, americanValue = "", americanNegative = false, odds = "")
                else {
                    val v = a.second.toDouble()
                    val dec = if (a.first) 100.0 / v + 1.0 else v / 100.0 + 1.0
                    copy(
                        oddsFormat = target,
                        americanNegative = a.first,
                        americanValue = a.second,
                        odds = exactDecimalString(dec)
                    )
                }
            }
        }
    }

    private fun newOutcome(): Outcome = Outcome(oddsFormat = lastFormat())

    private fun newAnalysis(): BetAnalysis =
        BetAnalysis(outcomes = listOf(newOutcome(), newOutcome()))

    private fun lastFormat(): OddsFormat =
        prefs.getString(KEY_LAST_FORMAT, null)
            ?.let { runCatching { OddsFormat.valueOf(it) }.getOrNull() }
            ?: OddsFormat.DECIMAL

    private fun saveLastFormat(format: OddsFormat) {
        prefs.edit().putString(KEY_LAST_FORMAT, format.name).apply()
    }

    private companion object {
        const val KEY_LAST_FORMAT = "last_odds_format"
    }
}
