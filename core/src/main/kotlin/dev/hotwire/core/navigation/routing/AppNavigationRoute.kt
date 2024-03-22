package dev.hotwire.core.navigation.routing

import androidx.core.net.toUri
import dev.hotwire.core.navigation.activities.HotwireActivity
import dev.hotwire.core.navigation.session.SessionConfiguration

class AppNavigationRoute : Router.Route {
    override val name = "app-navigation"

    override val result = Router.RouteResult.NAVIGATE

    override fun matches(
        location: String,
        sessionConfiguration: SessionConfiguration
    ): Boolean {
        return sessionConfiguration.startLocation.toUri().host == location.toUri().host
    }

    override fun handle(
        location: String,
        sessionConfiguration: SessionConfiguration,
        activity: HotwireActivity
    ) {
        // No-op
    }
}
