package dev.hotwire.core.security

import dev.hotwire.core.config.Hotwire
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OriginsTest {
    @After
    fun teardown() {
        Hotwire.config.originTrustPolicy = DefaultOriginTrustPolicy
    }

    @Test
    fun `hasSameOriginAs is true for locations sharing scheme, host, and port`() {
        assertTrue("https://a.com/one".hasSameOriginAs("https://a.com:443/two"))
    }

    @Test
    fun `hasSameOriginAs is false for a different scheme, host, or port`() {
        assertFalse("https://a.com/one".hasSameOriginAs("http://a.com/one"))
        assertFalse("https://a.com/one".hasSameOriginAs("https://b.com/one"))
        assertFalse("https://a.com/one".hasSameOriginAs("https://a.com:8443/one"))
    }

    @Test
    fun `hasSameOriginAs is false when either side does not parse`() {
        assertFalse("https://a.com".hasSameOriginAs("not a url"))
        assertFalse("not a url".hasSameOriginAs("https://a.com"))
    }

    @Test
    fun `isTrustedForNavigation reflects the policy's navigation answer, not its native access answer`() {
        Hotwire.config.originTrustPolicy = recordingPolicy(navigation = true, nativeAccess = false)

        assertTrue(isTrustedForNavigation("https://a.com"))
        assertFalse(isTrustedForNativeAccess("https://a.com"))
    }

    @Test
    fun `isTrustedForNativeAccess needs both of the policy's answers`() {
        Hotwire.config.originTrustPolicy = recordingPolicy(navigation = true, nativeAccess = true)
        assertTrue(isTrustedForNativeAccess("https://a.com"))

        Hotwire.config.originTrustPolicy = recordingPolicy(navigation = false, nativeAccess = true)
        assertFalse(isTrustedForNavigation("https://a.com"))
        assertFalse(isTrustedForNativeAccess("https://a.com"))
    }

    @Test
    fun `a non-http location fails closed without consulting the policy`() {
        Hotwire.config.originTrustPolicy = throwingPolicy()

        assertFalse(isTrustedForNavigation("not a url"))
        assertFalse(isTrustedForNavigation("javascript:alert(1)"))
        assertFalse(isTrustedForNativeAccess("not a url"))
        assertFalse(isTrustedForNativeAccess("javascript:alert(1)"))
    }

    private fun recordingPolicy(navigation: Boolean, nativeAccess: Boolean) = object : OriginTrustPolicy() {
        override fun isTrustedForNavigation(origin: Origin) = navigation
        override fun isTrustedForNativeAccess(origin: Origin) = nativeAccess
    }

    private fun throwingPolicy() = object : OriginTrustPolicy() {
        override fun isTrustedForNavigation(origin: Origin): Boolean = error("policy should not be consulted")
        override fun isTrustedForNativeAccess(origin: Origin): Boolean = error("policy should not be consulted")
    }
}
