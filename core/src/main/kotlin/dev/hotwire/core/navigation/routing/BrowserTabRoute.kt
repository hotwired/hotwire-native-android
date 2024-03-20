package dev.hotwire.core.navigation.routing

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.google.android.material.R
import dev.hotwire.core.turbo.util.colorFromThemeAttr

class BrowserTabRoute : Router.Route {
    override val name = "browser-tab"

    override fun matches(location: String): Boolean {
        return appUrl.toUri().host != location.toUri().host
    }

    override fun perform(location: String, activity: AppCompatActivity): Router.RouteResult {
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
            .launchUrl(activity, Uri.parse(location))

        return Router.RouteResult.STOP
    }
}
