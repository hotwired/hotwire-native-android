package dev.hotwire.core.navigation.routing

import dev.hotwire.core.navigation.navigator.NavigatorConfiguration
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BrowserTabRouteDecisionHandlerTest {
    private val route = BrowserTabRouteDecisionHandler()
    private val config = NavigatorConfiguration(
        name = "test",
        startLocation = "https://my.app.com",
        navigatorHostId = 0
    )

    @Test
    fun `matching result stops navigation`() {
        assertEquals(Router.Decision.CANCEL, route.decision)
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
}
