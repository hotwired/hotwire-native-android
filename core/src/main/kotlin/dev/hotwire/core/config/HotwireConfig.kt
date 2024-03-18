package dev.hotwire.core.config

import dev.hotwire.core.bridge.StradaJsonConverter
import dev.hotwire.core.turbo.http.TurboHttpClient

class HotwireConfig internal constructor() {
    /**
     * Set a custom JSON converter to easily decode Message.dataJson to a data
     * object in received messages and to encode a data object back to json to
     * reply with a custom message back to the web.
     */
    var jsonConverter: StradaJsonConverter? = null

    /**
     * Enables/disables debug logging. This should be disabled in production environments.
     * Disabled by default.
     *
     */
    var debugLoggingEnabled = false
        set(value) {
            field = value
            TurboHttpClient.reset()
        }

    /**
     * Provides a standard substring to be included in your WebView's user agent
     * to identify itself as a Hotwire Native app.
     *
     * Important: Ensure that you've registered your bridge components before
     * calling this so the bridge component names are included in your user agent.
     */
    fun userAgentSubstring(): String {
        val components = Hotwire.registeredBridgeComponentFactories.joinToString(" ") { it.name }
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
