package com.rtpcalculator.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.rtpcalculator.data.HistoryRepository
import com.rtpcalculator.model.BetAnalysis
import com.rtpcalculator.model.OddsFormat
import com.rtpcalculator.model.Outcome
import com.rtpcalculator.ui.formatOddsTwoDecimals
import com.rtpcalculator.ui.toDecimalOddsOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private fun BetAnalysis.withNormalizedOdds(): BetAnalysis =
    copy(outcomes = outcomes.map { it.copy(odds = formatOddsTwoDecimals(it.odds)) })

class BetViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = HistoryRepository(app)
    private val prefs = app.getSharedPreferences("rtp_prefs", Context.MODE_PRIVATE)

    private val _analyses = MutableStateFlow<List<BetAnalysis>>(emptyList())
    val analyses: StateFlow<List<BetAnalysis>> = _analyses.asStateFlow()

    private val _history = MutableStateFlow<List<BetAnalysis>>(emptyList())
    val history: StateFlow<List<BetAnalysis>> = _history.asStateFlow()

    init {
        _history.value = repo.load()
    }

    fun addAnalysis() {
        _analyses.value = _analyses.value + newAnalysis()
    }

    fun clearAnalyses() {
        _analyses.value = emptyList()
    }

    fun deleteAnalysis(id: String) {
        _analyses.value = _analyses.value.filter { it.id != id }
    }

    fun saveToHistory(id: String) {
        val analysis = _analyses.value.find { it.id == id } ?: return
        val saved = analysis.withNormalizedOdds().copy(savedAt = System.currentTimeMillis())
        val updated = _history.value.filter { it.id != id } + saved
        _history.value = updated
        repo.save(updated)
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

    fun updateOutcomeOdds(analysisId: String, outcomeId: String, odds: String) =
        update(analysisId) { a ->
            a.copy(outcomes = a.outcomes.map { o ->
                if (o.id == outcomeId) o.copy(odds = odds) else o
            })
        }

    fun updateOutcomeBookmaker(analysisId: String, outcomeId: String, bookmaker: String) =
        update(analysisId) { a ->
            a.copy(outcomes = a.outcomes.map { o ->
                if (o.id == outcomeId) o.copy(bookmaker = bookmaker) else o
            })
        }

    fun updateOutcomeFormat(analysisId: String, outcomeId: String, format: OddsFormat) {
        updateOutcome(analysisId, outcomeId) { it.copy(oddsFormat = format).recomputeOdds() }
        saveLastFormat(format)
    }

    fun updateOutcomeFracNumerator(analysisId: String, outcomeId: String, value: String) =
        updateOutcome(analysisId, outcomeId) { it.copy(fracNumerator = value).recomputeOdds() }

    fun updateOutcomeFracDenominator(analysisId: String, outcomeId: String, value: String) =
        updateOutcome(analysisId, outcomeId) { it.copy(fracDenominator = value).recomputeOdds() }

    fun updateOutcomeAmericanValue(analysisId: String, outcomeId: String, value: String) =
        updateOutcome(analysisId, outcomeId) { it.copy(americanValue = value).recomputeOdds() }

    fun toggleOutcomeAmericanSign(analysisId: String, outcomeId: String) =
        updateOutcome(analysisId, outcomeId) {
            it.copy(americanNegative = !it.americanNegative).recomputeOdds()
        }

    fun addOutcome(analysisId: String) =
        update(analysisId) { it.copy(outcomes = it.outcomes + newOutcome()) }

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

    /** Keeps decimal `odds` in sync with the active format's raw input. */
    private fun Outcome.recomputeOdds(): Outcome = when (oddsFormat) {
        OddsFormat.DECIMAL -> this // odds holds the raw user input, kept as today
        else -> copy(odds = toDecimalOddsOrNull() ?: "")
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
