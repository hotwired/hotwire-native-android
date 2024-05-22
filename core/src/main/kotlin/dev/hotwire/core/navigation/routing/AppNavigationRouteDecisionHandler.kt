package dev.hotwire.core.navigation.routing

import androidx.core.net.toUri
import dev.hotwire.core.navigation.activities.HotwireActivity
import dev.hotwire.core.navigation.navigator.NavigatorConfiguration

class AppNavigationRouteDecisionHandler : Router.RouteDecisionHandler {
    override val name = "app-navigation"

    override val decision = Router.Decision.NAVIGATE

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
    ) {
        // No-op
    }
}
