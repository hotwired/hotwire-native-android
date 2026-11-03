package dev.hotwire.core.turbo.http

import dev.hotwire.core.logging.logError
import dev.hotwire.core.security.hasSameOriginAs
import dev.hotwire.core.turbo.util.dispatcherProvider
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

internal class HttpRepository {

    // Only same-origin hops are requested, so the cross-origin destination never gets a request
    // before the WebView visits it.
    suspend fun fetchCrossOriginRedirect(location: String): String? {
        return withContext(dispatcherProvider.io) {
            try {
                location.toHttpUrlOrNull()?.let { findCrossOriginRedirect(it) }
            } catch (e: Exception) {
                logError("httpRequestError", e)
                null
            }
        }
    }

    private fun findCrossOriginRedirect(start: HttpUrl): String? {
        val client = redirectClient()
        var url = start

        repeat(MAX_REDIRECTS) {
            val next = client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (!response.isRedirect) return null

                response.header("Location")
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { url.resolve(it) }
                    ?: return null
            }

            if (!next.toString().hasSameOriginAs(url.toString())) {
                return next.toString()
            }

            url = next
        }

        return null
    }

    // Derived on each call: Session replaces the shared client with a caching one after it
    // constructs this repository.
    private fun redirectClient(): OkHttpClient {
        return HotwireHttpClient.instance.newBuilder()
            .followRedirects(false)
            .build()
    }

    private companion object {
        // Matches OkHttp's own limit when it follows redirects.
        const val MAX_REDIRECTS = 20
    }
}
