package dev.hotwire.core.turbo.http

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

// The two servers share a host, so only their ports, and so their origins, differ.
class CrossOriginCredentialInterceptorTest {
    private val firstPartyServer = MockWebServer()
    private val crossOriginServer = MockWebServer()

    private val client = OkHttpClient.Builder()
        .addNetworkInterceptor(CrossOriginCredentialInterceptor())
        .build()

    @Before
    fun setup() {
        firstPartyServer.start()
        crossOriginServer.start()
    }

    @After
    fun teardown() {
        firstPartyServer.shutdown()
        crossOriginServer.shutdown()
    }

    @Test
    fun `strips the caller's headers on a cross-origin redirect`() {
        firstPartyServer.enqueue(redirect(crossOriginServer.url("/config")))
        crossOriginServer.enqueue(MockResponse())

        client.newCall(callerRequest()).execute().close()

        assertThat(firstPartyServer.takeRequest().callerHeaders()).containsOnlyKeys(CALLER_HEADERS.keys)
        assertThat(crossOriginServer.takeRequest().callerHeaders()).isEmpty()
    }

    @Test
    fun `keeps the caller's headers on a same-origin redirect`() {
        firstPartyServer.enqueue(redirect(firstPartyServer.url("/config")))
        firstPartyServer.enqueue(MockResponse())

        client.newCall(callerRequest()).execute().close()

        firstPartyServer.takeRequest()
        assertThat(firstPartyServer.takeRequest().callerHeaders()).isEqualTo(CALLER_HEADERS)
    }

    @Test
    fun `compares every hop with the original request's origin, not the previous hop's`() {
        // B -> C doesn't cross origins, but OkHttp builds it from its own copy of the request, which
        // still has the caller's headers.
        firstPartyServer.enqueue(redirect(crossOriginServer.url("/b")))
        crossOriginServer.enqueue(redirect(crossOriginServer.url("/c")))
        crossOriginServer.enqueue(MockResponse())

        client.newCall(callerRequest()).execute().close()

        crossOriginServer.takeRequest()
        assertThat(crossOriginServer.takeRequest().callerHeaders()).isEmpty()
    }

    @Test
    fun `keeps the redirect destination's own cookies on a cross-origin redirect`() {
        val destinationCookies = object : CookieJar {
            override fun loadForRequest(url: HttpUrl) = if (url.port == crossOriginServer.port) {
                listOfNotNull(Cookie.parse(url, "destination=own"))
            } else {
                emptyList()
            }

            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {}
        }
        val clientWithCookies = client.newBuilder().cookieJar(destinationCookies).build()
        firstPartyServer.enqueue(redirect(crossOriginServer.url("/config")))
        crossOriginServer.enqueue(MockResponse())

        val request = Request.Builder()
            .url(firstPartyServer.url("/"))
            .header("X-Api-Token", "secret")
            .build()

        clientWithCookies.newCall(request).execute().close()

        val crossOrigin = crossOriginServer.takeRequest()
        assertThat(crossOrigin.headers["Cookie"]).isEqualTo("destination=own")
        assertThat(crossOrigin.headers["X-Api-Token"]).isNull()
    }

    private fun callerRequest() = Request.Builder()
        .url(firstPartyServer.url("/"))
        .apply { CALLER_HEADERS.forEach { (name, value) -> header(name, value) } }
        .build()

    private fun RecordedRequest.callerHeaders() =
        CALLER_HEADERS.keys.mapNotNull { name -> headers[name]?.let { name to it } }.toMap()

    private fun redirect(location: HttpUrl) =
        MockResponse().setResponseCode(302).addHeader("Location", location.toString())

    private companion object {
        val CALLER_HEADERS = mapOf(
            "Authorization" to "Bearer secret",
            "X-Api-Token" to "secret",
            "Cookie" to "session=secret"
        )
    }
}
