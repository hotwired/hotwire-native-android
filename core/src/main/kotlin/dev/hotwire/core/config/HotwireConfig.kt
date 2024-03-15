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
}
