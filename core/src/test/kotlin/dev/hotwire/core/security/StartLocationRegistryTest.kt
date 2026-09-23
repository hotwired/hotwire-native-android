package dev.hotwire.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StartLocationRegistryTest {
    private val registry = StartLocationRegistry()
    private val myAppOrigin = Origin.parse("https://my.app.com")

    @Test
    fun `a registered start location's origin is present`() {
        registry.register("https://my.app.com/start")

        assertTrue(myAppOrigin in registry.origins)
    }

    @Test
    fun `unregistering a host's location removes its origin`() {
        registry.register("https://my.app.com/start")
        registry.unregister("https://my.app.com/start")

        assertFalse(myAppOrigin in registry.origins)
    }

    @Test
    fun `a shared origin survives until its last registration is withdrawn`() {
        registry.register("https://my.app.com/start")
        registry.register("https://my.app.com/other-start")

        registry.unregister("https://my.app.com/start")
        assertTrue(myAppOrigin in registry.origins)

        registry.unregister("https://my.app.com/other-start")
        assertFalse(myAppOrigin in registry.origins)
    }

    @Test
    fun `host recreation re-registers the same origin without losing it`() {
        registry.register("https://my.app.com/start")

        registry.unregister("https://my.app.com/start")
        registry.register("https://my.app.com/start")

        assertTrue(myAppOrigin in registry.origins)
    }

    @Test
    fun `a non-http location never registers`() {
        registry.register("file:///sdcard/start")
        registry.register("not a url")

        assertEquals(emptySet<Origin>(), registry.origins)
    }

    @Test
    fun `unregistering an unknown location is a no-op`() {
        registry.register("https://my.app.com/start")
        registry.unregister("https://other.app.com/start")
        registry.unregister("not a url")

        assertTrue(myAppOrigin in registry.origins)
    }

    @Test
    fun `clear withdraws every registration`() {
        registry.register("https://my.app.com/start")
        registry.register("https://other.app.com/start")

        registry.clear()

        assertEquals(emptySet<Origin>(), registry.origins)
    }

    @Test
    fun `origins is a live view`() {
        val origins = registry.origins

        registry.register("https://my.app.com/start")

        assertEquals(setOf(myAppOrigin), origins)
    }

    @Test
    fun `origins reports bare origins`() {
        registry.register("https://my.app.com/start?utm=1")

        assertEquals(setOf(myAppOrigin), registry.origins)
    }

    @Test
    fun `an unregistered origin is absent`() {
        assertFalse(myAppOrigin in registry.origins)
    }
}
