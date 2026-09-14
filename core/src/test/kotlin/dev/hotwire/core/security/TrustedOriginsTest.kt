package dev.hotwire.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TrustedOriginsTest {
    private val origins = TrustedOrigins()
    private val myAppOrigin = Origin("https", "my.app.com", 443)

    @Test
    fun `a registered start location's origin is trusted`() {
        origins.register("https://my.app.com/start")

        assertTrue(origins.contains(myAppOrigin))
    }

    @Test
    fun `unregistering a host's location withdraws trust`() {
        origins.register("https://my.app.com/start")
        origins.unregister("https://my.app.com/start")

        assertFalse(origins.contains(myAppOrigin))
    }

    @Test
    fun `a shared origin survives until its last registration is withdrawn`() {
        origins.register("https://my.app.com/start")
        origins.register("https://my.app.com/other-start")

        origins.unregister("https://my.app.com/start")
        assertTrue(origins.contains(myAppOrigin))

        origins.unregister("https://my.app.com/other-start")
        assertFalse(origins.contains(myAppOrigin))
    }

    @Test
    fun `host recreation re-registers the same origin without losing trust`() {
        origins.register("https://my.app.com/start")

        origins.unregister("https://my.app.com/start")
        origins.register("https://my.app.com/start")

        assertTrue(origins.contains(myAppOrigin))
    }

    @Test
    fun `a non-http location never registers`() {
        origins.register("file:///sdcard/start")
        origins.register("not a url")

        assertEquals(emptySet<Origin>(), origins.snapshot)
    }

    @Test
    fun `unregistering an unknown location is a no-op`() {
        origins.register("https://my.app.com/start")
        origins.unregister("https://other.app.com/start")
        origins.unregister("not a url")

        assertTrue(origins.contains(myAppOrigin))
    }

    @Test
    fun `clear withdraws every registration`() {
        origins.register("https://my.app.com/start")
        origins.register("https://other.app.com/start")

        origins.clear()

        assertEquals(emptySet<Origin>(), origins.snapshot)
    }

    @Test
    fun `snapshot reports bare origins`() {
        origins.register("https://my.app.com/start?utm=1")

        assertEquals(setOf(myAppOrigin), origins.snapshot)
    }

    @Test
    fun `contains is false for an unregistered origin`() {
        assertFalse(origins.contains(myAppOrigin))
    }
}
