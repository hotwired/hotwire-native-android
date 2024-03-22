package dev.hotwire.core.navigation.routing

import dev.hotwire.core.turbo.activities.SessionConfiguration
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BrowserRouteTest {
    private val route = BrowserRoute()
    private val sessionConfig = SessionConfiguration(
        name = "test",
        startLocation = "https://my.app.com",
        navHostFragmentId = 0
    )

    @Test
    fun `matching result stops navigation`() {
        assertEquals(Router.RouteResult.STOP, route.result)
    }

    @Test
    fun `url on external domain matches`() {
        val url = "https://external.com/page"
        assertTrue(route.matches(url, sessionConfig))
    }

    @Test
    fun `url without subdomain matches`() {
        val url = "https://app.com/page"
        assertTrue(route.matches(url, sessionConfig))
    }

    @Test
    fun `url on app domain does not match`() {
        val url = "https://my.app.com/page"
        assertFalse(route.matches(url, sessionConfig))
    }
}
