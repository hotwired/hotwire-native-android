package dev.hotwire.core.config

import android.content.Context
import dev.hotwire.core.turbo.config.PathConfiguration
import dev.hotwire.core.turbo.webview.WebViewInfo

object Hotwire {
    val config: HotwireConfig = HotwireConfig()

    /**
     * Provides useful version and type information about the system WebView component installed
     * on the device. This can be used in your app to require a minimum system WebView version on
     * the device and point users to the Play Store to update the corresponding app (Google Chrome
     * or Android System WebView).
     */
    fun webViewInfo(context: Context) = WebViewInfo(context.applicationContext)

    /**
     * Loads the [PathConfiguration] JSON file(s) from the provided location to
     * configure navigation rules.
     */
    fun loadPathConfiguration(
        context: Context,
        location: PathConfiguration.Location,
        clientConfig: PathConfiguration.ClientConfig = PathConfiguration.ClientConfig()
    ) {
        config.pathConfiguration.load(context.applicationContext, location, clientConfig)
    }
}
