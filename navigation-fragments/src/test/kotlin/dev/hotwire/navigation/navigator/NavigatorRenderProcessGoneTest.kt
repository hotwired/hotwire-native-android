package dev.hotwire.navigation.navigator

import android.content.Context
import android.net.http.SslCertificate
import android.net.http.SslError
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.children
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockito_kotlin.mock
import dev.hotwire.core.config.Hotwire
import dev.hotwire.core.turbo.config.PathConfiguration
import dev.hotwire.core.turbo.webview.HotwireWebChromeClient
import dev.hotwire.core.turbo.webview.HotwireWebView
import dev.hotwire.navigation.R
import dev.hotwire.navigation.activities.HotwireActivity
import dev.hotwire.navigation.config.registerFragmentDestinations
import dev.hotwire.navigation.destinations.HotwireDestination
import dev.hotwire.navigation.destinations.HotwireDestinationDeepLink
import dev.hotwire.navigation.fragments.HotwireBottomSheetFragment
import dev.hotwire.navigation.fragments.HotwireWebBottomSheetFragment
import dev.hotwire.navigation.fragments.HotwireWebFragment
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

// All WebViews in an app share one render process, so every Session's
// WebView is gone at the same time. The start location is configured with
// `clear_all`, so routing it again wouldn't navigate.
@RunWith(RobolectricTestRunner::class)
class NavigatorRenderProcessGoneTest {

    private lateinit var controller: ActivityController<TestActivity>
    private lateinit var navigator: Navigator
    private lateinit var tabNavigator: Navigator
    private lateinit var goneWebViews: List<HotwireWebView>
    private var webViewsCreated = 0

    @Before
    fun setUp() {
        Hotwire.config.makeCustomWebView = { context ->
            webViewsCreated++
            TestWebView(context)
        }
        Hotwire.loadPathConfiguration(
            context = ApplicationProvider.getApplicationContext(),
            location = PathConfiguration.Location(assetFilePath = "json/render-process-gone-configuration.json")
        )
        Hotwire.registerFragmentDestinations(
            TestWebFragment::class,
            HotwireWebBottomSheetFragment::class,
            NativeBottomSheetFragment::class
        )

        controller = Robolectric.buildActivity(TestActivity::class.java).setup()
        navigator = navigator(HOST_ID)
        tabNavigator = navigator(TAB_HOST_ID)
        idle()
    }

    @After
    fun tearDown() {
        Hotwire.loadPathConfiguration(
            context = ApplicationProvider.getApplicationContext(),
            location = PathConfiguration.Location(assetFilePath = "json/empty-configuration.json")
        )
        Hotwire.registerFragmentDestinations(HotwireWebFragment::class, HotwireWebBottomSheetFragment::class)
        Hotwire.config.makeCustomWebView = { context -> HotwireWebView(context, null) }
        TestWebFragment.detachedWebViews.clear()
    }

    @Test
    fun `recovers the current destination in place`() {
        val destination = navigator.currentDestination

        renderProcessGone()

        assertRecovered()
        assertThat(navigator.currentDestination).isSameAs(destination)
        assertWebViewAttached(destination)
    }

    @Test
    fun `recovers a navigator host that isn't visible`() {
        val destination = tabNavigator.currentDestination

        renderProcessGone()

        assertRecovered()
        assertThat(tabNavigator.currentDestination).isSameAs(destination)
        assertWebViewAttached(destination, tabNavigator)
    }

    @Test
    fun `recovers without changing the back stack`() {
        navigator.route("$BASE_URL/page")
        idle()
        val destination = navigator.currentDestination

        renderProcessGone()

        assertRecovered()
        assertThat(navigator.currentDestination).isSameAs(destination)
        assertThat(navigator.previousLocation).isEqualTo(START_LOCATION)

        navigator.pop()
        idle()

        assertThat(navigator.isAtStartDestination()).isTrue()
        assertWebViewAttached(navigator.currentDestination)
    }

    @Test
    fun `recovers a web page under a web bottom sheet`() {
        val page = navigator.currentDestination
        navigator.route("$BASE_URL/web-sheet")
        idle()
        val sheet = navigator.currentDestination

        renderProcessGone()

        assertRecovered()
        assertThat(navigator.currentDestination).isSameAs(sheet)
        assertWebViewAttached(sheet)

        navigator.pop()
        idle()

        assertThat(navigator.currentDestination).isSameAs(page)
        assertWebViewAttached(page)
    }

