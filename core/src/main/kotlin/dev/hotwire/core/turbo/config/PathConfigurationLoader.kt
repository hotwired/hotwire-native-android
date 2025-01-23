package dev.hotwire.core.turbo.config

import android.content.Context
import com.google.gson.reflect.TypeToken
import dev.hotwire.core.logging.logError
import dev.hotwire.core.logging.logEvent
import dev.hotwire.core.turbo.util.dispatcherProvider
import dev.hotwire.core.turbo.util.toObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

internal class PathConfigurationLoader(val context: Context) : CoroutineScope {
    internal var repository = PathConfigurationRepository()

    override val coroutineContext: CoroutineContext
        get() = dispatcherProvider.io + Job()

    fun load(location: PathConfiguration.Location, onCompletion: (PathConfiguration) -> Unit) {
        location.assetFilePath?.let {
            loadBundledAssetConfiguration(it, onCompletion)
        }

        location.remoteFileUrl?.let {
            downloadRemoteConfiguration(it, onCompletion)
        }
    }

    private fun downloadRemoteConfiguration(url: String, onCompletion: (PathConfiguration) -> Unit) {
        // Always load the previously cached version first, if available
        loadCachedConfigurationForUrl(url, onCompletion)

        launch {
            repository.getRemoteConfiguration(url)?.let { json ->
                load(json)?.let {
                    logEvent("remotePathConfigurationLoaded", url)
                    onCompletion(it)
                    cacheConfigurationForUrl(url, it)
                }
            }
        }
    }

    private fun loadBundledAssetConfiguration(filePath: String, onCompletion: (PathConfiguration) -> Unit) {
        val json = repository.getBundledConfiguration(context, filePath)
        load(json)?.let {
            logEvent("bundledPathConfigurationLoaded", filePath)
            onCompletion(it)
        }
    }

    private fun loadCachedConfigurationForUrl(url: String, onCompletion: (PathConfiguration) -> Unit) {
        repository.getCachedConfigurationForUrl(context, url)?.let { json ->
            load(json)?.let {
                logEvent("cachedPathConfigurationLoaded", url)
                onCompletion(it)
            }
        }
    }

    private fun cacheConfigurationForUrl(url: String, pathConfiguration: PathConfiguration) {
        repository.cacheConfigurationForUrl(context, url, pathConfiguration)
    }

    fun load(json: String): PathConfiguration? {
     return try {
            json.toObject(object : TypeToken<PathConfiguration>() {})
        } catch (e: Exception) {
            logError("pathConfiguredFailedToParse", e)
            null
        }
    }
}
