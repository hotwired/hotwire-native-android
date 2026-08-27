package dev.hotwire.core.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultHostVerifierTest {
    private val verifier = DefaultHostVerifier
    private val startLocation = "https://my.app.com/start"

    private val checks = listOf<(String) -> Boolean>(
        { verifier.isTrustedForNavigation(it, startLocation) },
        { verifier.isTrustedForBridge(it, startLocation) }
    )

    @Test
    fun `same origin is trusted for navigation and bridge`() {
        checks.forEach { isTrusted ->
            assertTrue(isTrusted("https://my.app.com/another/page?q=1"))
        }
    }

    @Test
    fun `explicit default port is the same origin`() {
        checks.forEach { isTrusted ->
            assertTrue(isTrusted("https://my.app.com:443/page"))
        }
    }

    @Test
    fun `different host is not trusted`() {
        checks.forEach { isTrusted ->
            assertFalse(isTrusted("https://evil.com/page"))
        }
    }

    @Test
    fun `subdomain of the app host is not trusted`() {
        checks.forEach { isTrusted ->
            assertFalse(isTrusted("https://sub.my.app.com/page"))
        }
    }

    @Test
    fun `parent domain of the app host is not trusted`() {
        checks.forEach { isTrusted ->
            assertFalse(isTrusted("https://app.com/page"))
        }
    }

    @Test
    fun `scheme downgrade is not trusted`() {
        checks.forEach { isTrusted ->
            assertFalse(isTrusted("http://my.app.com/page"))
        }
    }

    @Test
    fun `different port is not trusted`() {
        checks.forEach { isTrusted ->
            assertFalse(isTrusted("https://my.app.com:8443/page"))
        }
    }

    @Test
    fun `userinfo masquerade is not trusted`() {
        checks.forEach { isTrusted ->
            assertFalse(isTrusted("https://my.app.com@evil.com/page"))
        }
    }

    @Test
    fun `non-http schemes are not trusted`() {
        checks.forEach { isTrusted ->
            assertFalse(isTrusted("javascript:alert(1)"))
            assertFalse(isTrusted("about:blank"))
            assertFalse(isTrusted("file:///etc/hosts"))
        }
    }

    @Test
    fun `unparseable locations are not trusted`() {
        checks.forEach { isTrusted ->
            assertFalse(isTrusted("not a url"))
            assertFalse(isTrusted(""))
        }
    }

    @Test
    fun `an unparseable start location trusts nothing`() {
        assertFalse(verifier.isTrustedForNavigation("https://my.app.com/page", ""))
        assertFalse(verifier.isTrustedForBridge("https://my.app.com/page", ""))
    }
}
