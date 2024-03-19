package dev.hotwire.core.bridge

import dev.hotwire.core.config.Hotwire
import org.junit.Assert.assertEquals
import org.junit.Test

class UserAgentTest {
    @Test
    fun userAgentSubstring() {
        Hotwire.registerBridgeComponents(TestData.componentFactories)

        val userAgentSubstring = Hotwire.config.userAgentSubstring()
        assertEquals(userAgentSubstring, "Turbo Native Android; bridge-components: [one two];")
    }

    @Test
    fun userAgent() {
        Hotwire.registerBridgeComponents(TestData.componentFactories)
        Hotwire.config.userAgent = "Test; ${Hotwire.config.userAgentSubstring()}"

        val userAgent = Hotwire.config.userAgent
        assertEquals(userAgent, "Test; Turbo Native Android; bridge-components: [one two];")
    }
}
