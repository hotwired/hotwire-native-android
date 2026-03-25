package dev.hotwire.core.turbo.config

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import dev.hotwire.core.logging.logEvent
import dev.hotwire.core.turbo.config.PathConfigurationLoadState.Idle
import dev.hotwire.core.turbo.config.PathConfigurationLoadState.Loaded
import dev.hotwire.core.turbo.nav.Presentation
import dev.hotwire.core.turbo.nav.PresentationContext
import dev.hotwire.core.turbo.nav.QueryStringPresentation
import dev.hotwire.core.turbo.util.dispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Provides the ability to load, parse, and retrieve url path
 * properties from the app's JSON configuration file.
 */
class PathConfiguration {
    private val cachedProperties: HashMap<String, PathConfigurationProperties> = hashMapOf()
    private val loadingScope: CoroutineScope = CoroutineScope(dispatcherProvider.io + SupervisorJob())
    private val _loadState = MutableStateFlow<PathConfigurationLoadState>(Idle)

    internal var loader = PathConfigurationLoader()

    /**
     * A [StateFlow] that emits the current state of the path configuration
     * loading process. Observe this to know when the configuration has been
     * loaded and from which source (bundled asset, cached remote, or fresh remote).
     */
    val loadState: StateFlow<PathConfigurationLoadState> = _loadState.asStateFlow()

    /**
     * Gets the top-level settings specified in the app's path configuration.
     * The settings are map of key/value `String` items.
     */
    val settings: PathConfigurationSettings
        get() = synchronized(this) { currentConfiguration.settings }

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
        logEvent("pathConfigurationLoading", location.toString())

        val appContext = context.applicationContext

        loader.loadCachedOrBundledConfiguration(appContext, location)?.let {
            applyLoadedState(it)
        }

        loadingScope.launch {
            location.remoteFileUrl?.let { url ->
                loader.loadRemoteConfigurationForUrl(appContext, url, options)?.let {
                    applyLoadedState(it)
                }
            }
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
        synchronized(this) {
            cachedProperties[location]?.let { return it }

            val properties = currentConfiguration.properties(location)
            cachedProperties[location] = properties

            return properties
        }
    }

    private fun applyLoadedState(state: Loaded) = synchronized(this) {
        cachedProperties.clear()
        _loadState.value = state

        logEvent(
            "pathConfigurationUpdated", listOf(
                "Source" to state.javaClass.simpleName,
                "Rules" to state.configuration.rules.size,
                "Settings" to state.configuration.settings.size
            )
        )
    }

    private val currentConfiguration: PathConfigurationData
        get() = (_loadState.value as? Loaded)?.configuration ?: PathConfigurationData()
}

typealias PathConfigurationProperties = HashMap<String, Any>
typealias PathConfigurationSettings = HashMap<String, Any>

val PathConfigurationProperties.presentation: Presentation
    @SuppressLint("DefaultLocale") get() = try {
        val value = get("presentation")?.toString() ?: "default"
        Presentation.valueOf(value.uppercase())
    } catch (e: IllegalArgumentException) {
        Presentation.DEFAULT
    }

val PathConfigurationProperties.queryStringPresentation: QueryStringPresentation
    @SuppressLint("DefaultLocale") get() = try {
        val value = get("query_string_presentation")?.toString() ?: "default"
        QueryStringPresentation.valueOf(value.uppercase())
    } catch (e: IllegalArgumentException) {
        QueryStringPresentation.DEFAULT
    }

val PathConfigurationProperties.context: PresentationContext
    @SuppressLint("DefaultLocale") get() = try {
        val value = get("context")?.toString() ?: "default"
        PresentationContext.valueOf(value.uppercase())
    } catch (e: IllegalArgumentException) {
        PresentationContext.DEFAULT
    }

val PathConfigurationProperties.uri: Uri?
    get() = get("uri")?.toString()?.toUri()

val PathConfigurationProperties.fallbackUri: Uri?
    get() = get("fallback_uri")?.toString()?.toUri()

val PathConfigurationProperties.title: String?
    get() = get("title")?.toString()

val PathConfigurationProperties.pullToRefreshEnabled: Boolean
    get() = get("pull_to_refresh_enabled")?.let { it as Boolean } ?: false

val PathConfigurationProperties.animated: Boolean
    get() = get("animated")?.let { it as Boolean } ?: true

val PathConfigurationProperties.isHistoricalLocation: Boolean
    get() = get("historical_location")?.let { it as Boolean } ?: false
