package dev.hotwire.core.navigation.routing

import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri

class NavigationRoute : Router.Route {
    override val name = "navigation"

    override fun matches(location: String): Boolean {
        return appUrl.toUri().host == location.toUri().host
    }

    override fun perform(location: String, activity: AppCompatActivity): Router.RouteResult {
        return Router.RouteResult.NAVIGATE
    }
}
