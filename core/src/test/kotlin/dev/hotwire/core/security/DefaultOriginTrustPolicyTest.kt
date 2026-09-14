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

    private fun isTrustedForNavigation(location: String): Boolean {
        return location.toOriginOrNull()?.let { policy.isTrustedForNavigation(it) } ?: false
    }

    private fun isTrustedForNativeAccess(location: String): Boolean {
        return location.toOriginOrNull()?.let { policy.isTrustedForNativeAccess(it) } ?: false
    }

    @Before
    fun setup() {
        Hotwire.config.trustedOrigins.clear()
        Hotwire.config.trustedOrigins.register("https://my.app.com/start")
    }

    @After
    fun teardown() {
        Hotwire.config.trustedOrigins.clear()
    }

    @Test
    fun `same origin as a registered start location grants native access`() {
        assertTrue(isTrustedForNavigation("https://my.app.com/another/page?q=1"))
        assertTrue(isTrustedForNativeAccess("https://my.app.com/another/page?q=1"))
    }

    @Test
    fun `explicit default port is the same origin`() {
        assertTrue(isTrustedForNavigation("https://my.app.com:443/page"))
        assertTrue(isTrustedForNativeAccess("https://my.app.com:443/page"))
    }

    @Test
    fun `any registered origin grants native access, not just the first`() {
        Hotwire.config.trustedOrigins.register("https://other.app.com/home")

        assertTrue(isTrustedForNavigation("https://other.app.com/page"))
        assertTrue(isTrustedForNativeAccess("https://other.app.com/page"))
        assertTrue(isTrustedForNavigation("https://my.app.com/page"))
        assertTrue(isTrustedForNativeAccess("https://my.app.com/page"))
    }

    @Test
    fun `nothing is trusted when no start location is registered`() {
        Hotwire.config.trustedOrigins.clear()

        assertFalse(isTrustedForNavigation("https://my.app.com/page"))
        assertFalse(isTrustedForNativeAccess("https://my.app.com/page"))
    }

    @Test
    fun `different host is not trusted`() {
        assertFalse(isTrustedForNavigation("https://evil.com/page"))
        assertFalse(isTrustedForNativeAccess("https://evil.com/page"))
    }

    @Test
    fun `subdomain of a registered host is not trusted`() {
        assertFalse(isTrustedForNavigation("https://sub.my.app.com/page"))
        assertFalse(isTrustedForNativeAccess("https://sub.my.app.com/page"))
    }

    @Test
    fun `parent domain of a registered host is not trusted`() {
        assertFalse(isTrustedForNavigation("https://app.com/page"))
        assertFalse(isTrustedForNativeAccess("https://app.com/page"))
    }

    @Test
    fun `scheme downgrade is not trusted`() {
        assertFalse(isTrustedForNavigation("http://my.app.com/page"))
        assertFalse(isTrustedForNativeAccess("http://my.app.com/page"))
    }

    @Test
    fun `different port is not trusted`() {
        assertFalse(isTrustedForNavigation("https://my.app.com:8443/page"))
        assertFalse(isTrustedForNativeAccess("https://my.app.com:8443/page"))
    }

    @Test
    fun `userinfo masquerade is not trusted`() {
        assertFalse(isTrustedForNavigation("https://my.app.com@evil.com/page"))
        assertFalse(isTrustedForNativeAccess("https://my.app.com@evil.com/page"))
    }

    @Test
    fun `non-http schemes are not trusted`() {
        assertFalse(isTrustedForNavigation("javascript:alert(1)"))
        assertFalse(isTrustedForNativeAccess("javascript:alert(1)"))
        assertFalse(isTrustedForNavigation("about:blank"))
        assertFalse(isTrustedForNativeAccess("about:blank"))
        assertFalse(isTrustedForNavigation("file:///etc/hosts"))
        assertFalse(isTrustedForNativeAccess("file:///etc/hosts"))
    }

    @Test
    fun `unparseable locations are not trusted`() {
        assertFalse(isTrustedForNavigation("not a url"))
        assertFalse(isTrustedForNativeAccess("not a url"))
        assertFalse(isTrustedForNavigation(""))
        assertFalse(isTrustedForNativeAccess(""))
    }
}
