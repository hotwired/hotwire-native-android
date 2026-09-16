package dev.hotwire.navigation.util

import android.app.ActivityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class HotwireViewScreenshotHolderTest {
    private val holder = HotwireViewScreenshotHolder()
    private val mb = 1024L * 1024L
    private val screenshotBytes = 12 * mb

    @Test
    fun `has enough memory when there is headroom above the threshold`() {
        val info = memoryInfo(availMem = 1300 * mb, threshold = 216 * mb)
        assertThat(holder.hasEnoughMemory(info, screenshotBytes)).isTrue()
    }

    @Test
    fun `does not have enough memory when the system reports low memory`() {
        val info = memoryInfo(availMem = 500 * mb, threshold = 216 * mb, lowMemory = true)
        assertThat(holder.hasEnoughMemory(info, screenshotBytes)).isFalse()
    }

    @Test
    fun `does not have enough memory when the screenshot would cross the threshold`() {
        val info = memoryInfo(availMem = 226 * mb, threshold = 216 * mb)
        assertThat(holder.hasEnoughMemory(info, screenshotBytes)).isFalse()
    }

    @Test
    fun `does not have enough memory when the screenshot exactly reaches the threshold`() {
        val info = memoryInfo(availMem = 228 * mb, threshold = 216 * mb)
        assertThat(holder.hasEnoughMemory(info, screenshotBytes)).isFalse()
    }

    private fun memoryInfo(availMem: Long, threshold: Long, lowMemory: Boolean = false) =
        ActivityManager.MemoryInfo().also {
            it.availMem = availMem
            it.threshold = threshold
            it.lowMemory = lowMemory
        }
}
