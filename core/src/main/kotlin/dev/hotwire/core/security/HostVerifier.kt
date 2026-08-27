package dev.hotwire.core.security

/**
 * Verifies that a location's host can be trusted before the library performs
 * a security-sensitive action with it.
 *
 * The default implementation trusts a location only when its origin (scheme,
 * host, port) is equal to the origin of the navigator's start location. This
 * works for apps that run on a single origin.
 *
 * Provide your own implementation via [dev.hotwire.core.config.HotwireConfig.hostVerifier]
 * when your app trusts multiple hosts (for example, separate asset/storage
 * hosts) or already has a central host verification facility.
 */
interface HostVerifier {
    /**
     * Determines whether the library may route [location] through in-app
     * navigation.
     *
     * @param location The location whose host needs verification.
     * @param startLocation The start location of the navigator this decision
     * belongs to — the app-authored trust anchor.
     */
    fun isTrustedForNavigation(location: String, startLocation: String): Boolean

    /**
     * Determines whether the library may inject its JavaScript into a page at
     * [location], register bridge components with it, and dispatch bridge
     * messages received from it.
     *
     * Keep this at least as strict as [isTrustedForNavigation]: a page that
     * passes this check can exchange messages with the app's native bridge
     * components.
     *
     * @param location The location whose host needs verification. The library
     * passes authoritative values here (the WebView's current URL or a
     * WebViewClient callback), never page-supplied data.
     * @param startLocation The start location of the navigator this decision
     * belongs to — the app-authored trust anchor.
     */
    fun isTrustedForBridge(location: String, startLocation: String): Boolean
}
