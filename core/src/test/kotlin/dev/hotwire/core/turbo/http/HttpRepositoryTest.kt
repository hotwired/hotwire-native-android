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

    @Test
    fun `does not forward credentials to a cross-origin redirect destination`() {
        val crossOriginUrl = crossOriginServer.url("/attacker").toString()

        // First-party origin issues a redirect to a cross-origin destination.
        server.enqueue(redirectResponse(crossOriginUrl))
        // If a credential-bearing request were (incorrectly) followed to the destination, it would
        // land here. It must never be reached.
        crossOriginServer.enqueue(MockResponse().setResponseCode(200))

        val result = runBlocking { repository.fetch(baseUrl()) }

        // Security invariant: the cross-origin destination received no request at all, so no
        // Cookie or other credential could possibly have leaked to it.
        assertThat(crossOriginServer.requestCount).isEqualTo(0)

        // The credentialed request was sent only to the first-party origin.
        val firstPartyRequest = server.takeRequest()
        assertThat(firstPartyRequest.headers["Cookie"]).contains("session=test-cookie")

        // The caller still gets what it needs: the cross-origin redirect is detected (without
        // following it) so it can propose the cross-origin redirect visit.
        assertThat(result).isNotNull
        assertThat(result!!.redirect).isNotNull
        assertThat(result.redirect!!.isCrossOrigin).isTrue
        assertThat(result.redirect.location).isEqualTo(crossOriginUrl)
    }

    @Test
    fun `detects a same-origin redirect without flagging it cross-origin`() {
        server.enqueue(redirectResponse(server.url("/redirected").toString()))

        val result = runBlocking { repository.fetch(baseUrl()) }

        assertThat(result).isNotNull
        assertThat(result!!.redirect).isNotNull
        assertThat(result.redirect!!.isCrossOrigin).isFalse

        // The redirect is not followed, so the same-origin destination is not fetched.
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
}
