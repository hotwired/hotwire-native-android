package dev.hotwire.core.security

/**
 * Verifies that a location's host can be trusted before the library performs
 * a security-sensitive action with it.
 *
 * The default implementation trusts a location only when its origin (scheme,
 * host, port) is equal to the origin of a location the app has registered as
 * trusted — every navigator host registers its start location automatically.
 * This works for apps that run on one or more app-declared origins.
 *
 * Provide your own implementation via [dev.hotwire.core.config.HotwireConfig.hostVerifier]
 * when your app trusts hosts beyond its navigator start locations (for
 * example, separate asset/storage hosts) or already has a central host
 * verification facility. A custom implementation can read the registered
 * start locations from [dev.hotwire.core.config.HotwireConfig.registeredStartLocations].
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
     * current URL or a WebViewClient callback), never page-supplied data.
     */
    fun isTrustedForBridge(location: String): Boolean
}
