package dev.hotwire.core.turbo.http

import dev.hotwire.core.security.Origin
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Keeps the caller's headers from following a redirect to another origin.
 *
 * OkHttp forwards a request's headers to the redirect target and only drops `Authorization`
 * when the origin changes. Apps can pass credentials in any header, for example through the path
 * configuration's `httpHeaders`, so a hop to a different origin than the original request gets
 * none of the caller's headers.
 *
 * Cookies that the client's `CookieJar` loads for the hop are the destination's own, so they
 * stay. A redirect back to the original origin gets the caller's headers again, because OkHttp
 * builds each follow-up from its own copy of the request, not from the one this interceptor sends.
 */
internal class CrossOriginCredentialInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val originalRequest = chain.call().request()

        if (request.url.hasSameOriginAs(originalRequest.url)) {
            return chain.proceed(request)
        }

        val forwarded = request.newBuilder().apply {
            originalRequest.headers.names().forEach { removeHeader(it) }
        }.build()

        return chain.proceed(forwarded)
    }

    private fun HttpUrl.hasSameOriginAs(other: HttpUrl) =
        Origin.parseOrNull(toString()) == Origin.parseOrNull(other.toString())
}
