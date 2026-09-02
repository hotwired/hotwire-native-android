package dev.hotwire.core.security

/**
 * Verifies that a location's host can be trusted before the library performs
 * a security-sensitive action with it. Configured via
 * [dev.hotwire.core.config.HotwireConfig.hostVerifier].
 */
interface HostVerifier {
    /**
     * Determines whether the library may route [location] through in-app
     * navigation.
     */
    fun isTrustedForNavigation(location: String): Boolean

    /**
     * Determines whether the library may inject its JavaScript into a page at
     * [location], register bridge components with it, dispatch bridge and
     * Turbo messages received from it, and grant it native capabilities
     * (geolocation, media capture).
     *
     * Keep this at least as strict as [isTrustedForNavigation]: a page that
     * passes this check can exchange messages with the app's native bridge
     * components. The library passes authoritative values here (the WebView's
     * current URL, a WebViewClient callback, or the browser-reported origin
     * of the frame that posted a message), never page-supplied data. Note the
     * frame origin arrives as a bare origin (`https://host:port`), not a
     * full URL.
     */
    fun isTrustedForBridge(location: String): Boolean
}
