package dev.hotwire.navigation.routing

import androidx.core.net.toUri
import dev.hotwire.navigation.activities.HotwireActivity
import dev.hotwire.navigation.navigator.NavigatorConfiguration

/**
 * Navigates internal urls through in-app routing.
 */
class AppNavigationRouteDecisionHandler : Router.RouteDecisionHandler {
    override val name = "app-navigation"

    override fun matches(
        location: String,
        configuration: NavigatorConfiguration
    ): Boolean {
        return configuration.startLocation.toUri().host == location.toUri().host
    }

    override fun handle(
        location: String,
        configuration: NavigatorConfiguration,
        activity: HotwireActivity
    ): Router.Decision {
        return Router.Decision.NAVIGATE
    }
}
