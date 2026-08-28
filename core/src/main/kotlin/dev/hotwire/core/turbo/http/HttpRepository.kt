package dev.hotwire.core.turbo.http

import dev.hotwire.core.logging.logError
import dev.hotwire.core.turbo.util.dispatcherProvider
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
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
     * Inspects a response for a redirect and, when present, resolves the destination and whether
     * it is cross-origin.
     *
     * The verification request deliberately does not follow redirects (see [verificationClient]),
     * so the destination is determined solely from the `Location` header of the first response.
     * This guarantees no credentials (e.g. `Cookie`, `Authorization`) are ever forwarded to a
     * cross-origin redirect destination, while still giving the caller everything it needs to
     * detect and propose a cross-origin redirect visit.
     */
    private fun redirectFrom(response: Response): HttpRedirect? {
        if (!response.isRedirect) return null

        val locationHeader = response.header("Location") ?: return null
        val requestUrl = response.request.url
        val redirectUrl = requestUrl.resolve(locationHeader) ?: return null

        return HttpRedirect(
            location = redirectUrl.toString(),
            isCrossOrigin = !redirectUrl.isSameOriginAs(requestUrl)
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
     * A client dedicated to the native redirect-verification fetch, derived from the shared
     * client so it inherits its cache, timeouts, and interceptors. Redirects are disabled so a
     * cross-origin redirect can be detected from the `Location` header without ever sending the
     * credential-bearing request (`Cookie`/`Authorization`) on to the redirect destination.
     */
    private fun verificationClient(): OkHttpClient {
        return HotwireHttpClient.instance.newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
    }
}

/**
 * Two URLs share an origin only when their scheme, host, and (effective) port all match. Comparing
 * the full origin — not just the host — ensures a scheme downgrade (e.g. https → http) or a port
 * change is correctly treated as cross-origin.
 */
private fun HttpUrl.isSameOriginAs(other: HttpUrl): Boolean {
    return scheme == other.scheme && host == other.host && port == other.port
}
