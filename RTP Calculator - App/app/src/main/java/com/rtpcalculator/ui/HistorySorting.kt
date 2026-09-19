package com.rtpcalculator.ui

import com.rtpcalculator.model.BetAnalysis

fun sortHistory(history: List<BetAnalysis>, newestFirst: Boolean): List<BetAnalysis> {
    val (noDate, withDate) = history.partition { it.date.isBlank() }
    // No-date group: its INTERNAL ordering follows the toggle too, so flipping `newestFirst`
    // visually swaps every adjacent pair (dated pairs, duplicate pairs, no-date pairs) in
    // unison — the "mirror effect" the user expects.
    val sortedNoDate = if (newestFirst) noDate.sortedByDescending { it.savedAt }
                       else noDate.sortedBy { it.savedAt }

    return if (newestFirst) {
        // Newest event-date first; within the same date+time, older save first → the
        // original appears above its duplicate.
        val sortedWithDate = withDate.sortedWith(
            compareByDescending<BetAnalysis> { parseDateTimeToMillis(it.date, it.time) ?: Long.MIN_VALUE }
                .thenBy { it.savedAt }
        )
        sortedWithDate + sortedNoDate
    } else {
        // Oldest event-date first; within the same date+time, newer save first → the
        // duplicate now sits above its original (mirror of the newestFirst case).
        val sortedWithDate = withDate.sortedWith(
            compareBy<BetAnalysis> { parseDateTimeToMillis(it.date, it.time) ?: Long.MAX_VALUE }
                .thenByDescending { it.savedAt }
        )
        sortedNoDate + sortedWithDate
    }
}
