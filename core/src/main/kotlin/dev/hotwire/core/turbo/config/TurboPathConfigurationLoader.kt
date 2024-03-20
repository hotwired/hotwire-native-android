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

internal class TurboPathConfigurationLoader(val context: Context) : CoroutineScope {
    internal var repository = TurboPathConfigurationRepository()

    override val coroutineContext: CoroutineContext
        get() = dispatcherProvider.io + Job()

    fun load(location: TurboPathConfiguration.Location, onCompletion: (TurboPathConfiguration) -> Unit) {
        location.assetFilePath?.let {
            loadBundledAssetConfiguration(it, onCompletion)
        }

        location.remoteFileUrl?.let {
            downloadRemoteConfiguration(it, onCompletion)
        }
    }

    private fun downloadRemoteConfiguration(url: String, onCompletion: (TurboPathConfiguration) -> Unit) {
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

    private fun loadBundledAssetConfiguration(filePath: String, onCompletion: (TurboPathConfiguration) -> Unit) {
        val json = repository.getBundledConfiguration(context, filePath)
        load(json)?.let {
            logEvent("bundledPathConfigurationLoaded", filePath)
            onCompletion(it)
        }
    }

    private fun loadCachedConfigurationForUrl(url: String, onCompletion: (TurboPathConfiguration) -> Unit) {
        repository.getCachedConfigurationForUrl(context, url)?.let { json ->
            load(json)?.let {
                logEvent("cachedPathConfigurationLoaded", url)
                onCompletion(it)
            }
        }
    }

    private fun cacheConfigurationForUrl(url: String, pathConfiguration: TurboPathConfiguration) {
        repository.cacheConfigurationForUrl(context, url, pathConfiguration)
    }

    private fun load(json: String) = try {
        json.toObject(object : TypeToken<TurboPathConfiguration>() {})
    } catch(e: Exception) {
        logError("pathConfiguredFailedToParse", e)
        null
    }
}
