package dev.hotwire.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TrustedLocationsTest {
    private val locations = TrustedLocations()

    @Test
    fun `a registered start location's origin is trusted`() {
        locations.register("https://my.app.com/start")

        assertTrue(locations.isTrustedOrigin("https://my.app.com/page"))
    }

    @Test
    fun `unregistering a host's location withdraws trust`() {
        locations.register("https://my.app.com/start")
        locations.unregister("https://my.app.com/start")

        assertFalse(locations.isTrustedOrigin("https://my.app.com/page"))
    }

    @Test
    fun `a shared origin survives until its last registration is withdrawn`() {
        locations.register("https://my.app.com/start")
        locations.register("https://my.app.com/other-start")

        locations.unregister("https://my.app.com/start")
        assertTrue(locations.isTrustedOrigin("https://my.app.com/page"))

        locations.unregister("https://my.app.com/other-start")
        assertFalse(locations.isTrustedOrigin("https://my.app.com/page"))
    }

    @Test
    fun `host recreation re-registers the same origin without losing trust`() {
        locations.register("https://my.app.com/start")

        locations.unregister("https://my.app.com/start")
        locations.register("https://my.app.com/start")

        assertTrue(locations.isTrustedOrigin("https://my.app.com/page"))
    }

    @Test
    fun `a non-http location never registers`() {
        locations.register("file:///sdcard/start")
        locations.register("not a url")

        assertFalse(locations.isTrustedOrigin("file:///sdcard/start"))
        assertEquals(emptySet<String>(), locations.snapshot)
    }

    @Test
    fun `unregistering an unknown location is a no-op`() {
        locations.register("https://my.app.com/start")
        locations.unregister("https://other.app.com/start")
        locations.unregister("not a url")

        assertTrue(locations.isTrustedOrigin("https://my.app.com/page"))
    }

    @Test
    fun `snapshot reports bare origins`() {
        locations.register("https://my.app.com/start?utm=1")

        assertEquals(setOf("https://my.app.com/"), locations.snapshot)
    }
}
