package dev.hotwire.core.bridge

import dev.hotwire.core.config.Hotwire
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserAgentTest {
    @Test
    fun userAgentSubstring() {
        val factories = listOf(
            BridgeComponentFactory("one", TestData::OneBridgeComponent),
            BridgeComponentFactory("two", TestData::TwoBridgeComponent)
        )

        Hotwire.registerBridgeComponentFactories(factories)

        val userAgentSubstring = Hotwire.config.userAgentSubstring()
        assertEquals(userAgentSubstring, "Turbo Native Android; bridge-components: [one two];")
    }

    @Test
    fun userAgent() {
        val factories = listOf(
            BridgeComponentFactory("one", TestData::OneBridgeComponent),
            BridgeComponentFactory("two", TestData::TwoBridgeComponent)
        )

        Hotwire.registerBridgeComponentFactories(factories)
        Hotwire.config.userAgent = "Test; ${Hotwire.config.userAgentSubstring()}"

        val userAgent = Hotwire.config.userAgent
        assertEquals(userAgent, "Test; Turbo Native Android; bridge-components: [one two];")
    }
}
