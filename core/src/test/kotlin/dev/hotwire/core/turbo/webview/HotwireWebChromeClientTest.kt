package dev.hotwire.core.turbo.webview

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import dev.hotwire.core.turbo.BaseUnitTest
import dev.hotwire.core.turbo.session.Session
import dev.hotwire.core.turbo.visit.VisitOptions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class HotwireWebChromeClientTest : BaseUnitTest() {
    private lateinit var context: Context
    private lateinit var session: Session
    private lateinit var webView: WebView
    private lateinit var chromeClient: HotwireWebChromeClient

    @Before
    override fun setup() {
        super.setup()
        context = ApplicationProvider.getApplicationContext()
        session = mock()
        webView = WebView(context)
        chromeClient = HotwireWebChromeClient(session)
    }

    @Test
    fun createWindowWithoutTransportIsNotHandled() {
        val message = Message.obtain(Handler(Looper.getMainLooper()))

        assertThat(chromeClient.onCreateWindow(webView, false, true, message)).isFalse()
        assertThat(chromeClient.onCreateWindow(webView, false, true, null)).isFalse()
    }

    @Test
    fun createWindowProvidesWindowWebView() {
        val transport = webView.WebViewTransport()
        val message = Message.obtain(Handler(Looper.getMainLooper()), 0, transport)

        assertThat(chromeClient.onCreateWindow(webView, false, true, message)).isTrue()
        assertThat(transport.webView).isNotNull()
        assertThat(transport.webView).isNotSameAs(webView)
    }

    @Test
    fun windowOverridingUrlLoadingProposesVisit() {
        val window = createWindow()
        val location = "https://example.com/page"

        val overridden = window.webViewClient().shouldOverrideUrlLoading(window, request(location))

        assertThat(overridden).isTrue()
        verify(session).visitProposedToLocation(location, VisitOptions())
    }

    @Test
    fun windowStartingPageLoadProposesVisit() {
        val window = createWindow()
        val location = "https://example.com/page"

        window.webViewClient().onPageStarted(window, location, null)

        verify(session).visitProposedToLocation(location, VisitOptions())
    }

    @Test
    fun windowProposesOnlyOneVisit() {
        val window = createWindow()
        val location = "https://example.com/page"

        window.webViewClient().shouldOverrideUrlLoading(window, request(location))
        window.webViewClient().onPageStarted(window, location, null)
        window.webViewClient().shouldOverrideUrlLoading(window, request("https://example.com/other"))

        verify(session, times(1)).visitProposedToLocation(any(), any())
    }

    @Test
    fun windowLoadingAboutBlankDoesNotProposeVisit() {
        val window = createWindow()
        val location = "https://example.com/page"

        window.webViewClient().onPageStarted(window, "about:blank", null)
        verify(session, never()).visitProposedToLocation(any(), any())

        window.webViewClient().shouldOverrideUrlLoading(window, request(location))
        verify(session).visitProposedToLocation(location, VisitOptions())
    }

    @Test
    fun windowIsDestroyedAfterProposingVisit() {
        val window = createWindow()

        window.webViewClient().shouldOverrideUrlLoading(window, request("https://example.com/page"))
        assertThat(shadowOf(window).wasDestroyCalled()).isFalse()

        idleMainLooper()
        assertThat(shadowOf(window).wasDestroyCalled()).isTrue()
    }

    @Test
    fun windowIsDestroyedWhenClosed() {
        val window = createWindow()

        window.webChromeClient().onCloseWindow(window)
        idleMainLooper()

        assertThat(shadowOf(window).wasDestroyCalled()).isTrue()
        window.webViewClient().shouldOverrideUrlLoading(window, request("https://example.com/page"))
        verify(session, never()).visitProposedToLocation(any(), any())
    }

    @Test
    fun previousWindowIsDestroyedWhenNewWindowIsCreated() {
        val previousWindow = createWindow()
        val window = createWindow()
        val location = "https://example.com/page"

        assertThat(shadowOf(previousWindow).wasDestroyCalled()).isTrue()
        assertThat(shadowOf(window).wasDestroyCalled()).isFalse()

        previousWindow.webViewClient().shouldOverrideUrlLoading(previousWindow, request(location))
        verify(session, never()).visitProposedToLocation(any(), any())

        window.webViewClient().shouldOverrideUrlLoading(window, request(location))
        verify(session).visitProposedToLocation(location, VisitOptions())
    }

    private fun createWindow(): WebView {
        val transport = webView.WebViewTransport()
        val message = Message.obtain(Handler(Looper.getMainLooper()), 0, transport)
        chromeClient.onCreateWindow(webView, false, true, message)
        idleMainLooper()
        return requireNotNull(transport.webView)
    }

    private fun WebView.webViewClient(): WebViewClient {
        return shadowOf(this).webViewClient
    }

    private fun WebView.webChromeClient(): WebChromeClient {
        return shadowOf(this).webChromeClient
    }

    private fun request(location: String): WebResourceRequest {
        return mock<WebResourceRequest>().also {
            whenever(it.url).thenReturn(Uri.parse(location))
        }
    }

    private fun idleMainLooper() {
        shadowOf(Looper.getMainLooper()).idle()
    }
}
