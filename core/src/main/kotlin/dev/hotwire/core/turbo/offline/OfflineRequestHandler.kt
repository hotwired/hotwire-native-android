package dev.hotwire.core.turbo.offline

import android.webkit.WebResourceResponse

/**
 * Experimental: API may change, not ready for production use.
 */
interface OfflineRequestHandler {
    fun getCacheStrategy(url: String): OfflineCacheStrategy
    fun getCachedResponseHeaders(url: String): Map<String, String>?
    fun getCachedResponse(url: String, allowStaleResponse: Boolean = false): WebResourceResponse?
    fun getCachedSnapshot(url: String): WebResourceResponse?
    fun cacheResponse(url: String, response: WebResourceResponse): WebResourceResponse?
}
