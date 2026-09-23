package dev.hotwire.core.security

/**
 * Decides which origins the library trusts. Set via
 * [dev.hotwire.core.config.HotwireConfig.originTrustPolicy].
 *
 * Apps with their own source of trust answer both questions from it. Apps
 * that only add to the default call [DefaultOriginTrustPolicy]. Compare full
 * origins, not hosts, so a partner's `http://` pages or other ports stay
 * untrusted:
 *
 * ```
 * private val partner = Origin.parse("https://partner.example.com")
 *
 * Hotwire.config.originTrustPolicy = object : OriginTrustPolicy() {
 *     override fun isTrustedForNavigation(origin: Origin) =
 *         origin == partner || DefaultOriginTrustPolicy.isTrustedForNavigation(origin)
 *
 *     override fun isTrustedForNativeAccess(origin: Origin) =
 *         DefaultOriginTrustPolicy.isTrustedForNativeAccess(origin)
 * }
 * ```
 *
 * The library only passes authoritative origins here: the WebView's current
 * URL, a WebViewClient callback, or the browser-reported origin of the frame
 * that posted a message. Non-http(s) locations are rejected before the policy
 * is consulted.
 */
abstract class OriginTrustPolicy {
    // Add future gates as open methods that default to isTrustedForNativeAccess,
    // so existing policies keep compiling and answer with their strictest trust.

    abstract fun isTrustedForNavigation(origin: Origin): Boolean

    /**
     * Gates JavaScript injection, bridge and Turbo message dispatch, the file
     * chooser, and native permission grants (geolocation, media capture) for
     * pages at [origin]. The library also requires [isTrustedForNavigation],
     * so trusting an origin here alone has no effect.
     */
    abstract fun isTrustedForNativeAccess(origin: Origin): Boolean
}
