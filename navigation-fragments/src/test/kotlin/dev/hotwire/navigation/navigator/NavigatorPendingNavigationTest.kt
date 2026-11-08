package dev.hotwire.navigation.navigator

import android.os.Bundle
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import dev.hotwire.core.bridge.Bridge
import dev.hotwire.core.config.Hotwire
import dev.hotwire.core.turbo.config.PathConfiguration
import dev.hotwire.core.turbo.webview.HotwireWebView
import dev.hotwire.navigation.activities.HotwireActivity
import dev.hotwire.navigation.config.defaultFragmentDestination
import dev.hotwire.navigation.config.registerFragmentDestinations
import dev.hotwire.navigation.destinations.HotwireDestinationDeepLink
import dev.hotwire.navigation.fragments.HotwireWebBottomSheetFragment
import dev.hotwire.navigation.fragments.HotwireWebFragment
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class NavigatorPendingNavigationTest {
    private lateinit var activityController: ActivityController<HostActivity>
    private val activity get() = activityController.get()
    private val navigator get() = activity.host.navigator
    private val page get() = navigator.currentDestination as PageFragment

    @Before
    fun setUp() {
        Hotwire.defaultFragmentDestination = PageFragment::class
        Hotwire.registerFragmentDestinations(PageFragment::class)
        Hotwire.loadPathConfiguration(
            ApplicationProvider.getApplicationContext(),
            PathConfiguration.Location(assetFilePath = "json/empty-configuration.json")
        )
        activityController = Robolectric.buildActivity(HostActivity::class.java).setup().visible()
        idle()
    }

    @After
    fun tearDown() {
        if (!activity.isDestroyed) activityController.pause().stop().destroy()
        Hotwire.defaultFragmentDestination = HotwireWebFragment::class
        Hotwire.registerFragmentDestinations(HotwireWebFragment::class, HotwireWebBottomSheetFragment::class)
    }

    @Test
    fun `pending reset is discarded when its host is removed`() {
        val removedPage = page
        var resets = 0
        navigator.reset { resets++ }

        removeHost()
        activity.configurations = emptyList()
        idle()

        assertThat(resets).isZero()
        assertThat(removedPage.detachments).isEqualTo(1)
    }

    @Test
    fun `pending reset is discarded when the activity is destroyed`() {
        val removedPage = page
        var resets = 0
        navigator.reset { resets++ }

        activityController.pause().stop().destroy()
        idle()

        assertThat(resets).isZero()
        assertThat(removedPage.detachments).isEqualTo(1)
    }

    @Test
    fun `pending reset belongs to the original fragment view`() {
        val originalPage = page
        val originalView = originalPage.requireView()
        var resets = 0
        navigator.reset { resets++ }

        activity.host.childFragmentManager.beginTransaction().detach(originalPage).commitNow()
        activity.host.childFragmentManager.beginTransaction().attach(originalPage).commitNow()
        idle()

        assertThat(originalPage.requireView()).isNotSameAs(originalView)
        assertThat(resets).isZero()
    }

    @Test
    fun `reset requested after host removal is ignored`() {
        val removedNavigator = navigator
        removeHost()
        var resets = 0

        removedNavigator.reset { resets++ }
        idle()

        assertThat(resets).isZero()
    }

    @Test
    fun `detachment callback may remove the host before navigation resumes`() {
        val removedPage = page
        removedPage.onDetached = { removeHost() }
        var resets = 0

        navigator.reset { resets++ }
        idle()

        assertThat(removedPage.detachments).isEqualTo(1)
        assertThat(resets).isZero()
    }

    @Test
    fun `obsolete navigation still runs detachment cleanup with an initialized bridge`() {
        val removedPage = page
        Bridge.initialize(navigator.session.webView)
        removedPage.onWebViewAttached(navigator.session.webView)
        var resets = 0
        navigator.reset { resets++ }

        removeHost()
        idle()

        assertThat(removedPage.detachments).isEqualTo(1)
        assertThat(resets).isZero()
    }

    @Test
    fun `live host completes a pending reset once`() {
        val previousPage = page
        var resets = 0

        navigator.reset { resets++ }
        idle()

        assertThat(resets).isEqualTo(1)
        assertThat(page).isNotSameAs(previousPage)
    }

    @Test
    fun `live view detached from its window can still finish navigation`() {
        val detachedPage = page
        val root = detachedPage.requireView()
        (root.parent as ViewGroup).removeView(root)
        assertThat(detachedPage.hotwireView?.windowToken).isNull()
        var resets = 0

        navigator.reset { resets++ }
        idle()

        assertThat(detachedPage.detachments).isEqualTo(1)
        assertThat(resets).isEqualTo(1)
    }

    @Test
    fun `live host still routes to a new destination`() {
        navigator.route("$START_LOCATION/next")
        idle()

        assertThat(navigator.location).isEqualTo("$START_LOCATION/next")
    }

    private fun removeHost() {
        activity.supportFragmentManager.beginTransaction().remove(activity.host).commitNow()
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    class HostActivity : HotwireActivity() {
        val host = NavigatorHost()
        var configurations = listOf(NavigatorConfiguration("main", START_LOCATION, CONTAINER_ID))

        override fun navigatorConfigurations() = configurations

        override fun onCreate(savedInstanceState: Bundle?) {
            setTheme(com.google.android.material.R.style.Theme_Material3_Light_NoActionBar)
            super.onCreate(savedInstanceState)
            setContentView(FrameLayout(this).apply { id = CONTAINER_ID })
            supportFragmentManager.beginTransaction().add(CONTAINER_ID, host).commitNow()
            delegate.setCurrentNavigator(configurations.single())
        }
    }

    @HotwireDestinationDeepLink(uri = "hotwire://fragment/web")
    class PageFragment : HotwireWebFragment() {
        var detachments = 0
        var onDetached: () -> Unit = {}

        override fun onWebViewDetached(webView: HotwireWebView) {
            super.onWebViewDetached(webView)
            detachments++
            onDetached()
        }
    }

    companion object {
        private const val START_LOCATION = "https://example.com/start"
        private val CONTAINER_ID = View.generateViewId()
    }
}
