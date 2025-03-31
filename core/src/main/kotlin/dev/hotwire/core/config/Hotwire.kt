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
     * @param context The application or activity context.
     * @param location Specifies local and/or remote location to retrieve path configuration files.
     * @param options Optional loader options to use when fetching remote path configuration files
     *  from your server.
     */
    fun loadPathConfiguration(
        context: Context,
        location: PathConfiguration.Location,
        options: PathConfiguration.LoaderOptions = PathConfiguration.LoaderOptions(),
        onCompletion: (PathConfiguration) -> Unit = {}
    ) {
        config.pathConfiguration.load(context.applicationContext, location, options, onCompletion)
    }
}
