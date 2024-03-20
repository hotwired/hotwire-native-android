package dev.hotwire.core.navigation.routing

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import dev.hotwire.core.lib.logging.logError

class BrowserRoute : Router.Route {
    override val name = "browser"

    override val result = Router.RouteResult.STOP

    override fun matches(location: String): Boolean {
        return appUrl.toUri().host != location.toUri().host
    }

    override fun handle(location: String, activity: AppCompatActivity) {
        val intent = Intent(Intent.ACTION_VIEW, location.toUri())

        try {
            activity.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            logError("BrowserRoute", e)
        }
    }
}
