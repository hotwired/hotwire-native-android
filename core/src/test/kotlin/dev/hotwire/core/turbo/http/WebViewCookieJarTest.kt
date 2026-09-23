package dev.hotwire.core.turbo.http

import android.os.Build
import android.webkit.CookieManager
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class WebViewCookieJarTest {
    private val cookieJar = WebViewCookieJar()
    private val cookieManager = CookieManager.getInstance()
    private val server = MockWebServer()

    private val greenUrl = "https://green.example.com/".toHttpUrl()
    private val blueUrl = "https://blue.example.com/".toHttpUrl()

    @Before
    fun setup() {
        cookieManager.removeAllCookies(null)
        server.start()
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun `the shared http client uses the web view cookie jar`() {
        assertThat(HotwireHttpClient.instance.cookieJar).isInstanceOf(WebViewCookieJar::class.java)
    }

    @Test
    fun `loadForRequest returns cookies stored for the request host`() {
        cookieManager.setCookie(greenUrl.toString(), "flavor=oatmeal")

        val cookies = cookieJar.loadForRequest(greenUrl)

        assertThat(cookies).hasSize(1)
        assertThat(cookies[0].name).isEqualTo("flavor")
        assertThat(cookies[0].value).isEqualTo("oatmeal")
    }

    @Test
    fun `loadForRequest returns all cookies stored for the request host`() {
        cookieManager.setCookie(greenUrl.toString(), "flavor=oatmeal")
        cookieManager.setCookie(greenUrl.toString(), "size=large")

        val cookies = cookieJar.loadForRequest(greenUrl)

        assertThat(cookies.map { it.name }).containsExactlyInAnyOrder("flavor", "size")
    }

    @Test
    fun `loadForRequest returns an empty list when no cookies are stored`() {
        val cookies = cookieJar.loadForRequest(greenUrl)

        assertThat(cookies).isEmpty()
    }

    @Test
    fun `loadForRequest returns only the cookies for the request host`() {
        cookieManager.setCookie(greenUrl.toString(), "flavor=oatmeal")

        val cookies = cookieJar.loadForRequest(blueUrl)

        assertThat(cookies).isEmpty()
    }

    @Test
    fun `saveFromResponse stores cookies for the response host`() {
        val cookie = Cookie.parse(greenUrl, "flavor=oatmeal")!!

        cookieJar.saveFromResponse(greenUrl, listOf(cookie))

        assertThat(cookieManager.getCookie(greenUrl.toString())).contains("flavor=oatmeal")
    }

    @Test
    fun `saveFromResponse stores cookies only for the response host`() {
        val cookie = Cookie.parse(blueUrl, "flavor=oatmeal")!!

        cookieJar.saveFromResponse(blueUrl, listOf(cookie))

        assertThat(cookieManager.getCookie(greenUrl.toString())).isNull()
    }

    @Test
    fun `requests include the cookies stored for their host`() {
        val url = server.url("/")
        cookieManager.setCookie(url.toString(), "flavor=oatmeal")
        server.enqueue(MockResponse())

        client().newCall(Request.Builder().url(url).build()).execute()

        assertThat(server.takeRequest().getHeader("Cookie")).isEqualTo("flavor=oatmeal")
    }

    @Test
    fun `response cookies are stored for the response host`() {
        val url = server.url("/")
        server.enqueue(MockResponse().addHeader("Set-Cookie", "flavor=oatmeal"))

        client().newCall(Request.Builder().url(url).build()).execute()

        assertThat(cookieManager.getCookie(url.toString())).contains("flavor=oatmeal")
    }

    @Test
    fun `cookies are loaded fresh for each request when following redirects`() {
        val url = server.url("/")
        server.enqueue(
            MockResponse()
                .setResponseCode(302)
                .addHeader("Location", server.url("/next"))
                .addHeader("Set-Cookie", "flavor=oatmeal")
        )
        server.enqueue(MockResponse())

        client().newCall(Request.Builder().url(url).build()).execute()

        server.takeRequest()
        assertThat(server.takeRequest().getHeader("Cookie")).isEqualTo("flavor=oatmeal")
    }

    private fun client(): OkHttpClient {
        return OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .build()
    }
}
