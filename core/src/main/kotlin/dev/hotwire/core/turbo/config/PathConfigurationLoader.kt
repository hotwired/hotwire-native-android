package dev.hotwire.core.turbo.config

import android.content.Context
import com.google.gson.reflect.TypeToken
import dev.hotwire.core.logging.logError
import dev.hotwire.core.logging.logEvent
import dev.hotwire.core.turbo.util.toObject

internal class PathConfigurationLoader {
    internal var repository = PathConfigurationRepository()

    fun loadCachedOrBundledConfiguration(
        context: Context,
        location: PathConfiguration.Location
    ): PathConfigurationLoadState.Loaded? {
        // Attempt to load the cached remote configuration for the url, if available
        if (location.remoteFileUrl != null) {
            loadCachedConfigurationForUrl(context, location.remoteFileUrl)
                ?.let { return it }
        }

        // Fall back to the bundled config when a cached config is not available
        return location.assetFilePath?.let { loadBundledAssetConfiguration(context, it) }
    }

    private fun loadBundledAssetConfiguration(
        context: Context,
        filePath: String
    ): PathConfigurationLoadState.Loaded.BundledAssetLoaded? {
        logEvent("bundledPathConfigurationLoading", filePath)

        val json = repository.getBundledConfiguration(context, filePath)
        return load(json)?.let {
            logEvent("bundledPathConfigurationLoaded", filePath)
            PathConfigurationLoadState.Loaded.BundledAssetLoaded(it)
        }
    }

    private fun loadCachedConfigurationForUrl(
        context: Context,
        url: String
    ): PathConfigurationLoadState.Loaded.CachedRemoteLoaded? {
        logEvent("cachedPathConfigurationLoading", url)

        val json = repository.getCachedConfigurationForUrl(context, url)
        val config = json?.let { load(it) }

        return if (config == null) {
            logEvent("cachedPathConfigurationFailedToLoad", url)
            null
        } else {
            logEvent("cachedPathConfigurationLoaded", url)
            PathConfigurationLoadState.Loaded.CachedRemoteLoaded(config)
        }
    }

    suspend fun loadRemoteConfigurationForUrl(
        context: Context,
        url: String,
        options: PathConfiguration.LoaderOptions
    ): PathConfigurationLoadState.Loaded.RemoteLoaded? {
        logEvent("remotePathConfigurationLoading", url)

        val config = repository.getRemoteConfiguration(url, options)?.let { json -> load(json) }

        return if (config == null) {
            null
        } else {
            logEvent("remotePathConfigurationLoaded", url)
            cacheConfigurationForUrl(context, url, config)
            PathConfigurationLoadState.Loaded.RemoteLoaded(config)
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
