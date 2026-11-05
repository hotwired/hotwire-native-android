package dev.hotwire.navigation.navigator

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.webkit.RenderProcessGoneDetail
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentFactory
import androidx.navigation.NavHostController
import androidx.navigation.fragment.DialogFragmentNavigator
import dev.hotwire.core.bridge.BridgeComponent
import dev.hotwire.core.bridge.BridgeComponentFactory
import dev.hotwire.core.bridge.BridgeDelegate
import dev.hotwire.core.bridge.Message
import dev.hotwire.core.config.Hotwire
import dev.hotwire.navigation.R
import dev.hotwire.navigation.activities.HotwireActivity
import dev.hotwire.navigation.config.HotwireNavigation
import dev.hotwire.navigation.config.registerBridgeComponents
import dev.hotwire.navigation.destinations.HotwireDestination
import dev.hotwire.navigation.fragments.HotwireWebFragment
import dev.hotwire.navigation.views.HotwireSwipeRefreshLayout
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.lang.ref.WeakReference

@RunWith(RobolectricTestRunner::class)
class NavigatorHostTest {

    @After
    fun tearDown() {
        HotwireNavigation.registeredBridgeComponentFactories = emptyList()
    }

    @Test
    fun `doesn't navigate to a destination matching the launching Intent's data`() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SHEET_DEEP_LINK))

        val activity = launch(intent)

        assertThat(activity.navigatorHost.destinations()).containsExactly(START_LOCATION)
    }

    @Test
    fun `doesn't navigate to the destination and location in the launching Intent's extras`() {
        val (graphId, sheetId) = launch(Intent()).navigatorHost.navController.graph.let { graph ->
            graph.id to graph.single { it is DialogFragmentNavigator.Destination }.id
        }
        val intent = Intent().apply {
            putExtra(DEEPLINK_IDS_KEY, intArrayOf(graphId, sheetId))
            putExtra(DEEPLINK_EXTRAS_KEY, bundleOf("location" to OTHER_LOCATION))
            putParcelableArrayListExtra(DEEPLINK_ARGS_KEY, arrayListOf(Bundle(), bundleOf("location" to OTHER_LOCATION)))
        }

        val activity = launch(intent)

        assertThat(activity.navigatorHost.destinations()).containsExactly(START_LOCATION)
    }

    @Test
    fun `leaves the launching Intent unchanged`() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SHEET_DEEP_LINK)).apply {
            putExtra(DEEPLINK_EXTRAS_KEY, bundleOf("location" to OTHER_LOCATION))
            putExtra("app_extra", "kept")
        }

        val activity = launch(intent)

        assertThat(activity.intent.data).isEqualTo(Uri.parse(SHEET_DEEP_LINK))
        assertThat(activity.intent.getBundleExtra(DEEPLINK_EXTRAS_KEY)?.getString("location")).isEqualTo(OTHER_LOCATION)
        assertThat(activity.intent.getStringExtra("app_extra")).isEqualTo("kept")
    }

    @Test
    fun `app code sees the launching Intent without its deep link while the graph is set`() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SHEET_DEEP_LINK)).apply {
            putExtra(DEEPLINK_EXTRAS_KEY, bundleOf("location" to OTHER_LOCATION))
            putExtra("app_extra", "kept")
        }
        val seen = mutableListOf<Intent>()
        FactoryHostActivity.onInstantiate = { activity -> seen += Intent(activity.intent) }

        Robolectric.buildActivity(FactoryHostActivity::class.java, intent).setup()
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(seen).isNotEmpty
        assertThat(seen.first().action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(seen.first().getStringExtra("app_extra")).isEqualTo("kept")
        assertThat(seen.first().data).isNull()
        assertThat(seen.first().hasExtra(DEEPLINK_EXTRAS_KEY)).isFalse()
    }

    // NavHostFragment.onCreate() sets a graph supplied through its arguments or XML, and creates
    // its NavController just before. That graph can match a deep link by action alone.
    @Test
    fun `hides the launching Intent's deep link from NavHostFragment's own setup`() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SHEET_DEEP_LINK))
        RecordingHostActivity.seen.clear()

        val activity = Robolectric.buildActivity(RecordingHostActivity::class.java, intent).setup().get()

        assertThat(RecordingHostActivity.seen).hasSize(1)
        assertThat(RecordingHostActivity.seen.first().data).isNull()
        assertThat(RecordingHostActivity.seen.first().action).isNull()
        assertThat(activity.intent.data).isEqualTo(Uri.parse(SHEET_DEEP_LINK))
    }

    // A Bridge is only created when bridge components are registered.
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

    // prepareNavigation() is what showing a native dialog does to the destination below it:
    // it detaches the WebView and stops listening to the session, but keeps the view.
    @Test
    fun `canceling a dialog after losing the render process recovers the destination in place`() {
        val controller = Robolectric.buildActivity(HostActivity::class.java).setup()
        shadowOf(Looper.getMainLooper()).idle()
        val host = controller.get().navigatorHost
        val destination = host.childFragmentManager.primaryNavigationFragment as HotwireWebFragment
        val oldSession = host.navigator.session

        destination.prepareNavigation {}
        oldSession.webView.webViewClient.onRenderProcessGone(oldSession.webView, crashedRenderProcess)
        shadowOf(Looper.getMainLooper()).idle()
        assertThat(host.navigator.session).isSameAs(oldSession)

        destination.onStartAfterDialogCancel()
        shadowOf(Looper.getMainLooper()).idle()
        val newSession = host.navigator.session

        assertThat(host.childFragmentManager.primaryNavigationFragment).isSameAs(destination)
        assertThat(newSession).isNotSameAs(oldSession)
        val refreshLayout = destination.requireView().findViewById<HotwireSwipeRefreshLayout>(R.id.hotwire_webView_container)
        assertThat(newSession.webView.parent).isSameAs(refreshLayout.webViewContainer)
        assertThat(shadowOf(oldSession.webView).wasDestroyCalled()).isTrue()
        assertThat(shadowOf(newSession.webView).wasDestroyCalled()).isFalse()
    }

    private fun launch(intent: Intent): HostActivity {
        val activity = Robolectric.buildActivity(HostActivity::class.java, intent).setup().get()
        shadowOf(Looper.getMainLooper()).idle()
        return activity
    }

    private fun NavigatorHost.destinations() = childFragmentManager.fragments
        .filterIsInstance<HotwireDestination>()
        .map { it.location }

    companion object {
        private const val START_LOCATION = "https://example.com/start"
        private const val OTHER_LOCATION = "https://example.com/other"
        private const val SHEET_DEEP_LINK = "hotwire://fragment/web/modal/sheet"
        private const val DEEPLINK_IDS_KEY = "android-support-nav:controller:deepLinkIds"
        private const val DEEPLINK_EXTRAS_KEY = "android-support-nav:controller:deepLinkExtras"
        private const val DEEPLINK_ARGS_KEY = "android-support-nav:controller:deepLinkArgs"
        private val HOST_ID = View.generateViewId()

        private val crashedRenderProcess = object : RenderProcessGoneDetail() {
            override fun didCrash() = true
            override fun rendererPriorityAtExit() = 0
        }
    }

    class RecordingNavigatorHost : NavigatorHost() {
        override fun onCreateNavHostController(navHostController: NavHostController) {
            super.onCreateNavHostController(navHostController)
            RecordingHostActivity.seen += Intent(requireActivity().intent)
        }
    }

    class RecordingHostActivity : HostActivity() {
        override fun createNavigatorHost() = RecordingNavigatorHost()

        companion object {
            val seen = mutableListOf<Intent>()
        }
    }

    class FactoryHostActivity : HostActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            val activity = this
            supportFragmentManager.fragmentFactory = object : FragmentFactory() {
                override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
                    if (className == HotwireWebFragment::class.java.name) onInstantiate(activity)
                    return super.instantiate(classLoader, className)
                }
            }
            super.onCreate(savedInstanceState)
        }

        companion object {
            var onInstantiate: (Activity) -> Unit = {}
        }
    }

    class TestComponent(name: String, delegate: BridgeDelegate<HotwireDestination>) :
        BridgeComponent<HotwireDestination>(name, delegate) {
        override fun onReceive(message: Message) {}
    }

    open class HostActivity : HotwireActivity() {
        val navigatorHost get() = supportFragmentManager.findFragmentById(HOST_ID) as NavigatorHost

        override fun navigatorConfigurations() = listOf(
            NavigatorConfiguration(
                name = "test",
                startLocation = "https://example.com/start",
                navigatorHostId = HOST_ID
            )
        )

        open fun createNavigatorHost() = NavigatorHost()

        override fun onCreate(savedInstanceState: Bundle?) {
            setTheme(androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar)
            super.onCreate(savedInstanceState)
            setContentView(FragmentContainerView(this).apply { id = HOST_ID })

            if (savedInstanceState == null) {
                supportFragmentManager.beginTransaction().add(HOST_ID, createNavigatorHost()).commitNow()
            }
        }
    }
}