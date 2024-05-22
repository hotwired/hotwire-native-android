package dev.hotwire.core.navigation.routing

import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.google.android.material.R
import dev.hotwire.core.navigation.activities.HotwireActivity
import dev.hotwire.core.navigation.navigator.NavigatorConfiguration
import dev.hotwire.core.turbo.util.colorFromThemeAttr

class BrowserTabRouteDecisionHandler : Router.RouteDecisionHandler {
    override val name = "browser-tab"

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
        val color = activity.colorFromThemeAttr(R.attr.colorSurface)
        val colorParams = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(color)
            .setNavigationBarColor(color)
            .build()

        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setShareState(CustomTabsIntent.SHARE_STATE_ON)
            .setUrlBarHidingEnabled(false)
            .setDefaultColorSchemeParams(colorParams)
            .build()
            .launchUrl(activity, location.toUri())
    }
}
