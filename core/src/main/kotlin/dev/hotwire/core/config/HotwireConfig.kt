package dev.hotwire.core.config

import android.content.Context
import android.webkit.WebView
import dev.hotwire.core.bridge.BridgeComponent
import dev.hotwire.core.bridge.BridgeComponentFactory
import dev.hotwire.core.bridge.StradaJsonConverter
import dev.hotwire.core.turbo.config.PathConfiguration
import dev.hotwire.core.turbo.http.HotwireHttpClient
import dev.hotwire.core.turbo.offline.OfflineRequestHandler
import dev.hotwire.core.turbo.webview.HotwireWebView

class HotwireConfig internal constructor() {
    /**
     * The path configuration that defines your navigation rules.
     */
    val pathConfiguration = PathConfiguration()

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
    var offlineRequestHandler: OfflineRequestHandler? = null

    /**
     * Enables/disables debug logging. This should be disabled in production environments.
     * Disabled by default.
     *
     * Important: You should not enable debug logging in production release builds.
     */
    var debugLoggingEnabled = false
        set(value) {
            field = value
            HotwireHttpClient.reset()
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
     * your own implementation and subclass [HotwireWebView] if you need
     * custom behaviors.
     */
    var makeCustomWebView: (context: Context) -> HotwireWebView = { context ->
        HotwireWebView(context, null)
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
