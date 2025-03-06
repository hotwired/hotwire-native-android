package dev.hotwire.core.turbo.config

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.google.gson.annotations.SerializedName
import dev.hotwire.core.turbo.nav.Presentation
import dev.hotwire.core.turbo.nav.PresentationContext
import dev.hotwire.core.turbo.nav.QueryStringPresentation
import java.net.URL

/**
 * Provides the ability to load, parse, and retrieve url path
 * properties from the app's JSON configuration file.
 */
class PathConfiguration {
    private val cachedProperties: HashMap<String, PathConfigurationProperties> = hashMapOf()

    internal var loader: PathConfigurationLoader? = null

    @SerializedName("rules")
    internal var rules: List<PathConfigurationRule> = emptyList()

    /**
     * Gets the top-level settings specified in the app's path configuration.
     * The settings are map of key/value `String` items.
     */
    @SerializedName("settings")
    var settings: PathConfigurationSettings = PathConfigurationSettings()
        private set

    /**
     * Represents the location of the app's path configuration JSON file(s).
     */
    data class Location(
        /**
         * The location of the locally bundled configuration file. Providing a
         * local configuration file is highly recommended, so your app's
         * configuration is available immediately at startup. This must be
         * located in the app's `assets` directory. For example, a configuration
         * located in `assets/json/configuration.json` would specify the path
         * without the `assets` prefix: `"json/configuration.json"`.
         */
        val assetFilePath: String? = null,

        /**
         * The location of the remote configuration file on your server. This
         * file must be publicly available via a GET request. The file will be
         * automatically downloaded and cached at app startup. This location
         * must be the full url of the JSON file, for example:
         * `"https://turbo.hotwired.dev/demo/json/configuration.json"`
         */
        val remoteFileUrl: String? = null
    )

    /**
     * Loader options when fetching remote path configuration files from your server.
     */
    data class LoaderOptions(
        /**
         * Custom HTTP headers to send with each remote path configuration file request.
         */
        val httpHeaders: Map<String, String> = emptyMap()
    )

    /**
     * Loads and parses the specified configuration file(s) from their local
     * and/or remote locations.
     */
    fun load(
        context: Context,
        location: Location,
        options: LoaderOptions
    ) {
        if (loader == null) {
            loader = PathConfigurationLoader(context.applicationContext)
        }

        loader?.load(location, options) {
            cachedProperties.clear()
            rules = it.rules
            settings = it.settings
        }
    }

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
        cachedProperties[location]?.let { return it }

        val properties = PathConfigurationProperties()
        val path = path(location)

        for (rule in rules) {
            if (rule.matches(path)) properties.putAll(rule.properties)
        }

        cachedProperties[location] = properties

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

typealias PathConfigurationProperties = HashMap<String, String>
typealias PathConfigurationSettings = HashMap<String, String>

val PathConfigurationProperties.presentation: Presentation
    @SuppressLint("DefaultLocale") get() = try {
        val value = get("presentation") ?: "default"
        Presentation.valueOf(value.uppercase())
    } catch (e: IllegalArgumentException) {
        Presentation.DEFAULT
    }

val PathConfigurationProperties.queryStringPresentation: QueryStringPresentation
    @SuppressLint("DefaultLocale") get() = try {
        val value = get("query_string_presentation") ?: "default"
        QueryStringPresentation.valueOf(value.uppercase())
    } catch (e: IllegalArgumentException) {
        QueryStringPresentation.DEFAULT
    }

val PathConfigurationProperties.context: PresentationContext
    @SuppressLint("DefaultLocale") get() = try {
        val value = get("context") ?: "default"
        PresentationContext.valueOf(value.uppercase())
    } catch (e: IllegalArgumentException) {
        PresentationContext.DEFAULT
    }

val PathConfigurationProperties.uri: Uri?
    get() = get("uri")?.toUri()

val PathConfigurationProperties.fallbackUri: Uri?
    get() = get("fallback_uri")?.toUri()

val PathConfigurationProperties.title: String?
    get() = get("title")

val PathConfigurationProperties.pullToRefreshEnabled: Boolean
    get() = get("pull_to_refresh_enabled")?.toBoolean() ?: false
