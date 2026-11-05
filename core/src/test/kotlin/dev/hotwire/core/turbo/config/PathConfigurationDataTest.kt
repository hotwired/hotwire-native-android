package dev.hotwire.core.turbo.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PathConfigurationDataTest {
    private val data = PathConfigurationData(
        rules = listOf(
            rule("^/files/", "files"),
            rule("^/$", "root"),
            rule("\\?q=report\\.pdf$", "query"),
            rule("^example\\.com$", "mailto")
        )
    )

    @Test
    fun `matches a plain url`() {
        assertThat(data.matched("https://app.example.com/files/1")).containsExactly("files")
    }

    @Test
    fun `matches a url with a tab or newline in its path the way it loads`() {
        assertThat(data.matched("https://app.example.com/fi\tles/1")).containsExactly("files")
        assertThat(data.matched("https://app.example.com/fi\nles/1")).containsExactly("files")
    }

    @Test
    fun `matches a url with backslashes the way it loads`() {
        assertThat(data.matched("https://app.example.com\\files\\1")).containsExactly("files")
    }

    @Test
    fun `matches a url with dot segments the way it loads`() {
        assertThat(data.matched("https://app.example.com/other/../files/1")).containsExactly("files")
        assertThat(data.matched("https://app.example.com/other/%2e%2e/files/1")).containsExactly("files")
    }

    @Test
    fun `matches the query`() {
        assertThat(data.matched("https://app.example.com/search?q=report.pdf")).containsExactly("query")
    }

    @Test
    fun `matches the root path`() {
        assertThat(data.matched("https://app.example.com/")).containsExactly("root")
    }

    @Test
    fun `keeps an empty path for a url without one`() {
        assertThat(data.matched("https://app.example.com")).isEmpty()
        assertThat(data.matched("https://app.example.com?q=report.pdf")).containsExactly("query")
    }

    @Test
    fun `still matches a non-http url by its path`() {
        assertThat(data.matched("mailto:example.com")).containsExactly("mailto")
    }

    @Test
    fun `matches nothing for a malformed url`() {
        assertThat(data.matched("not a url")).isEmpty()
    }

    private fun rule(pattern: String, name: String) = PathConfigurationRule(
        patterns = listOf(pattern),
        properties = hashMapOf(name to true)
    )

    private fun PathConfigurationData.matched(location: String) = properties(location).keys
}
