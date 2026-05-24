package com.rtpcalculator.ui

import com.rtpcalculator.model.BetAnalysis

fun sortHistory(history: List<BetAnalysis>, newestFirst: Boolean): List<BetAnalysis> {
    val (noDate, withDate) = history.partition { it.date.isBlank() }
    val sortedNoDate = noDate.sortedByDescending { it.savedAt }

    return if (newestFirst) {
        val sortedWithDate = withDate.sortedWith(
            compareByDescending { parseDateTimeToMillis(it.date, it.time) ?: Long.MIN_VALUE }
        )
        sortedWithDate + sortedNoDate
    } else {
        val sortedWithDate = withDate.sortedWith(
            compareBy { parseDateTimeToMillis(it.date, it.time) ?: Long.MAX_VALUE }
        )
        sortedNoDate + sortedWithDate
    }
}
