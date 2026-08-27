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
class DefaultHostVerifierTest {
    private val verifier = DefaultHostVerifier

    private val checks = listOf<(String) -> Boolean>(
        { verifier.isTrustedForNavigation(it) },
        { verifier.isTrustedForBridge(it) }
    )

    @Before
    fun setup() {
        Hotwire.config.clearTrustedLocations()
        Hotwire.config.registerTrustedLocation("https://my.app.com/start")
    }

    @After
    fun teardown() {
        Hotwire.config.clearTrustedLocations()
    }

    @Test
    fun `same origin as a registered start location is trusted`() {
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
    fun `any registered origin is trusted, not just the first`() {
        Hotwire.config.registerTrustedLocation("https://other.app.com/home")

        checks.forEach { isTrusted ->
            assertTrue(isTrusted("https://other.app.com/page"))
            assertTrue(isTrusted("https://my.app.com/page"))
        }
    }

    @Test
    fun `nothing is trusted when no start location is registered`() {
        Hotwire.config.clearTrustedLocations()

        checks.forEach { isTrusted ->
            assertFalse(isTrusted("https://my.app.com/page"))
        }
    }

    @Test
    fun `different host is not trusted`() {
        checks.forEach { isTrusted ->
            assertFalse(isTrusted("https://evil.com/page"))
        }
    }

    @Test
    fun `subdomain of a registered host is not trusted`() {
        checks.forEach { isTrusted ->
            assertFalse(isTrusted("https://sub.my.app.com/page"))
        }
    }

    @Test
    fun `parent domain of a registered host is not trusted`() {
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
}
