package dev.hotwire.core.security

import dev.hotwire.core.config.Hotwire
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultOriginTrustPolicyTest {
    private val policy = DefaultOriginTrustPolicy

    private fun isTrusted(location: String): Boolean {
        val origin = Origin.parse(location)
        return policy.isTrustedForNavigation(origin) && policy.isTrustedForNativeAccess(origin)
    }

    @Before
    fun setup() {
        Hotwire.config.startLocationRegistry.clear()
        Hotwire.config.registerStartLocation("https://my.app.com/start")
    }

    @After
    fun teardown() {
        Hotwire.config.startLocationRegistry.clear()
    }

    @Test
    fun `a registered start location's origin is trusted for navigation and native access`() {
        assertTrue(isTrusted("https://my.app.com/another/page?q=1"))
    }

    @Test
    fun `every registered origin is trusted, not just the first`() {
        Hotwire.config.registerStartLocation("https://other.app.com/home")

        assertTrue(isTrusted("https://other.app.com/page"))
        assertTrue(isTrusted("https://my.app.com/page"))
    }

    @Test
    fun `nothing is trusted when no start location is registered`() {
        Hotwire.config.startLocationRegistry.clear()

        assertFalse(isTrusted("https://my.app.com/page"))
    }

    @Test
    fun `only an exact origin match is trusted`() {
        assertFalse(isTrusted("http://my.app.com/page"))
        assertFalse(isTrusted("https://my.app.com:8443/page"))
        assertFalse(isTrusted("https://sub.my.app.com/page"))
        assertFalse(isTrusted("https://app.com/page"))
        assertFalse(isTrusted("https://evil.com/page"))
    }
}
