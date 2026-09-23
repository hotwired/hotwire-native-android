package dev.hotwire.core.security

import dev.hotwire.core.config.Hotwire

/**
 * Decides which origins the library trusts. Set via
 * [dev.hotwire.core.config.HotwireConfig.originTrustPolicy].
 *
 * Each method trusts only [dev.hotwire.core.config.HotwireConfig.registeredOrigins]
 * unless you override it. Compare full origins, not hosts, so a partner's
 * `http://` pages or other ports stay untrusted:
 *
 * ```
 * private val partner = Origin.parse("https://partner.example.com")
 *
 * Hotwire.config.originTrustPolicy = object : OriginTrustPolicy() {
 *     override fun isTrustedForNavigation(origin: Origin) =
 *         origin == partner || super.isTrustedForNavigation(origin)
 * }
 * ```
 *
 * The library only passes authoritative origins here: the WebView's current
 * URL, a WebViewClient callback, or the browser-reported origin of the frame
 * that posted a message. Non-http(s) locations are rejected before the policy
 * is consulted.
 */
abstract class OriginTrustPolicy {
    open fun isTrustedForNavigation(origin: Origin): Boolean {
        return origin in Hotwire.config.registeredOrigins
    }

    /**
     * Gates JavaScript injection, bridge and Turbo message dispatch, the file
     * chooser, and native permission grants (geolocation, media capture) for
     * pages at [origin]. The library also requires [isTrustedForNavigation],
     * so trusting an origin here alone has no effect.
     */
    open fun isTrustedForNativeAccess(origin: Origin): Boolean {
        return origin in Hotwire.config.registeredOrigins
    }
}
