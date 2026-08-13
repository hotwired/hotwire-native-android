package dev.hotwire.navigation.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class HotwireViewScreenshotHolderTest {
    private val holder = HotwireViewScreenshotHolder()

    private val mb = 1024L * 1024L

    @Test
    fun `has enough memory when the heap is fully grown but mostly free`() {
        assertThat(holder.hasEnoughMemory(used = 16 * mb, max = 256 * mb)).isTrue()
    }

    @Test
    fun `does not have enough memory when the heap is nearly full`() {
        assertThat(holder.hasEnoughMemory(used = 250 * mb, max = 256 * mb)).isFalse()
    }

    @Test
    fun `does not have enough memory at the threshold`() {
        assertThat(holder.hasEnoughMemory(used = 80 * mb, max = 100 * mb)).isFalse()
    }

    @Test
    fun `has enough memory just above the threshold`() {
        assertThat(holder.hasEnoughMemory(used = 79 * mb, max = 100 * mb)).isTrue()
    }
}
