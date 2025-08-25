package dev.hotwire.core.logging

import android.R.attr.enabled
import android.util.Log
import dev.hotwire.core.BuildConfig

internal object DefaultHotwireLogger : HotwireLogger {
    private val debugEnabled get() = BuildConfig.DEBUG

    override fun v(tag: String, msg: () -> String) {
        if (debugEnabled) {
            Log.v(tag, msg())
        }
    }

    override fun d(tag: String, msg: () -> String) {
        if (debugEnabled) {
            Log.d(tag, msg())
        }
    }

    override fun i(tag: String, msg: () -> String) {
        if (debugEnabled) {
            Log.i(tag, msg())
        }
    }

    override fun w(tag: String, msg: () -> String) {
        if (debugEnabled) {
            Log.w(tag, msg())
        }
    }

    override fun e(tag: String, throwable: Throwable?, msg: () -> String) {
        if (debugEnabled) {
            Log.e(tag, msg(), throwable)
        }
    }
}
