package dev.hotwire.core.bridge

import org.junit.Assert.assertEquals
import org.junit.Test

class UserAgentTest {
    @Test
    fun userAgentSubstring() {
        val factories = listOf(
            BridgeComponentFactory("one", TestData::OneBridgeComponent),
            BridgeComponentFactory("two", TestData::TwoBridgeComponent)
        )

        val userAgentSubstring = Strada.userAgentSubstring(factories)
        assertEquals(userAgentSubstring, "bridge-components: [one two]")
    }
}
