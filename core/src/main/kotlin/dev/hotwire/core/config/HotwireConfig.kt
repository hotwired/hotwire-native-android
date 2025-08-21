package dev.hotwire.core.config

import android.content.Context
import android.webkit.WebView
import dev.hotwire.core.bridge.BridgeComponent
import dev.hotwire.core.bridge.BridgeComponentFactory
import dev.hotwire.core.bridge.BridgeComponentJsonConverter
import dev.hotwire.core.logging.DefaultHotwireLogger
import dev.hotwire.core.logging.HotwireLogger
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
    var jsonConverter: BridgeComponentJsonConverter? = null

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
     * Set a custom logger to handle debug, warning, and error messages.
     * The default logger is [DefaultHotwireLogger].
     *
     * If you implement your own logger, you should handle debug logging according the the value
     * of [debugLoggingEnabled].
     */
    var logger: HotwireLogger = DefaultHotwireLogger

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
     * Set a custom user agent application prefix for every WebView instance. The
     * library will automatically append a substring to your prefix which includes:
     * - "Hotwire Native Android; Turbo Native Android;"
     * - "bridge-components: [your bridge components];"
     * - The WebView's default Chromium user agent string
     */
    var applicationUserAgentPrefix: String? = null

    /**
     * Gets the user agent that the library builds to identify the app
     * and its registered bridge components. This includes:
     * - Your (optional) custom `applicationUserAgentPrefix`
     * - "Hotwire Native Android; Turbo Native Android;"
     * - "bridge-components: [your bridge components];"
     */
    val userAgent: String get() {
        val components = registeredBridgeComponentFactories.joinToString(" ") { it.name }

        return listOf(
            applicationUserAgentPrefix,
            "Hotwire Native Android; Turbo Native Android;",
            "bridge-components: [$components];"
        ).filterNotNull().joinToString(" ")
    }

    /**
     * Gets the full user agent that is used for every WebView instance. This includes:
     * - Your (optional) custom `applicationUserAgentPrefix`
     * - "Hotwire Native Android; Turbo Native Android;"
     * - "bridge-components: [your bridge components];"
     * - The WebView's default Chromium user agent string
     */
    fun userAgentWithWebViewDefault(context: Context): String {
        return "$userAgent ${Hotwire.webViewInfo(context).defaultUserAgent}"
    }
}
