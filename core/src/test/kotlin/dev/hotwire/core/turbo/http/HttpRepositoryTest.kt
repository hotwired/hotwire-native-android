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
        CookieManager.getInstance().setCookie(baseUrl(), "session=test-cookie")
    }

    override fun teardown() {
        super.teardown()
        crossOriginServer.shutdown()
        CookieManager.getInstance().removeAllCookies(null)
    }

    @Test
    fun `returns a cross-origin redirect location without requesting it`() {
        val crossOriginUrl = crossOriginServer.url("/destination").toString()
        server.enqueue(redirectResponse(crossOriginUrl))

        assertThat(fetch()).isEqualTo(crossOriginUrl)
        assertThat(crossOriginServer.requestCount).isEqualTo(0)
        assertThat(server.takeRequest().headers["Cookie"]).contains("session=test-cookie")
    }

    @Test
    fun `follows same-origin redirects to a cross-origin redirect`() {
        server.enqueue(redirectResponse("/login"))
        server.enqueue(redirectResponse(OFF_HOST_LOCATION))

        assertThat(fetch()).isEqualTo(OFF_HOST_LOCATION)
        assertThat(server.requestCount).isEqualTo(2)

        server.takeRequest()
        val sameOriginHop = server.takeRequest()
        assertThat(sameOriginHop.path).isEqualTo("/login")
        assertThat(sameOriginHop.headers["Cookie"]).contains("session=test-cookie")
    }

    @Test
    fun `returns null for same-origin redirects that end in a response`() {
        server.enqueue(redirectResponse("/relative/path"))
        server.enqueue(MockResponse().setResponseCode(200))

        assertThat(fetch()).isNull()
        assertThat(server.requestCount).isEqualTo(2)

        server.takeRequest()
        assertThat(server.takeRequest().path).isEqualTo("/relative/path")
    }

    @Test
    fun `stops following a same-origin redirect loop`() {
        repeat(21) { server.enqueue(redirectResponse("/loop")) }

        assertThat(fetch()).isNull()
        assertThat(server.requestCount).isEqualTo(20)
    }

    @Test
    fun `returns null for a redirect with an empty location`() {
        server.enqueue(redirectResponse(""))

        assertThat(fetch()).isNull()
        assertThat(server.requestCount).isEqualTo(1)
    }

    @Test
    fun `returns null for a redirect to a non-http location`() {
        server.enqueue(redirectResponse("javascript:alert(1)"))

        assertThat(fetch()).isNull()
    }

    @Test
    fun `closes each response so the connection is reused`() {
        server.enqueue(redirectResponse("/next").setBody("You are being redirected."))
        server.enqueue(redirectResponse(OFF_HOST_LOCATION).setBody("You are being redirected."))

        fetch()
        assertThat(server.requestCount).isEqualTo(2)

        assertThat(server.takeRequest().sequenceNumber).isEqualTo(0)
        assertThat(server.takeRequest().sequenceNumber).isEqualTo(1)
    }

    private fun fetch(): String? {
        return runBlocking { repository.fetchCrossOriginRedirect(baseUrl()) }
    }

    private fun redirectResponse(location: String): MockResponse {
        return MockResponse()
            .setResponseCode(302)
            .addHeader("Location", location)
    }

    companion object {
        private const val OFF_HOST_LOCATION = "https://redirect-destination.example/auth"
    }
}
