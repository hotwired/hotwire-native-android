package dev.hotwire.core.turbo.http

import android.webkit.CookieManager
import dev.hotwire.core.logging.logError
import dev.hotwire.core.turbo.util.dispatcherProvider
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response

internal class HttpRepository {
    private val cookieManager = CookieManager.getInstance()

    data class Result(
        val response: Response,
        val redirectToLocation: String?,
        val redirectIsCrossOrigin: Boolean
    )

    suspend fun fetch(location: String): Result? {
        return withContext(dispatcherProvider.io) {
            val response = issueRequest(location)

            if (response != null) {
                // Determine if there was a redirect, based on the final response's request url
                val responseUrl = response.request.url
                val isRedirect = location != responseUrl.toString()
                val redirectIsCrossOrigin = isRedirect && location.toHttpUrl().host != responseUrl.host

                Result(
                    response = response,
                    redirectToLocation = if (isRedirect) responseUrl.toString() else null,
                    redirectIsCrossOrigin = redirectIsCrossOrigin
                )
            } else {
                null
            }
        }
    }

    private fun issueRequest(location: String): Response? {
        return try {
            val request = buildRequest(location)
            HotwireHttpClient.instance.newCall(request).execute()
        } catch (e: Exception) {
            logError("httpRequestError", e)
            null
        }
    }

    private fun buildRequest(location: String): Request {
        val builder = Request.Builder().url(location)

        cookieManager.getCookie(location)?.let {
            builder.header("Cookie", it)
        }

        return builder.build()
    }
}
