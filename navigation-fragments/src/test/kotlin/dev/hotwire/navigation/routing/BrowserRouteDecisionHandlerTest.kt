package dev.hotwire.navigation.routing

import dev.hotwire.navigation.activities.HotwireActivity
import dev.hotwire.navigation.navigator.NavigatorConfiguration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric.buildActivity
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BrowserRouteDecisionHandlerTest {
    private lateinit var activity: HotwireActivity

    private val route = BrowserRouteDecisionHandler()
    private val config = NavigatorConfiguration(
        name = "test",
        startLocation = "https://my.app.com",
        navigatorHostId = 0
    )

    @Before
    fun setup() {
        activity = buildActivity(TestActivity::class.java).get()
    }

    @Test
    fun `matching result stops navigation`() {
        val decision = route.handle("https://external.com/page", config, activity)
        assertEquals(Router.Decision.CANCEL, decision)
    }

    @Test
    fun `url on external domain matches`() {
        val url = "https://external.com/page"
        assertTrue(route.matches(url, config))
    }

    @Test
    fun `url without subdomain matches`() {
        val url = "https://app.com/page"
        assertTrue(route.matches(url, config))
    }

    @Test
    fun `url on app domain does not match`() {
        val url = "https://my.app.com/page"
        assertFalse(route.matches(url, config))
    }

    private class TestActivity : HotwireActivity() {
        override fun navigatorConfigurations() = emptyList<NavigatorConfiguration>()
    }
}
