package dev.hotwire.core.security

import dev.hotwire.core.config.Hotwire

/**
 * Trusts only the origins of the registered start locations. A custom policy
 * that also wants to trust those origins can delegate to this object:
 *
 * ```
 * Hotwire.config.originTrustPolicy = object : OriginTrustPolicy by DefaultOriginTrustPolicy {
 *     override fun isTrustedForNavigation(origin: Origin) =
 *         origin.host == "partner.example.com" || DefaultOriginTrustPolicy.isTrustedForNavigation(origin)
 * }
 * ```
 */
object DefaultOriginTrustPolicy : OriginTrustPolicy {
    override fun isTrustedForNavigation(origin: Origin): Boolean {
        return Hotwire.config.trustedOrigins.contains(origin)
    }

    override fun isTrustedForNativeAccess(origin: Origin): Boolean {
        return Hotwire.config.trustedOrigins.contains(origin)
    }
}
