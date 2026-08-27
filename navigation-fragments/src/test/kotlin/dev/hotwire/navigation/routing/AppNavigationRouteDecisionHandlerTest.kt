package dev.hotwire.navigation.routing

import androidx.core.net.toUri
import dev.hotwire.core.config.Hotwire
import dev.hotwire.core.security.HostVerifier
import dev.hotwire.core.turbo.config.PathConfigurationProperties
import dev.hotwire.core.turbo.visit.VisitOptions
import dev.hotwire.core.turbo.visit.VisitProposal
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
class AppNavigationRouteDecisionHandlerTest {
    private lateinit var activity: HotwireActivity

    private val route = AppNavigationRouteDecisionHandler()
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
    fun `matching result navigates`() {
        val decision = route.handle(proposal(config.startLocation), config, activity)
        assertEquals(Router.Decision.NAVIGATE, decision)
    }

    @Test
    fun `url on app domain matches`() {
        val url = "https://my.app.com/page"
        assertTrue(route.matches(proposal(url), config))
    }

    @Test
    fun `url without subdomain does not match`() {
        val url = "https://app.com/page"
        assertFalse(route.matches(proposal(url), config))
    }

    @Test
    fun `masqueraded url does not match`() {
        val url = "https://app.my.com@fake.domain"
        assertFalse(route.matches(proposal(url), config))
    }

    @Test
    fun `http url on the app domain does not match`() {
        val url = "http://my.app.com/page"
        assertFalse(route.matches(proposal(url), config))
    }

    @Test
    fun `url on another port does not match`() {
        val url = "https://my.app.com:8443/page"
        assertFalse(route.matches(proposal(url), config))
    }

    @Test
    fun `a custom host verifier decides the match`() {
        val previousVerifier = Hotwire.config.hostVerifier

        Hotwire.config.hostVerifier = object : HostVerifier {
            override fun isTrustedForNavigation(location: String, startLocation: String) =
                location.toUri().host == "asset.cdn.com"

            override fun isTrustedForBridge(location: String, startLocation: String) = false
        }

        try {
            assertTrue(route.matches(proposal("https://asset.cdn.com/image.png"), config))
            assertFalse(route.matches(proposal(config.startLocation), config))
        } finally {
            Hotwire.config.hostVerifier = previousVerifier
        }
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
