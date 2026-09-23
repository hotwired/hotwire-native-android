package dev.hotwire.navigation.routing

import dev.hotwire.core.config.Hotwire
import dev.hotwire.core.security.DefaultOriginTrustPolicy
import dev.hotwire.core.security.Origin
import dev.hotwire.core.security.OriginTrustPolicy
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
        Hotwire.config.registerStartLocation(config.startLocation)
    }

    @After
    fun teardown() {
        Hotwire.config.unregisterStartLocation(config.startLocation)
        Hotwire.config.originTrustPolicy = DefaultOriginTrustPolicy
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
    fun `url on another navigator's start origin matches`() {
        val otherStartLocation = "https://other.app.com/start"
        Hotwire.config.registerStartLocation(otherStartLocation)

        try {
            assertTrue(route.matches(proposal("https://other.app.com/page"), config))
        } finally {
            Hotwire.config.unregisterStartLocation(otherStartLocation)
        }
    }

    @Test
    fun `a custom origin trust policy decides the match`() {
        Hotwire.config.originTrustPolicy = object : OriginTrustPolicy() {
            override fun isTrustedForNavigation(origin: Origin) = origin.host == "asset.cdn.com"
            override fun isTrustedForNativeAccess(origin: Origin) = false
        }

        assertTrue(route.matches(proposal("https://asset.cdn.com/image.png"), config))
        assertFalse(route.matches(proposal(config.startLocation), config))
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
