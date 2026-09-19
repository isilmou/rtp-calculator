package com.rtpcalculator.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rtpcalculator.model.BetAnalysis
import com.rtpcalculator.model.Outcome
import com.rtpcalculator.model.OddsFormat

class HistoryRepository(context: Context) {

    private val prefs = context.getSharedPreferences("rtp_history", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val key = "saved_analyses"

    fun load(): List<BetAnalysis> {
        val json = prefs.getString(key, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<BetAnalysis>>() {}.type
            val parsed: List<BetAnalysis> = gson.fromJson(json, type) ?: emptyList()
            parsed.withLegacyOddsDefaults()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(analyses: List<BetAnalysis>) {
        prefs.edit().putString(key, gson.toJson(analyses)).apply()
    }
}

/**
 * Coerces the odds-format fields of legacy analyses to safe defaults.
 *
 * Gson does not run the Kotlin constructor on deserialization, so analyses saved
 * with the pre-format model have `null` in the new fields (and `oddsFormat`).
 * The nullable casts are deliberate: the declared types are non-null, but the
 * runtime values genuinely can be null here, so we restore the intended defaults.
 */
internal fun List<BetAnalysis>.withLegacyOddsDefaults(): List<BetAnalysis> =
    map { analysis ->
        analysis.copy(outcomes = analysis.outcomes.map { it.withLegacyOddsDefaults() })
    }

private fun Outcome.withLegacyOddsDefaults(): Outcome {
    val fmt = (oddsFormat as OddsFormat?) ?: OddsFormat.DECIMAL
    // Pre-auto-conversion analyses have no decimalRaw/refDecimal: seed them from the stored
    // decimal `odds` so the EU field still shows a value and conversions have a reference.
    val dRaw = (decimalRaw as String?).orEmpty().ifEmpty { if (fmt == OddsFormat.DECIMAL) odds else "" }
    val ref = (refDecimal as String?).orEmpty()
        .ifEmpty { odds.replace(',', '.').toDoubleOrNull()?.toString() ?: "" }
    return copy(
        oddsFormat = fmt,
        fracNumerator = (fracNumerator as String?).orEmpty(),
        fracDenominator = (fracDenominator as String?).orEmpty(),
        americanValue = (americanValue as String?).orEmpty(),
        decimalRaw = dRaw,
        refDecimal = ref
    )
}
