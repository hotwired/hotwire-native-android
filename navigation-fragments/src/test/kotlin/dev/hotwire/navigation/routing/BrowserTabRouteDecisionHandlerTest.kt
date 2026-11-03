package dev.hotwire.navigation.routing

import dev.hotwire.core.config.Hotwire
import dev.hotwire.core.turbo.config.PathConfigurationProperties
import dev.hotwire.core.turbo.visit.VisitOptions
import dev.hotwire.core.turbo.visit.VisitProposal
import dev.hotwire.navigation.activities.HotwireActivity
import dev.hotwire.navigation.navigator.NavigatorConfiguration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric.buildActivity
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BrowserTabRouteDecisionHandlerTest {
    private lateinit var activity: HotwireActivity

    private val route = BrowserTabRouteDecisionHandler()
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
    fun `matching result stops navigation`() {
        val decision = route.handle(proposal("https://external.com/page"), config, activity)
        assertEquals(Router.Decision.CANCEL, decision)
    }

    @Test
    fun `url on external domain matches`() {
        val url = "https://external.com/page"
        assertTrue(route.matches(proposal(url), config))
    }

    @Test
    fun `url without subdomain matches`() {
        val url = "https://app.com/page"
        assertTrue(route.matches(proposal(url), config))
    }

    @Test
    fun `url on app domain does not match`() {
        val url = "https://my.app.com/page"
        assertFalse(route.matches(proposal(url), config))
    }

    @Test
    fun `http url or another port on the app host matches`() {
        assertTrue(route.matches(proposal("http://my.app.com/page"), config))
        assertTrue(route.matches(proposal("https://my.app.com:8443/page"), config))
    }

    @Test
    fun `non-http scheme does not match`() {
        val url = "sms:555-555-5555"
        assertFalse(route.matches(proposal(url), config))
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
