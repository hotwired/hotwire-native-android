package dev.hotwire.core.turbo.config

import android.content.Context
import com.google.gson.reflect.TypeToken
import dev.hotwire.core.logging.logError
import dev.hotwire.core.logging.logEvent
import dev.hotwire.core.turbo.util.dispatcherProvider
import dev.hotwire.core.turbo.util.toObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

internal class PathConfigurationLoader : CoroutineScope {
    internal var repository = PathConfigurationRepository()

    private val _loadState = MutableStateFlow<PathConfigurationLoadState>(PathConfigurationLoadState.Idle)
    val loadState: StateFlow<PathConfigurationLoadState> = _loadState.asStateFlow()

    override val coroutineContext: CoroutineContext
        get() = dispatcherProvider.io + Job()

    fun load(
        context: Context,
        location: PathConfiguration.Location,
        options: PathConfiguration.LoaderOptions
    ) {
        location.assetFilePath?.let {
            loadBundledAssetConfiguration(context, it)
        }

        location.remoteFileUrl?.let { url ->
            loadCachedConfigurationForUrl(context, url)

            launch {
                downloadRemoteConfigurationForUrl(context, url, options)
            }
        }
    }

    private fun loadBundledAssetConfiguration(
        context: Context,
        filePath: String
    ) {
        val json = repository.getBundledConfiguration(context, filePath)
        load(json)?.let {
            logEvent("bundledPathConfigurationLoaded", filePath)
            _loadState.value = PathConfigurationLoadState.BundledAssetLoaded(it)
        }
    }

    private fun loadCachedConfigurationForUrl(
        context: Context,
        url: String
    ) {
        repository.getCachedConfigurationForUrl(context, url)?.let { json ->
            load(json)?.let {
                logEvent("cachedPathConfigurationLoaded", url)
                _loadState.value = PathConfigurationLoadState.CachedRemoteLoaded(it)
            }
        }
    }

    private suspend fun downloadRemoteConfigurationForUrl(
        context: Context,
        url: String,
        options: PathConfiguration.LoaderOptions
    ) {
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
        pathConfiguration: PathConfiguration
    ) {
        repository.cacheConfigurationForUrl(context, url, pathConfiguration)
    }

    private fun load(json: String) = try {
        json.toObject(object : TypeToken<PathConfiguration>() {})
    } catch(e: Exception) {
        logError("pathConfiguredFailedToParse", e)
        null
    }
}
