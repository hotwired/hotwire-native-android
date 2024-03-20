package dev.hotwire.core.navigation.routing

import dev.hotwire.core.config.Hotwire
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BrowserTabRouteTest {
    private val route = BrowserTabRoute()

    @Before
    fun setup() {
        Hotwire.appUrl = "https://my.app.com"
    }

    @Test
    fun `matching result stops navigation`() {
        assertEquals(Router.RouteResult.STOP, route.result)
    }

    @Test
    fun `url on external domain matches`() {
        val url = "https://external.com/page"
        assertTrue(route.matches(url))
    }

    @Test
    fun `url without subdomain matches`() {
        val url = "https://app.com/page"
        assertTrue(route.matches(url))
    }

    @Test
    fun `url on app domain does not match`() {
        val url = "https://my.app.com/page"
        assertFalse(route.matches(url))
    }
}
