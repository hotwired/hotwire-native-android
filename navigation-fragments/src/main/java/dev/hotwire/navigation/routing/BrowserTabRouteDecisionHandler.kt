package dev.hotwire.navigation.routing

import android.content.ActivityNotFoundException
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.google.android.material.R
import dev.hotwire.core.security.isTrustedForNavigation
import dev.hotwire.core.turbo.visit.VisitProposal
import dev.hotwire.navigation.activities.HotwireActivity
import dev.hotwire.navigation.logging.logError
import dev.hotwire.navigation.navigator.NavigatorConfiguration
import dev.hotwire.navigation.util.colorFromThemeAttr

/**
 * Opens HTTP/S urls that [dev.hotwire.core.security.OriginTrustPolicy.isTrustedForNavigation]
 * does not trust via a browser Custom Tab so the user stays in-app. If the
 * device default browser does not support Custom Tabs, the url will open
 * directly in the default browser.
 *
 * https://developer.chrome.com/docs/android/custom-tabs
 */
class BrowserTabRouteDecisionHandler : Router.RouteDecisionHandler {
    override val name = "browser-tab"

    override fun matches(
        proposal: VisitProposal,
        configuration: NavigatorConfiguration
    ): Boolean {
        val scheme = proposal.location.toUri().scheme?.lowercase()

        return !isTrustedForNavigation(proposal.location) && (scheme == "https" || scheme == "http")
    }

    override fun handle(
        proposal: VisitProposal,
        configuration: NavigatorConfiguration,
        activity: HotwireActivity
    ): Router.Decision {
        val color = activity.colorFromThemeAttr(R.attr.colorSurface)
        val colorParams = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(color)
            .setNavigationBarColor(color)
            .build()

        try {
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setShareState(CustomTabsIntent.SHARE_STATE_ON)
                .setUrlBarHidingEnabled(false)
                .setDefaultColorSchemeParams(colorParams)
                .build()
                .launchUrl(activity, proposal.location.toUri())
        } catch (e: ActivityNotFoundException) {
            logError("BrowserTabRouteDecisionHandler", e)
        }

        return Router.Decision.CANCEL
    }
}
