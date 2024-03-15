package dev.hotwire.core.bridge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BridgeComponentFactoryTest {
    @Test
    fun createComponents() {
        val factories = TestData.componentFactories
        val delegate = TestData.bridgeDelegate

        val componentOne = factories[0].create(delegate)
        assertEquals("one", componentOne.name)
        assertTrue(componentOne is TestData.OneBridgeComponent)

        val componentTwo = factories[1].create(delegate)
        assertEquals("two", componentTwo.name)
        assertTrue(componentTwo is TestData.TwoBridgeComponent)
    }
}
