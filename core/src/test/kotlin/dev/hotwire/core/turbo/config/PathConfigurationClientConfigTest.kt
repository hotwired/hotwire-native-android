package dev.hotwire.core.turbo.config

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PathConfigurationClientConfigTest {

    @After
    fun tearDown() {
        PathConfigurationClientConfig.setRequestHeaders(emptyMap())
    }

    @Test
    fun `headers should be null by default`() {
        assertNull(PathConfigurationClientConfig.getHeaders())
    }

    @Test
    fun `should set and get custom headers`() {
        val customHeaders = mapOf(
            "Accept" to "application/json",
            "Custom-Header" to "test-value"
        )

        PathConfigurationClientConfig.setRequestHeaders(customHeaders)

        assertEquals(customHeaders, PathConfigurationClientConfig.getHeaders())
    }

    @Test
    fun `should allow empty headers map`() {
        PathConfigurationClientConfig.setRequestHeaders(emptyMap())

        assertNull(PathConfigurationClientConfig.getHeaders())
    }

    @Test
    fun `should override previous headers when setting new ones`() {
        val initialHeaders = mapOf("Initial-Header" to "initial-value")
        val newHeaders = mapOf("New-Header" to "new-value")

        PathConfigurationClientConfig.setRequestHeaders(initialHeaders)
        PathConfigurationClientConfig.setRequestHeaders(newHeaders)

        assertEquals(newHeaders, PathConfigurationClientConfig.getHeaders())
    }
}