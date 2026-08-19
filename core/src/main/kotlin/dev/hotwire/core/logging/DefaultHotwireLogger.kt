package dev.hotwire.core.logging

import android.util.Log

internal object DefaultHotwireLogger : HotwireLogger {
    override var logLevel = HotwireLogLevel.NONE

    override fun v(tag: String, msg: () -> String) {
        if (logLevel.priority <= HotwireLogLevel.VERBOSE.priority) {
            Log.v(tag, msg())
        }
    }

    override fun d(tag: String, msg: () -> String) {
        if (logLevel.priority <= HotwireLogLevel.DEBUG.priority) {
            Log.d(tag, msg())
        }
    }

    override fun i(tag: String, msg: () -> String) {
        if (logLevel.priority <= HotwireLogLevel.INFO.priority) {
            Log.i(tag, msg())
        }
    }

    override fun w(tag: String, msg: () -> String) {
        if (logLevel.priority <= HotwireLogLevel.WARN.priority) {
            Log.w(tag, msg())
        }
    }

    override fun e(tag: String, throwable: Throwable?, msg: () -> String) {
        if (logLevel.priority <= HotwireLogLevel.ERROR.priority) {
            Log.e(tag, msg(), throwable)
        }
    }
}
