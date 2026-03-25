package dev.hotwire.core.turbo.config

import android.content.Context
import com.google.gson.reflect.TypeToken
import dev.hotwire.core.logging.logError
import dev.hotwire.core.logging.logEvent
import dev.hotwire.core.turbo.util.toObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class PathConfigurationLoader {
    internal var repository = PathConfigurationRepository()

    private val _loadState = MutableStateFlow<PathConfigurationLoadState>(PathConfigurationLoadState.Idle)
    val loadState: StateFlow<PathConfigurationLoadState> = _loadState.asStateFlow()

    suspend fun load(
        context: Context,
        location: PathConfiguration.Location,
        options: PathConfiguration.LoaderOptions
    ) {
        // Attempt to load the cached remote configuration for the url, if available
        val cachedLoaded = if (location.remoteFileUrl != null) {
            loadCachedConfigurationForUrl(context, location.remoteFileUrl)
        } else {
            false
        }

        // Only load the bundled config if a cached config is not available
        if (!cachedLoaded) {
            location.assetFilePath?.let {
                loadBundledAssetConfiguration(context, it)
            }
        }

        // Load a fresh remote config from the server
        location.remoteFileUrl?.let { url ->
            downloadRemoteConfigurationForUrl(context, url, options)
        }
    }

    private fun loadBundledAssetConfiguration(
        context: Context,
        filePath: String
    ) {
        logEvent("bundledPathConfigurationLoading", filePath)

        val json = repository.getBundledConfiguration(context, filePath)
        load(json)?.let {
            logEvent("bundledPathConfigurationLoaded", filePath)
            _loadState.value = PathConfigurationLoadState.BundledAssetLoaded(it)
        }
    }

    private fun loadCachedConfigurationForUrl(
        context: Context,
        url: String
    ): Boolean {
        logEvent("cachedPathConfigurationLoading", url)

        val json = repository.getCachedConfigurationForUrl(context, url)
        val config = json?.let { load(it) }

        return if (config != null) {
            logEvent("cachedPathConfigurationLoaded", url)
            _loadState.value = PathConfigurationLoadState.CachedRemoteLoaded(config)
            true
        } else {
            logEvent("cachedPathConfigurationFailedToLoad", url)
            false
        }
    }

    private suspend fun downloadRemoteConfigurationForUrl(
        context: Context,
        url: String,
        options: PathConfiguration.LoaderOptions
    ) {
        logEvent("remotePathConfigurationLoading", url)

        repository.getRemoteConfiguration(url, options)?.let { json ->
            load(json)?.let {
                logEvent("remotePathConfigurationLoaded", url)
                _loadState.value = PathConfigurationLoadState.RemoteLoaded(it)
                cacheConfigurationForUrl(context, url, it)
            }
        }
    }

    private fun cacheConfigurationForUrl(
        context: Context,
        url: String,
        pathConfiguration: PathConfigurationData
    ) {
        repository.cacheConfigurationForUrl(context, url, pathConfiguration)
    }

    private fun load(json: String) = try {
        json.toObject(object : TypeToken<PathConfigurationData>() {})
    } catch(e: Exception) {
        logError("pathConfiguredFailedToParse", e)
        null
    }
}
