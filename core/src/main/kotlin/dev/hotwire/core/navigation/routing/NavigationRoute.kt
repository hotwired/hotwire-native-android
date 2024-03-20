package dev.hotwire.core.navigation.routing

import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri

class NavigationRoute : Router.Route {
    override val name = "navigation"

    override val result = Router.RouteResult.NAVIGATE

    override fun matches(location: String): Boolean {
        return appUrl.toUri().host == location.toUri().host
    }

    override fun perform(location: String, activity: AppCompatActivity) {
        // No-op
    }
}
