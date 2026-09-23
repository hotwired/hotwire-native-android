package dev.hotwire.core.turbo.http

import android.os.Build
import android.webkit.CookieManager
import dev.hotwire.core.turbo.BaseRepositoryTest
import dev.hotwire.core.turbo.http.HttpRepository.HttpRedirect
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class HttpRepositoryTest : BaseRepositoryTest() {
    private val repository = HttpRepository()
    private val crossOriginServer = MockWebServer()

    override fun setup() {
        super.setup()
        crossOriginServer.start()
        seedSessionCookie()
    }

    override fun teardown() {
        super.teardown()
        crossOriginServer.shutdown()
        CookieManager.getInstance().removeAllCookies(null)
    }

    // Both mock servers run on the same loopback host, so this covers a redirect that crosses
    // origin by port. `flags an off-host redirect location as cross-origin` covers the host.
    @Test
    fun `does not forward credentials to a cross-origin redirect destination`() {
        val crossOriginUrl = crossOriginServer.url("/attacker").toString()

        server.enqueue(redirectResponse(crossOriginUrl))
        crossOriginServer.enqueue(MockResponse().setResponseCode(200))

        val redirect = runBlocking { repository.fetchRedirect(baseUrl()) }

        assertThat(crossOriginServer.requestCount).isEqualTo(0)

        val firstPartyRequest = server.takeRequest()
        assertThat(firstPartyRequest.headers["Cookie"]).contains("session=test-cookie")

        assertThat(redirect).isEqualTo(HttpRedirect(location = crossOriginUrl, isCrossOrigin = true))
    }

    @Test
    fun `flags an off-host redirect location as cross-origin`() {
        server.enqueue(redirectResponse(OFF_HOST_LOCATION))

        val redirect = runBlocking { repository.fetchRedirect(baseUrl()) }

        assertThat(redirect).isEqualTo(HttpRedirect(location = OFF_HOST_LOCATION, isCrossOrigin = true))

        // Nothing is sent on to the off-host destination. Were the redirect followed, the host
        // wouldn't resolve and the fetch would fail rather than report the redirect.
        assertThat(server.requestCount).isEqualTo(1)
    }

    @Test
    fun `flags a redirect that changes only the scheme as cross-origin`() {
        val otherSchemeUrl = server.url("/redirected").newBuilder().scheme("https").build().toString()

        server.enqueue(redirectResponse(otherSchemeUrl))

        val redirect = runBlocking { repository.fetchRedirect(baseUrl()) }

        assertThat(redirect).isEqualTo(HttpRedirect(location = otherSchemeUrl, isCrossOrigin = true))
    }

    @Test
    fun `detects a same-origin redirect without flagging it cross-origin`() {
        val sameOriginUrl = server.url("/redirected").toString()

        server.enqueue(redirectResponse(sameOriginUrl))

        val redirect = runBlocking { repository.fetchRedirect(baseUrl()) }

        assertThat(redirect).isEqualTo(HttpRedirect(location = sameOriginUrl, isCrossOrigin = false))

        // Same-origin redirects aren't followed either, so the destination is never fetched.
        assertThat(server.requestCount).isEqualTo(1)
    }

    @Test
    fun `resolves a relative redirect location against the request origin as same-origin`() {
        server.enqueue(redirectResponse("/relative/path"))

        val redirect = runBlocking { repository.fetchRedirect(baseUrl()) }

        assertThat(redirect).isEqualTo(
            HttpRedirect(location = server.url("/relative/path").toString(), isCrossOrigin = false)
        )
    }

    @Test
    fun `reports no redirect for a direct successful response`() {
        server.enqueue(MockResponse().setResponseCode(200))

        val redirect = runBlocking { repository.fetchRedirect(baseUrl()) }

        assertThat(redirect).isNull()
        assertThat(server.requestCount).isEqualTo(1)
    }

    @Test
    fun `closes the response so the connection is reused`() {
        repeat(2) {
            server.enqueue(redirectResponse("/next").setBody("You are being redirected."))
        }

        runBlocking {
            repository.fetchRedirect(baseUrl())
            repository.fetchRedirect(baseUrl())
        }

        assertThat(server.takeRequest().sequenceNumber).isEqualTo(0)
        assertThat(server.takeRequest().sequenceNumber).isEqualTo(1)
    }

    private fun redirectResponse(location: String): MockResponse {
        return MockResponse()
            .setResponseCode(302)
            .addHeader("Location", location)
    }

    private fun seedSessionCookie() {
        CookieManager.getInstance().setCookie(baseUrl(), "session=test-cookie")
    }

    companion object {
        private const val OFF_HOST_LOCATION = "https://redirect-destination.example/steal"
    }
}