    @Test
    fun `recovers a web page under a native bottom sheet`() {
        val page = navigator.currentDestination
        navigator.route("$BASE_URL/native-sheet")
        idle()

        // The page detached its WebView for the sheet, so it recovers when it's shown again
        renderProcessGone()
        navigator.pop()
        idle()

        assertRecovered()
        assertThat(navigator.currentDestination).isSameAs(page)
        assertThat(navigator.isAtStartDestination()).isTrue()
        assertWebViewAttached(page)
    }

    @Test
    fun `recovers when the render process is gone in the background`() {
        val destination = navigator.currentDestination

        controller.pause().stop()
        renderProcessGone()
        controller.start().resume()
        idle()

        assertRecovered()
        assertThat(navigator.currentDestination).isSameAs(destination)
        assertWebViewAttached(destination)
    }

    @Test
    fun `recovers a web bottom sheet and the page under it in the background`() {
        val page = navigator.currentDestination
        navigator.route("$BASE_URL/web-sheet")
        idle()
        val sheet = navigator.currentDestination

        controller.pause().stop()
        renderProcessGone()
        controller.start().resume()
        idle()

        assertRecovered()
        assertThat(navigator.currentDestination).isSameAs(sheet)
        assertWebViewAttached(sheet)

        navigator.pop()
        idle()

        assertThat(navigator.currentDestination).isSameAs(page)
        assertWebViewAttached(page)
    }

    @Test
    fun `removes a gone WebView that is still attached before destroying it`() {
        val page = navigator.currentDestination
        navigator.route("$BASE_URL/native-sheet")
        idle()

        // The page isn't notified, since it detached its WebView for the sheet
        val goneWebView = navigator.session.webView
        container(page).addView(goneWebView)
        renderProcessGone()
        navigator.pop()
        idle()

        assertThat(goneWebView.parent).isNull()
        assertThat(shadowOf(goneWebView).wasDestroyCalled()).isTrue()
        assertWebViewAttached(page)
    }

    @Test
    fun `recovers when navigating right after the render process is gone`() {
        renderProcessGone(idle = false)
        navigator.route("$BASE_URL/page")
        idle()

        assertRecovered()
        assertThat(navigator.location).isEqualTo("$BASE_URL/page")
        assertWebViewAttached(navigator.currentDestination)
    }

    @Test
    fun `recovers when going back right after the render process is gone`() {
        navigator.route("$BASE_URL/page")
        idle()

        renderProcessGone(idle = false)
        navigator.pop()
        idle()

        assertRecovered()
        assertThat(navigator.isAtStartDestination()).isTrue()
        assertWebViewAttached(navigator.currentDestination)
    }

    @Test
    fun `recovers when the activity is destroyed right after the render process is gone`() {
        renderProcessGone(idle = false)
        controller.pause().stop().destroy()
        idle()

        goneWebViews.forEach {
            assertThat(it.parent).isNull()
            assertThat(shadowOf(it).wasDestroyCalled()).isTrue()
        }
    }

    @Test
    fun `doesn't notify the recovered destination from the previous session`() {
        val destination = navigator.currentDestination
        renderProcessGone()
        val goneWebView = goneWebViews.first()

        @Suppress("DEPRECATION")
        val certificate = SslCertificate("example.com", "example.com", "", "")
        val error = SslError(SslError.SSL_UNTRUSTED, certificate, START_LOCATION)
        goneWebView.webViewClient.onReceivedSslError(goneWebView, mock<SslErrorHandler>(), error)
        idle()

        val errorContainer = requireNotNull(destination).fragment.requireView()
            .findViewById<ViewGroup>(R.id.hotwire_error_container)
        assertThat(errorContainer.childCount).isEqualTo(0)
    }

    private fun renderProcessGone(idle: Boolean = true) {
        @Suppress("DEPRECATION")
        val detail = object : RenderProcessGoneDetail() {
            override fun didCrash() = true
            override fun rendererPriorityAtExit() = 0
        }

        goneWebViews = listOf(navigator.session.webView, tabNavigator.session.webView)
        webViewsCreated = 0

        goneWebViews.forEach { it.webViewClient.onRenderProcessGone(it, detail) }
        if (idle) idle()
    }

