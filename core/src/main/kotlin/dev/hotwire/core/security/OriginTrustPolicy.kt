package dev.hotwire.core.security

/**
 * Decides which origins the library trusts. Set via
 * [dev.hotwire.core.config.HotwireConfig.originTrustPolicy].
 *
 * To add to the default, call [DefaultOriginTrustPolicy]. Navigation trust
 * on its own is for locations the path configuration routes to a native
 * fragment. A web destination also needs native access, or its visit fails
 * with `LoadError.UntrustedOrigin`. Compare full origins, not hosts, so a
 * CDN's `http://` urls or other ports stay untrusted:
 *
 * ```
 * private val imageCdn = Origin.parse("https://images.example.com")
 *
 * Hotwire.config.originTrustPolicy = object : OriginTrustPolicy() {
 *     // Lets a native image viewer open CDN images in-app.
 *     override fun isTrustedForNavigation(origin: Origin) =
 *         origin == imageCdn || DefaultOriginTrustPolicy.isTrustedForNavigation(origin)
 *
 *     override fun isTrustedForNativeAccess(origin: Origin) =
 *         DefaultOriginTrustPolicy.isTrustedForNativeAccess(origin)
 * }
 * ```
 *
 * The library only passes authoritative origins here: the WebView's current
 * URL, a WebViewClient callback, or the browser-reported origin of the frame
 * that posted a message.
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
