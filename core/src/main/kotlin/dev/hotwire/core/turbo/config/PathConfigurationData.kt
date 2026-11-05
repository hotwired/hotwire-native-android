package dev.hotwire.core.turbo.config

import com.google.gson.annotations.SerializedName
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.MalformedURLException
import java.net.URL

@ConsistentCopyVisibility
data class PathConfigurationData internal constructor(
    @SerializedName("rules")
    internal val rules: List<PathConfigurationRule> = emptyList(),

    /**
     * Gets the top-level settings specified in the app's path configuration.
     * The settings are map of key/value `String` items.
     */
    @SerializedName("settings")
    val settings: PathConfigurationSettings = PathConfigurationSettings()
) {
    /**
     * Retrieve the path properties based on the cascading rules in your
     * path configuration.
     *
     * @param location The absolute url to match against the configuration's
     *  rules. Only the url's relative path will be used to find the matching
     *  regex rules.
     * @return The map of key/value `String` properties
     */
    fun properties(location: String): PathConfigurationProperties {
        val properties = PathConfigurationProperties()
        val path = path(location)

        for (rule in rules + historicalLocationRules) {
            if (rule.matches(path)) properties.putAll(rule.properties)
        }

        return properties
    }

    private fun path(location: String): String {
        // Match http(s) urls the way the WebView loads them. It drops tabs and newlines anywhere
        // in the url, then reads backslashes as slashes and resolves dot segments, like OkHttp.
        // java.net.URL keeps all of these as written. OkHttp also drops a form feed, which the
        // WebView percent-encodes instead, so encode it first.
        val httpLocation = location
            .filterNot { it == '\t' || it == '\n' || it == '\r' }
            .trim { it <= ' ' }
            .replace("\u000C", "%0C")
        if (httpLocation.isHttpUrl()) {
            val url = httpLocation.toHttpUrlOrNull() ?: return ""
            val path = if (httpLocation.hasNoPath()) "" else url.encodedPath
            return url.encodedQuery?.let { "$path?$it" } ?: path
        }

        val url = try {
            URL(location)
        } catch (e: MalformedURLException) {
            return ""
        }

        return if (url.query == null) {
            url.path
        } else {
            "${url.path}?${url.query}"
        }
    }

    private fun String.isHttpUrl() = startsWith("http:", ignoreCase = true) ||
        startsWith("https:", ignoreCase = true)

    // A url without a path, like "https://example.com" or "https://example.com?q=1", keeps an
    // empty path, so rules for "/" don't match a bare host.
    private fun String.hasNoPath() = substringAfter(':')
        .trimStart('/', '\\')
        .takeWhile { it != '?' && it != '#' }
        .none { it == '/' || it == '\\' }
}
