package dev.hotwire.navigation.fragments

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class DisplayTitleTest {
    @Test
    fun `shows the page title`() {
        assertThat(displayTitle("Dashboard", "https://example.com/dashboard")).isEqualTo("Dashboard")
    }

    @Test
    fun `hides a missing title`() {
        assertThat(displayTitle(null, "https://example.com/dashboard")).isEmpty()
        assertThat(displayTitle(null, null)).isEmpty()
    }

    @Test
    fun `hides the fallback title for an https url`() {
        assertThat(displayTitle("example.com/dashboard", "https://example.com/dashboard")).isEmpty()
    }

    @Test
    fun `hides the fallback title for an http url with a port`() {
        assertThat(displayTitle("192.168.42.51:3000/dashboard", "http://192.168.42.51:3000/dashboard")).isEmpty()
    }

    @Test
    fun `hides the fallback title without www`() {
        assertThat(displayTitle("example.com/dashboard", "https://www.example.com/dashboard")).isEmpty()
    }

    @Test
    fun `hides the fallback title for a bare host`() {
        assertThat(displayTitle("example.com", "https://example.com/")).isEmpty()
    }

    @Test
    fun `hides the fallback title with decoded spaces`() {
        assertThat(displayTitle("example.com/my page", "https://example.com/my%20page")).isEmpty()
    }

    @Test
    fun `hides the fallback title for about blank`() {
        assertThat(displayTitle("about:blank", "about:blank")).isEmpty()
    }

    @Test
    fun `shows a title that differs from the url only by path`() {
        assertThat(displayTitle("example.com", "https://example.com/dashboard")).isEqualTo("example.com")
    }
}
