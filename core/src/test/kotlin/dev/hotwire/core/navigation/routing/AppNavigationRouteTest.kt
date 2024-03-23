package dev.hotwire.core.navigation.routing

import dev.hotwire.core.navigation.session.SessionConfiguration
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppNavigationRouteTest {
    private val route = AppNavigationRoute()
    private val sessionConfig = SessionConfiguration(
        name = "test",
        startLocation = "https://my.app.com",
        navHostFragmentId = 0
    )

    @Test
    fun `matching result navigates`() {
        assertEquals(Router.RouteResult.NAVIGATE, route.result)
    }

    @Test
    fun `url on app domain matches`() {
        val url = "https://my.app.com/page"
        assertTrue(route.matches(url, sessionConfig))
    }

    @Test
    fun `url without subdomain does not match`() {
        val url = "https://app.com/page"
        assertFalse(route.matches(url, sessionConfig))
    }

    @Test
    fun `masqueraded url does not match`() {
        val url = "https://app.my.com@fake.domain"
        assertFalse(route.matches(url, sessionConfig))
    }
}
