package dev.hotwire.core.config

import dev.hotwire.core.turbo.config.PathConfigurationClientConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test

class HotwireConfigTest {

    @After
    fun tearDown() {
        PathConfigurationClientConfig.setRequestHeaders(emptyMap())
    }

    @Test
    fun `pathConfigurationClientHeaders should set custom headers in PathConfigurationClientConfig`() {
        val config = HotwireConfig()
        val headers = mapOf(
            "Custom-Header" to "test-value",
            "Accept" to "application/json"
        )

        config.pathConfigurationClientHeaders(headers)

        assertThat(PathConfigurationClientConfig.getHeaders()).isEqualTo(headers)
    }

    @Test
    fun `pathConfigurationClientHeaders should override existing headers`() {
        val config = HotwireConfig()
        val initialHeaders = mapOf("Initial-Header" to "initial-value")
        val newHeaders = mapOf("Custom-Header" to "test-value")

        config.pathConfigurationClientHeaders(initialHeaders)

        config.pathConfigurationClientHeaders(newHeaders)

        assertThat(PathConfigurationClientConfig.getHeaders()).isEqualTo(newHeaders)
    }
}