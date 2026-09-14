package dev.hotwire.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OriginTest {
    @Test
    fun `https without an explicit port defaults to 443`() {
        assertEquals(Origin("https", "a.com", 443), Origin.parseOrNull("https://a.com/path"))
    }

    @Test
    fun `an explicit port is kept`() {
        assertEquals(Origin("https", "a.com", 8443), Origin.parseOrNull("https://a.com:8443/path"))
    }

    @Test
    fun `http without an explicit port defaults to 80`() {
        assertEquals(Origin("http", "a.com", 80), Origin.parseOrNull("http://a.com/path"))
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

    @Test
    fun `path, query, and fragment are ignored`() {
        val origin = Origin("https", "a.com", 443)

        assertEquals(origin, Origin.parseOrNull("https://a.com/path"))
        assertEquals(origin, Origin.parseOrNull("https://a.com/path?q=1"))
        assertEquals(origin, Origin.parseOrNull("https://a.com/path#fragment"))
        assertEquals(origin, Origin.parseOrNull("https://a.com"))
    }

    @Test
    fun `equality holds across an implicit and an explicit default port`() {
        assertEquals(Origin.parseOrNull("https://a.com"), Origin.parseOrNull("https://a.com:443/path"))
    }

    @Test
    fun `toString omits a default port`() {
        assertEquals("https://a.com", Origin("https", "a.com", 443).toString())
    }

    @Test
    fun `toString keeps a non-default port`() {
        assertEquals("https://a.com:8443", Origin("https", "a.com", 8443).toString())
    }

    @Test
    fun `toString brackets an IPv6 host`() {
        val origin = Origin.parseOrNull("https://[2001:db8::1]:8443/path")

        assertEquals(Origin("https", "2001:db8::1", 8443), origin)
        assertEquals("https://[2001:db8::1]:8443", origin.toString())
        assertEquals(origin, Origin.parseOrNull(origin.toString()))
    }
}
