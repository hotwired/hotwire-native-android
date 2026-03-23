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

internal class PathConfigurationLoader(val context: Context) : CoroutineScope {
    internal var repository = PathConfigurationRepository()

    private val _loadState = MutableStateFlow<PathConfigurationLoadState>(PathConfigurationLoadState.Idle)
    val loadState: StateFlow<PathConfigurationLoadState> = _loadState.asStateFlow()

    override val coroutineContext: CoroutineContext
        get() = dispatcherProvider.io + Job()

    fun load(
        location: PathConfiguration.Location,
        options: PathConfiguration.LoaderOptions,
        onCompletion: (PathConfiguration) -> Unit
    ) {
        location.assetFilePath?.let {
            loadBundledAssetConfiguration(it, onCompletion)
        }

        location.remoteFileUrl?.let {
            downloadRemoteConfiguration(it, options, onCompletion)
        }
    }

    private fun downloadRemoteConfiguration(
        url: String,
        options: PathConfiguration.LoaderOptions,
        onCompletion: (PathConfiguration) -> Unit
    ) {
        // Always load the previously cached version first, if available
        loadCachedConfigurationForUrl(url, onCompletion)

        launch {
            repository.getRemoteConfiguration(url, options)?.let { json ->
                load(json)?.let {
                    logEvent("remotePathConfigurationLoaded", url)
                    onCompletion(it)
                    _loadState.value = PathConfigurationLoadState.RemoteLoaded(it)
                    cacheConfigurationForUrl(url, it)
                }
            }
        }
    }

    private fun loadBundledAssetConfiguration(
        filePath: String,
        onCompletion: (PathConfiguration) -> Unit
    ) {
        val json = repository.getBundledConfiguration(context, filePath)
        load(json)?.let {
            logEvent("bundledPathConfigurationLoaded", filePath)
            onCompletion(it)
            _loadState.value = PathConfigurationLoadState.BundledAssetLoaded(it)
        }
    }

    private fun loadCachedConfigurationForUrl(
        url: String,
        onCompletion: (PathConfiguration) -> Unit
    ) {
        repository.getCachedConfigurationForUrl(context, url)?.let { json ->
            load(json)?.let {
                logEvent("cachedPathConfigurationLoaded", url)
                onCompletion(it)
                _loadState.value = PathConfigurationLoadState.CachedRemoteLoaded(it)
            }
        }
    }

    private fun cacheConfigurationForUrl(
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
