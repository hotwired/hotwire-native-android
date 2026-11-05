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
        val url = try {
            URL(location)
        } catch (e: MalformedURLException) {
            null
        }

        // Match http(s) urls the way the WebView and OkHttp load them: tabs and newlines removed,
        // backslashes read as slashes, and dot segments resolved. java.net.URL keeps all of these
        // as written. A url without a path keeps its empty path, so rules for "/" still don't
        // match a bare host.
        location.toHttpUrlOrNull()?.let { httpUrl ->
            val hasNoPath = url?.path?.isEmpty() == true && httpUrl.encodedPath == "/"
            val path = if (hasNoPath) "" else httpUrl.encodedPath
            return httpUrl.encodedQuery?.let { "$path?$it" } ?: path
        }

        return when {
            url == null -> ""
            url.query == null -> url.path
            else -> "${url.path}?${url.query}"
        }
    }
}
