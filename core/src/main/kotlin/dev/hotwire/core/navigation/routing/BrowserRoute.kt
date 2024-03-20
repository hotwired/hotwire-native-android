package dev.hotwire.core.navigation.routing

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import dev.hotwire.core.logging.logError

class BrowserRoute : Router.Route {
    override val name = "browser"

    override fun matches(location: String): Boolean {
        return appUrl.toUri().host != location.toUri().host
    }

    override fun perform(location: String, activity: AppCompatActivity): Router.RouteResult {
        val uri: Uri = Uri.parse(location)
        val intent = Intent(Intent.ACTION_VIEW, uri)

        try {
            activity.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            logError("BrowserRoute", e)
        }

        return Router.RouteResult.STOP
    }
}
