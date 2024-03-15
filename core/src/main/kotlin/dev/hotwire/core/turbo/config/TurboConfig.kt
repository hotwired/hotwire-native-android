package dev.hotwire.core.turbo.config

import dev.hotwire.core.turbo.http.TurboHttpClient

class TurboConfig internal constructor() {
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
