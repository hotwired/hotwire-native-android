package dev.hotwire.navigation.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import dev.hotwire.navigation.navigator.NavigatorConfiguration
import dev.hotwire.navigation.navigator.NavigatorHost
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

// A screen that hosts a nested NavigatorHost can be replaced by a new instance of itself. The new
// nested host is created before the old one is destroyed, and both use the same view ID.
@RunWith(RobolectricTestRunner::class)
class HotwireActivityDelegateTest {

    private lateinit var activity: TestActivity

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(TestActivity::class.java).setup().get()
    }

    @Test
    fun `new nested host loads its graph while the old host with the same id is still registered`() {
        addScreen(CONTAINER_A_ID)

        val newScreen = addScreen(CONTAINER_B_ID)

        assertThat(newScreen.navigatorHost.isGraphInitialized).isTrue()
    }

    @Test
    fun `destroying the old nested host keeps the new host with the same id registered`() {
        val oldScreen = addScreen(CONTAINER_A_ID)
        val newScreen = addScreen(CONTAINER_B_ID)

        activity.supportFragmentManager.beginTransaction().remove(oldScreen).commitNow()

        assertThat(activity.delegate.findNavigatorHost(NESTED_HOST_ID))
            .isSameAs(newScreen.navigatorHost)
    }

    @Test
    fun `destroying the old nested host only destroys its own WebView`() {
        val oldScreen = addScreen(CONTAINER_A_ID)
        val newScreen = addScreen(CONTAINER_B_ID)
        val oldWebView = oldScreen.navigatorHost.navigator.session.webView
        val newWebView = newScreen.navigatorHost.navigator.session.webView

        activity.supportFragmentManager.beginTransaction().remove(oldScreen).commitNow()

        assertThat(shadowOf(oldWebView).wasDestroyCalled()).isTrue()
        assertThat(shadowOf(newWebView).wasDestroyCalled()).isFalse()
    }

    private fun addScreen(containerId: Int): NestedHostScreen {
        val screen = NestedHostScreen()
        activity.supportFragmentManager.beginTransaction().add(containerId, screen).commitNow()
        return screen
    }

    class NestedHostScreen : Fragment() {
        val navigatorHost get() = childFragmentManager.findFragmentById(NESTED_HOST_ID) as NavigatorHost

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
            return FrameLayout(requireContext()).apply { id = NESTED_HOST_ID }
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            childFragmentManager.beginTransaction().add(NESTED_HOST_ID, NavigatorHost()).commitNow()
        }
    }

    class TestActivity : HotwireActivity() {
        override fun navigatorConfigurations() = listOf(
            NavigatorConfiguration(
                name = "nested",
                startLocation = "https://example.com/start",
                navigatorHostId = NESTED_HOST_ID
            )
        )

        override fun onCreate(savedInstanceState: Bundle?) {
            setTheme(androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar)
            super.onCreate(savedInstanceState)
            setContentView(LinearLayout(this).apply {
                addView(FrameLayout(context).apply { id = CONTAINER_A_ID })
                addView(FrameLayout(context).apply { id = CONTAINER_B_ID })
            })
        }
    }

    companion object {
        private val NESTED_HOST_ID = View.generateViewId()
        private val CONTAINER_A_ID = View.generateViewId()
        private val CONTAINER_B_ID = View.generateViewId()
    }
}
