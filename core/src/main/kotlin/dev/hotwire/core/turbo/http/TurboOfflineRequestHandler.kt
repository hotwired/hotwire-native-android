package dev.hotwire.core.turbo.http

import android.webkit.WebResourceResponse

@RequiresOptIn(message = "This API is experimental. It may be changed in the future without notice.")
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FIELD)
annotation class ExperimentalOfflineRequestHandler

@ExperimentalOfflineRequestHandler
interface TurboOfflineRequestHandler {
    fun getCacheStrategy(url: String): TurboOfflineCacheStrategy
    fun getCachedResponseHeaders(url: String): Map<String, String>?
    fun getCachedResponse(url: String, allowStaleResponse: Boolean = false): WebResourceResponse?
    fun getCachedSnapshot(url: String): WebResourceResponse?
    fun cacheResponse(url: String, response: WebResourceResponse): WebResourceResponse?
}
