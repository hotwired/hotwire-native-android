package dev.hotwire.navigation.util

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.Window
import dev.hotwire.navigation.logging.logError
import dev.hotwire.navigation.logging.logDebug
import dev.hotwire.navigation.views.HotwireView
import kotlinx.coroutines.CancellableContinuation
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

    suspend fun captureScreenshot(hotwireView: HotwireView, window: Window) {
        bitmap = copyViewToBitmap(hotwireView, window)
        screenshotOrientation = hotwireView.currentOrientation()
        screenshotZoomed = currentlyZoomed
    }

    private suspend fun copyViewToBitmap(hotwireView: HotwireView, window: Window): Bitmap? {
        return suspendCancellableCoroutine { continuation ->
            val start = System.currentTimeMillis()
            val rect = Rect().also { hotwireView.getGlobalVisibleRect(it) }
            val neededBytes = rect.width().toLong() * rect.height() * BYTES_PER_PIXEL
            val memoryInfo = hotwireView.context.memoryInfo()

            if (!hotwireView.isLaidOut || rect.isEmpty || !hasEnoughMemory(memoryInfo, neededBytes)) {
                logDebug(
                    "viewScreenshotSkipped", listOf(
                        "laidOut" to hotwireView.isLaidOut,
                        "size" to "${rect.width()}x${rect.height()}",
                        "neededBytes" to neededBytes,
                        "availableBytes" to memoryInfo.availMem - memoryInfo.threshold,
                    )
                )
                continuation.resumeIfActive(null)
                return@suspendCancellableCoroutine
            }

            val bitmap = try {
                Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888)
            } catch (error: OutOfMemoryError) {
                logError("viewScreenshotFailed", error)
                continuation.resumeIfActive(null)
                return@suspendCancellableCoroutine
            }

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
                            continuation.resumeIfActive(bitmap)
                        } else {
                            bitmap.recycle()
                            logError("viewScreenshotFailed", Exception("PixelCopy failed with result $result"))
                            continuation.resumeIfActive(null)
                        }
                    },
                    Handler(Looper.getMainLooper())
                )
            } catch (exception: Exception) {
                bitmap.recycle()
                logError("viewScreenshotFailed", exception)
                continuation.resumeIfActive(null)
            }
        }
    }

    internal fun hasEnoughMemory(memoryInfo: ActivityManager.MemoryInfo, neededBytes: Long): Boolean {
        return !memoryInfo.lowMemory && memoryInfo.availMem - memoryInfo.threshold > neededBytes
    }

    private fun Context.memoryInfo(): ActivityManager.MemoryInfo {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return ActivityManager.MemoryInfo().also { activityManager.getMemoryInfo(it) }
    }

    private fun <T> CancellableContinuation<T>.resumeIfActive(value: T) {
        if (isActive) resume(value)
    }

    companion object {
        private const val BYTES_PER_PIXEL = 4L
    }
}
