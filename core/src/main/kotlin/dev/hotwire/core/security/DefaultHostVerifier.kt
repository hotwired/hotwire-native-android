package dev.hotwire.core.security

import dev.hotwire.core.config.Hotwire
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

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
        val url = location.toHttpUrlOrNull() ?: return false

        return Hotwire.config.trustedLocations.anyRegistered { startLocation ->
            startLocation.toHttpUrlOrNull()?.let { url.hasSameOriginAs(it) } == true
        }
    }

    private fun HttpUrl.hasSameOriginAs(other: HttpUrl): Boolean {
        return scheme == other.scheme &&
            host == other.host &&
            port == other.port
    }
}
