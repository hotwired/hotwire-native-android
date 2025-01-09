package dev.hotwire.navigation.util

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import androidx.fragment.app.Fragment
import dev.hotwire.navigation.logging.logEvent
import dev.hotwire.navigation.views.HotwireView
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine

internal class HotwireViewScreenshot {
    var bitmap: Bitmap? = null
    var screenshotOrientation = 0
    var screenshotZoomed = false

    fun reset() {
        bitmap = null
        screenshotOrientation = 0
        screenshotZoomed = false
    }

    fun showScreenshotIfAvailable(hotwireView: HotwireView, currentlyZoomed: Boolean) {
        if (screenshotOrientation == hotwireView.currentOrientation() &&
            screenshotZoomed == currentlyZoomed
        ) {
            bitmap?.let { hotwireView.addScreenshot(it) }
        }
    }

    suspend fun captureScreenshot(hotwireView: HotwireView, fragment: Fragment, currentlyZoomed: Boolean) {
        bitmap = copyViewToBitmap(hotwireView, fragment)
        screenshotOrientation = hotwireView.currentOrientation()
        screenshotZoomed = currentlyZoomed
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun copyViewToBitmap(hotwireView: HotwireView, fragment: Fragment): Bitmap? {
        return suspendCancellableCoroutine { continuation ->
            if (!hotwireView.isLaidOut || !hasEnoughMemoryForScreenshot() || (hotwireView.width <= 0 || hotwireView.height <= 0)) {
                if (continuation.isActive) {
                    continuation.resume(null, null)
                }
            }

            val start = System.currentTimeMillis()

            val rect = Rect()
            hotwireView.getGlobalVisibleRect(rect)

            val bitmap = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888)

            PixelCopy.request(
                fragment.requireActivity().window,
                rect,
                bitmap,
                { result ->
                    if (result == PixelCopy.SUCCESS) {
                        logEvent(
                            "viewScreenshotCreated", listOf(
                                "size" to "${bitmap.width}x${bitmap.height}",
                                "duration" to "${System.currentTimeMillis() - start}ms",
                            )
                        )
                        if (continuation.isActive) {
                            continuation.resume(bitmap, null)
                        }
                    } else {
                        logEvent("viewScreenshotFailed", listOf("error" to result))
                        if (continuation.isActive) {
                            continuation.resume(null, null)
                        }
                    }
                },
                Handler(Looper.getMainLooper())
            )
        }
    }

    private fun hasEnoughMemoryForScreenshot(): Boolean {
        val runtime = Runtime.getRuntime()
        val used = runtime.totalMemory().toFloat()
        val max = runtime.maxMemory().toFloat()
        val remaining = 1f - (used / max)

        return remaining > .20
    }
}