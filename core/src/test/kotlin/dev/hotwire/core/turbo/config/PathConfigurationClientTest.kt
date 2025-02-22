package dev.hotwire.core.turbo.config

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PathConfigurationClientTest {

    @After
    fun tearDown() {
        PathConfigurationClient.setPathConfigurationHeaders(emptyMap())
    }

    @Test
    fun `headers should be null by default`() {
        assertNull(PathConfigurationClient.getHeaders())
    }

    @Test
    fun `should set and get custom headers`() {
        val customHeaders = mapOf(
            "Accept" to "application/json",
            "Custom-Header" to "test-value"
        )

        PathConfigurationClient.setPathConfigurationHeaders(customHeaders)

        assertEquals(customHeaders, PathConfigurationClient.getHeaders())
    }

    @Test
    fun `should allow empty headers map`() {
        PathConfigurationClient.setPathConfigurationHeaders(emptyMap())

        assertNull(PathConfigurationClient.getHeaders())
    }

    @Test
    fun `should override previous headers when setting new ones`() {
        val initialHeaders = mapOf("Initial-Header" to "initial-value")
        val newHeaders = mapOf("New-Header" to "new-value")

        PathConfigurationClient.setPathConfigurationHeaders(initialHeaders)
        PathConfigurationClient.setPathConfigurationHeaders(newHeaders)

        assertEquals(newHeaders, PathConfigurationClient.getHeaders())
    }
}