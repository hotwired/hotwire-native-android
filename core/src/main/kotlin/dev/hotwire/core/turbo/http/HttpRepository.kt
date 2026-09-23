package dev.hotwire.core.turbo.http

import dev.hotwire.core.logging.logError
import dev.hotwire.core.security.hasSameOriginAs
import dev.hotwire.core.turbo.util.dispatcherProvider
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

internal class HttpRepository {

    data class HttpRedirect(
        val location: String,
        val isCrossOrigin: Boolean
    )

    /**
     * Requests [location] without following redirects, so no credentialed request reaches the
     * destination. Returns null when the response is not a redirect or the request fails.
     */
    suspend fun fetchRedirect(location: String): HttpRedirect? {
        return withContext(dispatcherProvider.io) {
            issueRequest(location)?.use { redirectFrom(it) }
        }
    }

    private fun redirectFrom(response: Response): HttpRedirect? {
        if (!response.isRedirect) return null

        val requestUrl = response.request.url
        val redirectLocation = response.header("Location")
            ?.let { requestUrl.resolve(it) }
            ?.toString()
            ?: return null

        return HttpRedirect(
            location = redirectLocation,
            isCrossOrigin = !redirectLocation.hasSameOriginAs(requestUrl.toString())
        )
    }

    private fun issueRequest(location: String): Response? {
        return try {
            val request = Request.Builder().url(location).build()
            redirectClient().newCall(request).execute()
        } catch (e: Exception) {
            logError("httpRequestError", e)
            null
        }
    }

    // Derived on each call: Session replaces the shared client with a caching one after it
    // constructs this repository.
    private fun redirectClient(): OkHttpClient {
        return HotwireHttpClient.instance.newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
    }
}
