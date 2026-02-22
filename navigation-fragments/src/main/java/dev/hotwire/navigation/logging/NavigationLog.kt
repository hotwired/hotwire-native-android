package dev.hotwire.navigation.logging

import dev.hotwire.core.config.Hotwire

private const val DEFAULT_TAG = "Hotwire-Navigation"
private const val PAD_END_LENGTH = 35

internal fun logDebug(event: String, details: String = "") {
    Hotwire.config.logger.d(DEFAULT_TAG) {
        "${"$event ".padEnd(PAD_END_LENGTH, '.')} [$details]"
    }
}

internal fun logDebug(event: String, attributes: List<Pair<String, Any>>) {
    Hotwire.config.logger.d(DEFAULT_TAG) {
        val description = attributes.joinToString(prefix = "[", postfix = "]", separator = ", ") {
            "${it.first}: ${it.second}"
        }

        "${"$event ".padEnd(PAD_END_LENGTH, '.')} $description"
    }
}

internal fun logWarning(event: String, details: String) {
    Hotwire.config.logger.w(DEFAULT_TAG) {
        "${"$event ".padEnd(PAD_END_LENGTH, '.')} [$details]"
    }
}

internal fun logError(event: String, throwable: Throwable) {
    Hotwire.config.logger.e(DEFAULT_TAG, throwable) {
        "$event: ${throwable.stackTraceToString()}"
    }
}
