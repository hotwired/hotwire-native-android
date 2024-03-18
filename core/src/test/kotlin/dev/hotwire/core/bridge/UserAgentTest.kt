package dev.hotwire.core.bridge

import dev.hotwire.core.config.Hotwire
import org.junit.Assert.assertTrue
import org.junit.Test

class UserAgentTest {
    @Test
    fun userAgentSubstring() {
        val factories = listOf(
            BridgeComponentFactory("one", TestData::OneBridgeComponent),
            BridgeComponentFactory("two", TestData::TwoBridgeComponent)
        )

        val userAgentSubstring = Hotwire.userAgentSubstring(factories)
        assertTrue(userAgentSubstring.endsWith("bridge-components: [one two];"))
    }
}
