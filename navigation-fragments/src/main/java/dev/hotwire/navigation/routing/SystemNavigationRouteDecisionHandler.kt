package dev.hotwire.navigation.routing

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.core.net.toUri
import dev.hotwire.core.turbo.visit.VisitProposal
import dev.hotwire.navigation.activities.HotwireActivity
import dev.hotwire.navigation.logging.logError
import dev.hotwire.navigation.navigator.NavigatorConfiguration

/**
 * Opens external urls via a new Activity intent. Non-HTTP/S schemes are supported.
 */
class SystemNavigationRouteDecisionHandler : Router.RouteDecisionHandler {
    override val name = "system-navigation"

    override fun matches(
        proposal: VisitProposal,
        configuration: NavigatorConfiguration
    ): Boolean {
        return configuration.startLocation.toUri().host != proposal.location.toUri().host
    }

    override fun handle(
        proposal: VisitProposal,
        configuration: NavigatorConfiguration,
        activity: HotwireActivity
    ): Router.Decision {
        val intent = Intent(Intent.ACTION_VIEW, proposal.location.toUri())

        try {
            activity.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            logError("SystemNavigationRouteDecisionHandler", e)
        } catch (e: SecurityException) {
            logError("SystemNavigationRouteDecisionHandler", e)
        }

        return Router.Decision.CANCEL
    }
}
