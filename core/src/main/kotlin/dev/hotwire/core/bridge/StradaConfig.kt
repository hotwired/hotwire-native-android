package dev.hotwire.core.bridge

class StradaConfig internal constructor() {
    /**
     * Set a custom JSON converter to easily decode Message.dataJson to a data
     * object in received messages and to encode a data object back to json to
     * reply with a custom message back to the web.
     */
    var jsonConverter: StradaJsonConverter? = null

    /**
     * Enable debug logging to see message communication from/to the WebView.
     *
     * NOTE: You should not enable debug logging in production builds.
     */
    var debugLoggingEnabled = false
}
