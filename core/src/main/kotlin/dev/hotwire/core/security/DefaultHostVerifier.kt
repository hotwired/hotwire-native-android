package dev.hotwire.core.security

import dev.hotwire.core.config.Hotwire

/**
 * Trusts a location only when its origin (scheme, host, port) is equal to the
 * origin of one of the registered start locations
 * ([dev.hotwire.core.config.HotwireConfig.registeredStartLocations]).
 * Locations that don't parse as http(s) URLs are never trusted.
 */
internal object DefaultHostVerifier : HostVerifier {
    override fun isTrustedForNavigation(location: String): Boolean {
        return isTrustedOrigin(location)
    }

    override fun isTrustedForBridge(location: String): Boolean {
        return isTrustedOrigin(location)
    }

    private fun isTrustedOrigin(location: String): Boolean {
        return Hotwire.config.trustedLocations.anyRegistered { startLocation ->
            location.hasSameOriginAs(startLocation)
        }
    }
}
