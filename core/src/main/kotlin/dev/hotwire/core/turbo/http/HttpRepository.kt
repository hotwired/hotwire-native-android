package dev.hotwire.core.turbo.http

import dev.hotwire.core.logging.logError
import dev.hotwire.core.security.hasSameOriginAs
import dev.hotwire.core.turbo.util.dispatcherProvider
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

internal class HttpRepository {

    data class HttpRequestResult(
        val response: Response,
        val redirect: HttpRedirect?
    )

    data class HttpRedirect(
        val location: String,
        val isCrossOrigin: Boolean
    )

    suspend fun fetch(location: String): HttpRequestResult? {
        return withContext(dispatcherProvider.io) {
            val response = issueRequest(location)

            if (response != null) {
                HttpRequestResult(
                    response = response,
                    redirect = redirectFrom(response)
                )
            } else {
                null
            }
        }
    }

    /**
     * Resolves an unfollowed redirect from its `Location` header, so the caller can detect a
     * cross-origin destination without a credentialed request ever reaching it.
     */
    private fun redirectFrom(response: Response): HttpRedirect? {
        if (!response.isRedirect) return null

        val locationHeader = response.header("Location") ?: return null
        val requestUrl = response.request.url
        val redirectLocation = requestUrl.resolve(locationHeader)?.toString() ?: return null

        return HttpRedirect(
            location = redirectLocation,
            isCrossOrigin = !redirectLocation.hasSameOriginAs(requestUrl.toString())
        )
    }

    private fun issueRequest(location: String): Response? {
        return try {
            val request = buildRequest(location)
            verificationClient().newCall(request).execute()
        } catch (e: Exception) {
            logError("httpRequestError", e)
            null
        }
    }

    private fun buildRequest(location: String): Request {
        return Request.Builder().url(location).build()
    }

    /**
     * Derived from the shared client on each call, so it picks up a cache or timeout change, and
     * built without redirect following: this fetch only needs to see where a redirect points.
     */
    private fun verificationClient(): OkHttpClient {
        return HotwireHttpClient.instance.newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
    }
}
