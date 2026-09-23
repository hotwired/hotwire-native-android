package dev.hotwire.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OriginTest {
    @Test
    fun `https without an explicit port defaults to 443`() {
        assertEquals(443, Origin.parse("https://a.com/path").port)
    }

    @Test
    fun `an explicit port is kept`() {
        assertEquals(8443, Origin.parse("https://a.com:8443/path").port)
    }

    @Test
    fun `http without an explicit port defaults to 80`() {
        assertEquals(80, Origin.parse("http://a.com/path").port)
    }

    @Test
    fun `scheme and host are lowercased`() {
        val origin = Origin.parse("HTTPS://My.App.COM/Path")

        assertEquals("https", origin.scheme)
        assertEquals("my.app.com", origin.host)
        assertEquals(Origin.parse("https://my.app.com"), origin)
    }

    @Test
    fun `non-http schemes do not parse`() {
        assertNull(Origin.parseOrNull("about:blank"))
        assertNull(Origin.parseOrNull("javascript:alert(1)"))
        assertNull(Origin.parseOrNull("file:///x"))
    }

    @Test
    fun `unparseable locations do not parse`() {
        assertNull(Origin.parseOrNull("not a url"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parse throws for a non-http location`() {
        Origin.parse("javascript:alert(1)")
    }

    @Test
    fun `path, query, and fragment are ignored`() {
        val origin = Origin.parse("https://a.com")

        assertEquals(origin, Origin.parseOrNull("https://a.com/path"))
        assertEquals(origin, Origin.parseOrNull("https://a.com/path?q=1"))
        assertEquals(origin, Origin.parseOrNull("https://a.com/path#fragment"))
    }

    @Test
    fun `equality holds across an implicit and an explicit default port`() {
        assertEquals(Origin.parse("https://a.com"), Origin.parse("https://a.com:443/path"))
    }

    @Test
    fun `toString omits a default port`() {
        assertEquals("https://a.com", Origin.parse("https://a.com:443/path").toString())
    }

    @Test
    fun `toString keeps a non-default port`() {
        assertEquals("https://a.com:8443", Origin.parse("https://a.com:8443/path").toString())
    }

    @Test
    fun `toString brackets an IPv6 host`() {
        val origin = Origin.parse("https://[2001:db8::1]:8443/path")

        assertEquals("2001:db8::1", origin.host)
        assertEquals("https://[2001:db8::1]:8443", origin.toString())
        assertEquals(origin, Origin.parseOrNull(origin.toString()))
    }
}
