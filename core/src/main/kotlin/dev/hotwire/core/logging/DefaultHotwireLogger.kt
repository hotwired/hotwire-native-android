package dev.hotwire.core.logging

import android.util.Log
import dev.hotwire.core.BuildConfig

internal object DefaultHotwireLogger : HotwireLogger {
    private val debugEnabled get() = BuildConfig.DEBUG

    override fun v(tag: String, msg: String) =
        log(Log.VERBOSE, tag, msg)

    override fun d(tag: String, msg: String) =
        log(Log.DEBUG, tag, msg)

    override fun i(tag: String, msg: String) =
        log(Log.INFO, tag, msg)

    override fun w(tag: String, msg: String) =
        log(Log.WARN, tag, msg)

    override fun e(tag: String, msg: String, throwable: Throwable?) =
        log(Log.ERROR, tag, msg, throwable)

    private fun log(logLevel: Int, tag: String, msg: String, throwable: Throwable? = null) {
        when (logLevel) {
            Log.DEBUG -> if (debugEnabled) Log.d(tag, msg)
            Log.VERBOSE -> if (debugEnabled) Log.v(tag, msg)
            Log.INFO -> if (debugEnabled) Log.i(tag, msg)
            Log.WARN -> Log.w(tag, msg)
            Log.ERROR -> Log.e(tag, msg, throwable)
        }
    }
}
