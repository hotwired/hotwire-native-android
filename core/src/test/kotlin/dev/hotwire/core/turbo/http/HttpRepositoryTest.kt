package dev.hotwire.core.turbo.http

import android.os.Build
import android.webkit.CookieManager
import dev.hotwire.core.turbo.BaseRepositoryTest
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

        val result = runBlocking { repository.fetch(baseUrl()) }

        assertThat(crossOriginServer.requestCount).isEqualTo(0)

        val firstPartyRequest = server.takeRequest()
        assertThat(firstPartyRequest.headers["Cookie"]).contains("session=test-cookie")

        assertThat(result).isNotNull
        assertThat(result!!.redirect).isNotNull
        assertThat(result.redirect!!.isCrossOrigin).isTrue
        assertThat(result.redirect.location).isEqualTo(crossOriginUrl)
    }

    @Test
    fun `flags an off-host redirect location as cross-origin`() {
        server.enqueue(redirectResponse(OFF_HOST_LOCATION))

        val result = runBlocking { repository.fetch(baseUrl()) }

        assertThat(result).isNotNull
        assertThat(result!!.redirect).isNotNull
        assertThat(result.redirect!!.isCrossOrigin).isTrue
        assertThat(result.redirect.location).isEqualTo(OFF_HOST_LOCATION)

        // Nothing is sent on to the off-host destination. Were the redirect followed, the host
        // wouldn't resolve and the fetch would fail rather than report the redirect.
        assertThat(server.requestCount).isEqualTo(1)
    }

    @Test
    fun `flags a redirect that changes only the scheme as cross-origin`() {
        val otherSchemeUrl = server.url("/redirected").newBuilder().scheme("https").build().toString()

        server.enqueue(redirectResponse(otherSchemeUrl))

        val result = runBlocking { repository.fetch(baseUrl()) }

        assertThat(result).isNotNull
        assertThat(result!!.redirect).isNotNull
        assertThat(result.redirect!!.isCrossOrigin).isTrue
        assertThat(result.redirect.location).isEqualTo(otherSchemeUrl)
    }

    @Test
    fun `detects a same-origin redirect without flagging it cross-origin`() {
        server.enqueue(redirectResponse(server.url("/redirected").toString()))

        val result = runBlocking { repository.fetch(baseUrl()) }

        assertThat(result).isNotNull
        assertThat(result!!.redirect).isNotNull
        assertThat(result.redirect!!.isCrossOrigin).isFalse

        // Same-origin redirects aren't followed either, so the destination is never fetched.
        assertThat(server.requestCount).isEqualTo(1)
    }

    @Test
    fun `resolves a relative redirect location against the request origin as same-origin`() {
        server.enqueue(redirectResponse("/relative/path"))

        val result = runBlocking { repository.fetch(baseUrl()) }

        assertThat(result).isNotNull
        assertThat(result!!.redirect).isNotNull
        assertThat(result.redirect!!.isCrossOrigin).isFalse
        assertThat(result.redirect.location).isEqualTo(server.url("/relative/path").toString())
    }

    @Test
    fun `reports no redirect for a direct successful response`() {
        server.enqueue(MockResponse().setResponseCode(200))

        val result = runBlocking { repository.fetch(baseUrl()) }

        assertThat(result).isNotNull
        assertThat(result!!.response.isSuccessful).isTrue
        assertThat(result.redirect).isNull()
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
