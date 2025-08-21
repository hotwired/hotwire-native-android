package dev.hotwire.core.logging

import dev.hotwire.core.config.Hotwire

private const val DEFAULT_TAG = "Hotwire-Core"
private const val PAD_END_LENGTH = 35

internal fun logEvent(event: String, details: String = "") {
    Hotwire.config.logger.d(DEFAULT_TAG, "$event ".padEnd(PAD_END_LENGTH, '.') + " [$details]")
}

internal fun logEvent(event: String, attributes: List<Pair<String, Any>>) {
    val description = attributes.joinToString(prefix = "[", postfix = "]", separator = ", ") {
        "${it.first}: ${it.second}"
    }
    Hotwire.config.logger.d(DEFAULT_TAG, "$event ".padEnd(PAD_END_LENGTH, '.') + " $description")
}

internal fun logWarning(event: String, details: String) {
    Hotwire.config.logger.w(DEFAULT_TAG, "$event ".padEnd(PAD_END_LENGTH, '.') + " [$details]")
}

internal fun logError(event: String, throwable: Throwable) {
    Hotwire.config.logger.e(DEFAULT_TAG, "$event: ${throwable.stackTraceToString()}", throwable)
}