package dev.hotwire.navigation.routing

import android.content.Intent
import androidx.core.net.toUri
import dev.hotwire.core.config.Hotwire
import dev.hotwire.core.turbo.config.PathConfigurationProperties
import dev.hotwire.core.turbo.visit.VisitOptions
import dev.hotwire.core.turbo.visit.VisitProposal
import dev.hotwire.navigation.activities.HotwireActivity
import dev.hotwire.navigation.navigator.NavigatorConfiguration
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric.buildActivity
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * The default handlers must agree on what is internal, so every location
 * either navigates in-app or leaves the app. None may fall through.
 */
@RunWith(RobolectricTestRunner::class)
class RouterTest {
    private lateinit var activity: HotwireActivity

    private val router = Router(listOf(
        AppNavigationRouteDecisionHandler(),
        BrowserTabRouteDecisionHandler(),
        SystemNavigationRouteDecisionHandler()
    ))
    private val config = NavigatorConfiguration(
        name = "test",
        startLocation = "https://my.app.com",
        navigatorHostId = 0
    )

    @Before
    fun setup() {
        activity = buildActivity(TestActivity::class.java).get()
        Hotwire.config.registerStartLocation(config.startLocation)
    }

    @After
    fun teardown() {
        Hotwire.config.unregisterStartLocation(config.startLocation)
    }

    @Test
    fun `app origin navigates in-app`() {
        val decision = router.decideRoute(proposal("https://my.app.com/page"), config, activity)

        assertEquals(Router.Decision.NAVIGATE, decision)
        assertNull(shadowOf(activity).nextStartedActivity)
    }

    @Test
    fun `http url on the app host opens in the browser`() {
        assertLeavesTheApp("http://my.app.com/page")
    }

    @Test
    fun `url on another port of the app host opens in the browser`() {
        assertLeavesTheApp("https://my.app.com:8443/page")
    }

    @Test
    fun `url on another host opens in the browser`() {
        assertLeavesTheApp("https://external.com/page")
    }

    @Test
    fun `non-http url opens in the system`() {
        assertLeavesTheApp("sms:555-555-5555")
    }

    private fun assertLeavesTheApp(location: String) {
        val decision = router.decideRoute(proposal(location), config, activity)
        val started: Intent? = shadowOf(activity).nextStartedActivity

        assertEquals(Router.Decision.CANCEL, decision)
        assertEquals(location.toUri(), started?.data)
    }

    private fun proposal(location: String) = VisitProposal(
        location = location,
        options = VisitOptions(),
        properties = PathConfigurationProperties(),
        bundle = null
    )

    private class TestActivity : HotwireActivity() {
        override fun navigatorConfigurations() = emptyList<NavigatorConfiguration>()
    }
}
