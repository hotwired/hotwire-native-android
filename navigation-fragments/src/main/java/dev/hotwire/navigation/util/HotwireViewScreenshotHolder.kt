package dev.hotwire.navigation.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import dev.hotwire.navigation.logging.logError
import dev.hotwire.navigation.logging.logDebug
import dev.hotwire.navigation.views.HotwireView
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal class HotwireViewScreenshotHolder {
    private var bitmap: Bitmap? = null
    private var screenshotOrientation = 0
    private var screenshotZoomed = false
    var currentlyZoomed = false

    fun reset() {
        bitmap = null
        screenshotOrientation = 0
        screenshotZoomed = false
    }

    fun showScreenshotIfAvailable(hotwireView: HotwireView) {
        if (screenshotOrientation == hotwireView.currentOrientation() &&
            screenshotZoomed == currentlyZoomed
        ) {
            bitmap?.let { hotwireView.addScreenshot(it) }
        }
    }

    suspend fun captureScreenshot(hotwireView: HotwireView) {
        bitmap = copyViewToBitmap(hotwireView)
        screenshotOrientation = hotwireView.currentOrientation()
        screenshotZoomed = currentlyZoomed
    }

    private suspend fun copyViewToBitmap(hotwireView: HotwireView): Bitmap? {
        return suspendCancellableCoroutine { continuation ->
            val start = System.currentTimeMillis()
            val window = hotwireView.getActivity()?.window

            if (window == null || !hotwireView.isLaidOut || !hasEnoughMemoryForScreenshot() ||
                hotwireView.width <= 0 || hotwireView.height <= 0
            ) {
                if (continuation.isActive) {
                    continuation.resume(null)
                }
                return@suspendCancellableCoroutine
            }

            val rect = Rect()
            hotwireView.getGlobalVisibleRect(rect)

            val bitmap = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888)

            try {
                PixelCopy.request(
                    window, rect, bitmap,
                    { result ->
                        if (result == PixelCopy.SUCCESS) {
                            logDebug(
                                "viewScreenshotCreated", listOf(
                                    "size" to "${bitmap.width}x${bitmap.height}",
                                    "duration" to "${System.currentTimeMillis() - start}ms",
                                )
                            )
                            if (continuation.isActive) {
                                continuation.resume(bitmap)
                            }
                        } else {
                            logError("viewScreenshotFailed", Exception("PixelCopy failed with result $result"))
                            if (continuation.isActive) {
                                continuation.resume(null)
                            }
                        }
                    },
                    Handler(Looper.getMainLooper())
                )
            } catch (exception: Exception) {
                logError("viewScreenshotFailed", exception)
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
        }
    }

    private fun hasEnoughMemoryForScreenshot(): Boolean {
        val runtime = Runtime.getRuntime()
        val used = runtime.totalMemory().toFloat()
        val max = runtime.maxMemory().toFloat()
        val remaining = 1f - (used / max)

        return remaining > .20
    }

    // Inspired by https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/app/MediaRouteButton.java#163
    private fun View.getActivity(): Activity? {
        var context: Context? = context
        while (context is ContextWrapper) {
            if (context is Activity) {
                return context
            }
            context = context.baseContext
        }
        return null
    }
}