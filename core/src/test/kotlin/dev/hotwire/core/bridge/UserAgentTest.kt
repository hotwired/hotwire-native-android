package dev.hotwire.core.bridge

import dev.hotwire.core.config.HotwireCore
import org.junit.Assert.assertEquals
import org.junit.Test

class UserAgentTest {
    @Test
    fun userAgentSubstring() {
        HotwireCore.config.registeredBridgeComponentFactories = TestData.componentFactories

        val userAgentSubstring = HotwireCore.config.userAgentSubstring()
        assertEquals(userAgentSubstring, "Turbo Native Android; bridge-components: [one two];")
    }

    @Test
    fun userAgent() {
        HotwireCore.config.registeredBridgeComponentFactories = TestData.componentFactories
        HotwireCore.config.userAgent = "Test; ${HotwireCore.config.userAgentSubstring()}"

        val userAgent = HotwireCore.config.userAgent
        assertEquals(userAgent, "Test; Turbo Native Android; bridge-components: [one two];")
    }
}