    private fun assertRecovered() {
        // One new WebView per navigator, and each gone WebView is detached and destroyed
        assertThat(webViewsCreated).isEqualTo(2)
        assertThat(navigator.session.isRenderProcessGone).isFalse()
        assertThat(tabNavigator.session.isRenderProcessGone).isFalse()

        goneWebViews.forEach {
            assertThat(it.parent).isNull()
            assertThat(shadowOf(it).wasDestroyCalled()).isTrue()
            assertThat(TestWebFragment.detachedWebViews).contains(it)
        }
    }

    private fun assertWebViewAttached(destination: HotwireDestination?, navigator: Navigator = this.navigator) {
        val container = container(destination)
        val webView = navigator.session.webView

        assertThat(webView.parent).isSameAs(container)
        assertThat(container.children.filterIsInstance<HotwireWebView>().toList()).containsExactly(webView)
        assertThat(webView.webChromeClient).isInstanceOf(HotwireWebChromeClient::class.java)
        assertThat(navigator.session.currentVisit?.location).isEqualTo(requireNotNull(destination).location)

        // The container may have laid out the gone WebView before
        container.requestLayout()
        container.measure(
            MeasureSpec.makeMeasureSpec(1080, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(1920, MeasureSpec.EXACTLY)
        )
        container.layout(0, 0, 1080, 1920)
        assertThat(webView.measuredWidth).isEqualTo(1080)
        assertThat((webView as TestWebView).layoutWidth).isEqualTo(1080)
    }

    private fun container(destination: HotwireDestination?): ViewGroup {
        return requireNotNull(destination).fragment.requireView().findViewById(R.id.hotwire_webView_container)
    }

    private fun navigator(hostId: Int): Navigator {
        return requireNotNull(controller.get().delegate.findNavigatorHost(hostId)).navigator
    }

    // Fails instead of hanging if recovering keeps posting work.
    private fun idle() {
        val looper = shadowOf(Looper.getMainLooper())

        repeat(10_000) {
            if (looper.isIdle) return
            looper.runOneTask()
        }

        fail("The main looper never became idle")
    }

    // Robolectric's WebView doesn't keep its frame, so record the width it's laid out with
    class TestWebView(context: Context) : HotwireWebView(context) {
        var layoutWidth = 0

        override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
            super.onLayout(changed, left, top, right, bottom)
            layoutWidth = right - left
        }
    }

    @HotwireDestinationDeepLink(uri = "hotwire://fragment/web")
    class TestWebFragment : HotwireWebFragment() {
        override fun onWebViewDetached(webView: HotwireWebView) {
            super.onWebViewDetached(webView)
            assertThat(webView.parent).describedAs("parent when detached").isNull()
            detachedWebViews.add(webView)
        }

        companion object {
            val detachedWebViews = mutableListOf<HotwireWebView>()
        }
    }

    @HotwireDestinationDeepLink(uri = "hotwire://fragment/native/sheet")
    class NativeBottomSheetFragment : HotwireBottomSheetFragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
            return FrameLayout(requireContext())
        }
    }

    class TestActivity : HotwireActivity() {
        override fun navigatorConfigurations() = listOf(
            NavigatorConfiguration(
                name = "main",
                startLocation = START_LOCATION,
                navigatorHostId = HOST_ID
            ),
            NavigatorConfiguration(
                name = "tab",
                startLocation = "$BASE_URL/tab",
                navigatorHostId = TAB_HOST_ID
            )
        )

        override fun onCreate(savedInstanceState: Bundle?) {
            setTheme(com.google.android.material.R.style.Theme_Material3_Light_NoActionBar)
            super.onCreate(savedInstanceState)
            setContentView(LinearLayout(this).apply {
                addView(FrameLayout(context).apply { id = HOST_ID })
                addView(FrameLayout(context).apply { id = TAB_HOST_ID; visibility = View.GONE })
            })

            if (savedInstanceState == null) {
                supportFragmentManager.beginTransaction()
                    .add(HOST_ID, NavigatorHost())
                    .add(TAB_HOST_ID, NavigatorHost())
                    .commitNow()
            }
        }
    }

    companion object {
        private const val BASE_URL = "https://example.com"
        private const val START_LOCATION = "$BASE_URL/start"
        private val HOST_ID = View.generateViewId()
        private val TAB_HOST_ID = View.generateViewId()
    }
}
