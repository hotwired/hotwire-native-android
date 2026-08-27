package dev.hotwire.core.config

import android.content.Context
import android.webkit.WebView
import dev.hotwire.core.bridge.BridgeComponent
import dev.hotwire.core.bridge.BridgeComponentFactory
import dev.hotwire.core.bridge.BridgeComponentJsonConverter
import dev.hotwire.core.logging.DefaultHotwireLogger
import dev.hotwire.core.logging.HotwireLogger
import androidx.annotation.RestrictTo
import dev.hotwire.core.security.DefaultHostVerifier
import dev.hotwire.core.security.HostVerifier
import dev.hotwire.core.security.TrustedLocations
import dev.hotwire.core.turbo.config.PathConfiguration
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
     * Set a custom logger instance to handle library log messages in your app.
     *
     * The default logger is [DefaultHotwireLogger] which prints logs to Logcat.
     * If you'd like to change this behavior, provide your own implementation of [HotwireLogger].
     */
    var logger: HotwireLogger = DefaultHotwireLogger

    internal val trustedLocations = TrustedLocations()

    /**
     * A snapshot of the start locations registered by navigator hosts, for
     * use by a custom [HostVerifier] implementation.
     */
    val registeredStartLocations: Set<String>
        get() = trustedLocations.snapshot

    /**
     * Registers a navigator host's start location as trusted. Called by the
     * library when a navigator host initializes; not intended for app use.
     * An app that trusts hosts beyond its navigator start locations provides
     * a custom [hostVerifier] instead of registering additional locations.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun registerTrustedLocation(startLocation: String) = trustedLocations.register(startLocation)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun clearTrustedLocations() = trustedLocations.clear()

    /**
     * Set a custom host verifier instance to decide which hosts the library
     * trusts for in-app navigation, for installing its JavaScript into loaded
     * pages, and for accepting bridge messages from pages.
     *
     * The default verifier is [DefaultHostVerifier], which trusts a location
     * only when its origin (scheme, host, port) is equal to the origin of a
     * registered start location ([registeredStartLocations]). If your app
     * trusts hosts beyond its navigator start locations, provide your own
     * implementation of [HostVerifier].
     */
    var hostVerifier: HostVerifier = DefaultHostVerifier

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
