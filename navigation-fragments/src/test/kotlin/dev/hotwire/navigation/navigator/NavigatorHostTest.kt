package dev.hotwire.navigation.navigator

import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.webkit.RenderProcessGoneDetail
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentContainerView
import dev.hotwire.core.bridge.BridgeComponent
import dev.hotwire.core.bridge.BridgeComponentFactory
import dev.hotwire.core.bridge.BridgeDelegate
import dev.hotwire.core.bridge.Message
import dev.hotwire.core.config.Hotwire
import dev.hotwire.navigation.activities.HotwireActivity
import dev.hotwire.navigation.config.HotwireNavigation
import dev.hotwire.navigation.config.registerBridgeComponents
import dev.hotwire.navigation.destinations.HotwireDestination
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.lang.ref.WeakReference

@RunWith(RobolectricTestRunner::class)
class NavigatorHostTest {

    private lateinit var activity: TestActivity
    private lateinit var host: NavigatorHost

    @Before
    fun setUp() {
        host = NavigatorHost()
    }

    @After
    fun tearDown() {
        HotwireNavigation.registeredBridgeComponentFactories = emptyList()
    }

    @Test
    fun `reverts to config start location when deep link host differs`() {
        val intent = Intent().apply {
            putExtra(DEEPLINK_EXTRAS_KEY, bundleOf(LOCATION_KEY to "https://other.com/path"))
        }
        activity = Robolectric.buildActivity(TestActivity::class.java, intent).get()

        host.activity = activity
        host.ensureDeeplinkStartLocationValid()

        assertThat(activity.intent.getBundleExtra(DEEPLINK_EXTRAS_KEY)?.getString(LOCATION_KEY))
            .isEqualTo("https://example.com/start")
    }

    @Test
    fun `does not change start location when deep link host matches config`() {
        val intent = Intent().apply {
            putExtra(DEEPLINK_EXTRAS_KEY, bundleOf(LOCATION_KEY to "https://example.com/path"))
        }
        activity = Robolectric.buildActivity(TestActivity::class.java, intent).get()

        host.activity = activity
        host.ensureDeeplinkStartLocationValid()

        assertThat(activity.intent.getBundleExtra(DEEPLINK_EXTRAS_KEY)?.getString(LOCATION_KEY))
            .isEqualTo("https://example.com/path")
    }

    // NavController merges deepLinkArgs over deepLinkExtras (last write wins); the intent's args
    // must not survive to override the validated start location.
    @Test
    fun `empties deepLinkArgs so they cannot override the start location`() {
        val intent = Intent().apply {
            putExtra(DEEPLINK_EXTRAS_KEY, bundleOf(LOCATION_KEY to "https://example.com/ok"))
            putParcelableArrayListExtra(DEEPLINK_ARGS_KEY, arrayListOf(bundleOf(LOCATION_KEY to ATTACKER_URL)))
        }
        activity = Robolectric.buildActivity(TestActivity::class.java, intent).get()

        host.activity = activity
        host.ensureDeeplinkStartLocationValid()

        val survivingArgs = activity.intent.getParcelableArrayListExtra<Bundle>(DEEPLINK_ARGS_KEY)
            ?.mapNotNull { it.getString(LOCATION_KEY) }.orEmpty()
        assertThat(survivingArgs).doesNotContain(ATTACKER_URL)
    }

    // Bridge instances are held statically and are only created when bridge components
    // are registered.
    @Test
    fun `recreating the activity releases the previous activity`() {
        Hotwire.registerBridgeComponents(BridgeComponentFactory("test", ::TestComponent))
        val controller = Robolectric.buildActivity(HostActivity::class.java).setup()
        shadowOf(Looper.getMainLooper()).idle()
        val oldActivity = WeakReference(controller.get())

        controller.recreate()
        shadowOf(Looper.getMainLooper()).idle()

        repeat(10) {
            if (oldActivity.get() != null) System.gc()
        }

        assertThat(oldActivity.get()).isNull()
    }

    // Every host instance creates its own Navigator, Session and WebView, including the host
    // that the FragmentManager restores when the Activity is recreated.
    @Test
    fun `recreating the activity destroys the previous host's WebView`() {
        val controller = Robolectric.buildActivity(HostActivity::class.java).setup()
        val oldWebView = controller.get().navigatorHost.navigator.session.webView

        controller.recreate()
        val newWebView = controller.get().navigatorHost.navigator.session.webView

        assertThat(newWebView).isNotSameAs(oldWebView)
        assertThat(shadowOf(oldWebView).wasDestroyCalled()).isTrue()
        assertThat(shadowOf(newWebView).wasDestroyCalled()).isFalse()
    }

    // A WebView whose render process is gone can't be used again. The current destination
    // routes to its location again, and the new destination view replaces the session.
    @Test
    fun `losing the render process replaces the session`() {
        val controller = Robolectric.buildActivity(HostActivity::class.java).setup()
        shadowOf(Looper.getMainLooper()).idle()
        val navigator = controller.get().navigatorHost.navigator
        val oldSession = navigator.session

        oldSession.webView.webViewClient.onRenderProcessGone(oldSession.webView, crashedRenderProcess)
        shadowOf(Looper.getMainLooper()).idle()
        val newSession = navigator.session

        assertThat(newSession).isNotSameAs(oldSession)
        assertThat(shadowOf(oldSession.webView).wasDestroyCalled()).isTrue()
        assertThat(shadowOf(newSession.webView).wasDestroyCalled()).isFalse()
    }

    companion object {
        private const val ATTACKER_URL = "https://attacker.example/steal"
        private val HOST_ID = View.generateViewId()

        private val crashedRenderProcess = object : RenderProcessGoneDetail() {
            override fun didCrash() = true
            override fun rendererPriorityAtExit() = 0
        }
    }

    private class TestActivity : HotwireActivity() {
        private val navConfig = NavigatorConfiguration(
            name = "test",
            startLocation = "https://example.com/start",
            navigatorHostId = 0
        )

        override fun navigatorConfigurations(): List<NavigatorConfiguration> = listOf(navConfig)
    }

    class TestComponent(name: String, delegate: BridgeDelegate<HotwireDestination>) :
        BridgeComponent<HotwireDestination>(name, delegate) {
        override fun onReceive(message: Message) {}
    }

    class HostActivity : HotwireActivity() {
        val navigatorHost get() = supportFragmentManager.findFragmentById(HOST_ID) as NavigatorHost

        override fun navigatorConfigurations() = listOf(
            NavigatorConfiguration(
                name = "test",
                startLocation = "https://example.com/start",
                navigatorHostId = HOST_ID
            )
        )

        override fun onCreate(savedInstanceState: Bundle?) {
            setTheme(androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar)
            super.onCreate(savedInstanceState)
            setContentView(FragmentContainerView(this).apply { id = HOST_ID })

            if (savedInstanceState == null) {
                supportFragmentManager.beginTransaction().add(HOST_ID, NavigatorHost()).commitNow()
            }
        }
    }
}