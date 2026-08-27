package dev.hotwire.navigation.routing

import dev.hotwire.core.config.Hotwire
import dev.hotwire.core.turbo.visit.VisitProposal
import dev.hotwire.navigation.activities.HotwireActivity
import dev.hotwire.navigation.navigator.NavigatorConfiguration

/**
 * Navigates internal urls through in-app routing.
 */
class AppNavigationRouteDecisionHandler : Router.RouteDecisionHandler {
    override val name = "app-navigation"

    override fun matches(
        proposal: VisitProposal,
        configuration: NavigatorConfiguration
    ): Boolean {
        return Hotwire.config.hostVerifier.isTrustedForNavigation(proposal.location)
    }

    override fun handle(
        proposal: VisitProposal,
        configuration: NavigatorConfiguration,
        activity: HotwireActivity
    ): Router.Decision {
        return Router.Decision.NAVIGATE
    }
}
