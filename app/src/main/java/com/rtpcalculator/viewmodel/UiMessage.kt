package com.rtpcalculator.viewmodel

/**
 * Transient one-shot messages emitted by [BetViewModel] for the UI to render (e.g. a Snackbar
 * after a history action). Closed enum — the UI layer maps each value to a localised string
 * resource so the ViewModel stays free of Android-string references.
 */
enum class UiMessage {
    ShareFailed
}
