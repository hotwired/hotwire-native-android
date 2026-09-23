package dev.hotwire.core.security

import dev.hotwire.core.config.Hotwire

/**
 * Trusts only [dev.hotwire.core.config.HotwireConfig.registeredOrigins]. An
 * app that does not use `NavigatorHost` registers none, so it must set its
 * own policy.
 */
object DefaultOriginTrustPolicy : OriginTrustPolicy() {
    override fun isTrustedForNavigation(origin: Origin): Boolean {
        return origin in Hotwire.config.registeredOrigins
    }

    override fun isTrustedForNativeAccess(origin: Origin): Boolean {
        return origin in Hotwire.config.registeredOrigins
    }
}
