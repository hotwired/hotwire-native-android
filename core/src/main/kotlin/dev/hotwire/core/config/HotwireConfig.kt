package dev.hotwire.core.config

import android.content.Context
import android.webkit.WebView
import dev.hotwire.core.bridge.BridgeComponent
import dev.hotwire.core.bridge.BridgeComponentFactory
import dev.hotwire.core.bridge.StradaJsonConverter
import dev.hotwire.core.turbo.config.PathConfiguration
import dev.hotwire.core.turbo.http.ExperimentalOfflineRequestHandler
import dev.hotwire.core.turbo.http.TurboHttpClient
import dev.hotwire.core.turbo.http.TurboOfflineRequestHandler
import dev.hotwire.core.turbo.views.TurboWebView

class HotwireConfig internal constructor() {
    /**
     * The path configuration that defines your navigation rules.
     */
    val pathConfiguration = PathConfiguration()

    /**
     * Loads the [PathConfiguration] JSON file(s) from the provided location to
     * configure navigation rules.
     */
    fun loadPathConfiguration(context: Context, location: PathConfiguration.Location) {
        pathConfiguration.load(context, location)
    }

    var registeredBridgeComponentFactories:
            List<BridgeComponentFactory<*, BridgeComponent<*>>> = emptyList()

    /**
     * Set a custom JSON converter to easily decode Message.dataJson to a data
     * object in received messages and to encode a data object back to json to
     * reply with a custom message back to the web.
     */
    var jsonConverter: StradaJsonConverter? = null

    /**
     * Experimental: API may be removed, not ready for production use.
     */
    @OptIn(ExperimentalOfflineRequestHandler::class)
    var offlineRequestHandler: TurboOfflineRequestHandler? = null

    /**
     * Enables/disables debug logging. This should be disabled in production environments.
     * Disabled by default.
     *
     * Important: You should not enable debug logging in production release builds.
     */
    var debugLoggingEnabled = false
        set(value) {
            field = value
            TurboHttpClient.reset()
        }

    /**
     * Enables/disables debugging of web contents loaded into WebViews.
     * Disabled by default.
     *
     * Important: You should not enable debugging in production release builds.
     */
    var webViewDebuggingEnabled = false
        set(value) {
            field = value
            WebView.setWebContentsDebuggingEnabled(value)
        }

    /**
     * Called whenever a new WebView instance needs to be (re)created. Provide
     * your own implementation and subclass [TurboWebView] if you need
     * custom behaviors.
     */
    var makeCustomWebView: (context: Context) -> TurboWebView = { context ->
        TurboWebView(context, null)
    }

    /**
     * Provides a standard substring to be included in your WebView's user agent
     * to identify itself as a Hotwire Native app.
     *
     * Important: Ensure that you've registered your bridge components before
     * calling this so the bridge component names are included in your user agent.
     */
    fun userAgentSubstring(): String {
        val components = registeredBridgeComponentFactories.joinToString(" ") { it.name }
        return "Turbo Native Android; bridge-components: [$components];"
    }

    /**
     * Set a custom user agent for every WebView instance.
     *
     * Important: Include `Hotwire.userAgentSubstring()` as part of your
     * custom user agent for compatibility with your server.
     */
    var userAgent: String = userAgentSubstring()
}
