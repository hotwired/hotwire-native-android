package dev.hotwire.core.logging

import android.util.Log
import dev.hotwire.core.config.Hotwire

internal object DefaultHotwireLogger : HotwireLogger {
    private val debugEnabled get() = Hotwire.config.debugLoggingEnabled

    override fun d(tag: String, msg: String) = log(Log.DEBUG, tag, msg)

    override fun w(tag: String, msg: String) = log(Log.WARN, tag, msg)

    override fun e(tag: String, msg: String) = log(Log.ERROR, tag, msg)

    private fun log(logLevel: Int, tag: String, msg: String) {
        when (logLevel) {
            Log.DEBUG -> if (debugEnabled) Log.d(tag, msg)
            Log.WARN -> Log.w(tag, msg)
            Log.ERROR -> Log.e(tag, msg)
        }
    }
}
