package dev.hotwire.core.security

/**
 * Decides which origins the library trusts. Set via
 * [dev.hotwire.core.config.HotwireConfig.originTrustPolicy].
 *
 * The library only passes authoritative origins here: the WebView's current
 * URL, a WebViewClient callback, or the browser-reported origin of the frame
 * that posted a message. Non-http(s) locations are rejected before the policy
 * is consulted.
 */
interface OriginTrustPolicy {
    fun isTrustedForNavigation(origin: Origin): Boolean

    /**
     * Gates JavaScript injection, bridge and Turbo message dispatch, the file
     * chooser, and native permission grants (geolocation, media capture) for
     * pages at [origin]. Keep this at least as strict as
     * [isTrustedForNavigation].
     */
    fun isTrustedForNativeAccess(origin: Origin): Boolean
}
