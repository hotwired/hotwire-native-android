package dev.hotwire.navigation.routing

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.core.net.toUri
import dev.hotwire.navigation.activities.HotwireActivity
import dev.hotwire.navigation.logging.logError
import dev.hotwire.navigation.navigator.NavigatorConfiguration

class BrowserRouteDecisionHandler : Router.RouteDecisionHandler {
    override val name = "browser"

    override val decision = Router.Decision.CANCEL

    override fun matches(
        location: String,
        configuration: NavigatorConfiguration
    ): Boolean {
        return configuration.startLocation.toUri().host != location.toUri().host
    }

    override fun handle(
        location: String,
        configuration: NavigatorConfiguration,
        activity: HotwireActivity
    ) {
        val intent = Intent(Intent.ACTION_VIEW, location.toUri())

        try {
            activity.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            logError("BrowserRouteDecisionHandler", e)
        }
    }
}
