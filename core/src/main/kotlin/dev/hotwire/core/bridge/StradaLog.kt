package dev.hotwire.core.bridge

import android.util.Log

@Suppress("unused")
internal object StradaLog {
    private const val DEFAULT_TAG = "StradaLog"

    private val debugEnabled get() = Strada.config.debugLoggingEnabled

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

internal fun logEvent(event: String, details: String = "") {
    StradaLog.d("$event ".padEnd(35, '.') + " [$details]")
}

internal fun logWarning(event: String, details: String) {
    StradaLog.w("$event ".padEnd(35, '.') + " [$details]")
}

internal fun logError(event: String, error: Exception) {
    StradaLog.e("$event: ${error.stackTraceToString()}")
}
