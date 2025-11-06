package dev.hotwire.core.logging

import dev.hotwire.core.config.Hotwire
import okhttp3.logging.HttpLoggingInterceptor

private const val DEFAULT_TAG = "Hotwire-Core"
private const val PAD_END_LENGTH = 35

internal class HotwireHttpLogger : HttpLoggingInterceptor.Logger {
    override fun log(message: String) {
        Hotwire.config.logger.d(DEFAULT_TAG) { message }
    }
}

internal fun logVerbose(event: String, details: String = "") {
    Hotwire.config.logger.v(DEFAULT_TAG) {
        "${"$event ".padEnd(PAD_END_LENGTH, '.')} [$details]"
    }
}

internal fun logEvent(event: String, details: String = "") {
    Hotwire.config.logger.d(DEFAULT_TAG) {
        "${"$event ".padEnd(PAD_END_LENGTH, '.')} [$details]"
    }
}

internal fun logEvent(event: String, attributes: List<Pair<String, Any>>) {
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