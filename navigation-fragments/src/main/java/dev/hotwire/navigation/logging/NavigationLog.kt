package dev.hotwire.navigation.logging

import android.util.Log
import dev.hotwire.core.config.Hotwire

internal object NavigationLog {
    private const val DEFAULT_TAG = "Hotwire-Navigation"

    private val debugEnabled get() = Hotwire.config.debugLoggingEnabled

    internal fun d(msg: String) = log(Log.DEBUG, msg)

    internal fun w(msg: String) = log(Log.WARN, msg)

    internal fun e(msg: String) = log(Log.ERROR, msg)

    private fun log(logLevel: Int, msg: String) {
        when (logLevel) {
            Log.DEBUG -> if (debugEnabled) Log.d(DEFAULT_TAG, msg)
            Log.WARN -> Log.w(DEFAULT_TAG, msg)
            Log.ERROR -> Log.e(DEFAULT_TAG, msg)
        }
    }
}

private const val PAD_END_LENGTH = 35

internal fun logEvent(event: String, details: String = "") {
    NavigationLog.d("$event ".padEnd(PAD_END_LENGTH, '.') + " [$details]")
}

internal fun logEvent(event: String, attributes: List<Pair<String, Any>>) {
    val description = attributes.joinToString(prefix = "[", postfix = "]", separator = ", ") {
        "${it.first}: ${it.second}"
    }
    NavigationLog.d("$event ".padEnd(PAD_END_LENGTH, '.') + " $description")
}

internal fun logWarning(event: String, details: String) {
    NavigationLog.w("$event ".padEnd(PAD_END_LENGTH, '.') + " [$details]")
}

internal fun logError(event: String, error: Exception) {
    NavigationLog.e("$event: ${error.stackTraceToString()}")
}
