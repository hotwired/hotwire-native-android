package dev.hotwire.core.turbo.config

import com.google.gson.annotations.SerializedName
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
        val url = URL(location)

        return when (url.query) {
            null -> url.path
            else -> "${url.path}?${url.query}"
        }
    }
}
