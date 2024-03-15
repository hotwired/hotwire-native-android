package dev.hotwire.core.bridge

import android.os.Handler
import android.os.Looper

/**
 * Guarantees main thread execution, posting a Runnable on
 * the main Looper if necessary. This allows compatibility
 * with unit tests that are already on the main thread.
 */
internal fun runOnUiThread(func: () -> Unit) {
    when (val mainLooper = Looper.getMainLooper()) {
        Looper.myLooper() -> func()
        else -> Handler(mainLooper).post { func() }
    }
}
