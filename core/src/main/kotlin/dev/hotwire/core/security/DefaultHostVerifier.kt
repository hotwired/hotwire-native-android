package dev.hotwire.core.security

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Trusts a location only when its origin (scheme, host, port) is equal to the
 * origin of the navigator's start location. Locations that don't parse as
 * http(s) URLs are never trusted.
 */
internal object DefaultHostVerifier : HostVerifier {
    override fun isTrustedForNavigation(location: String, startLocation: String): Boolean {
        return hasSameOrigin(location, startLocation)
    }

    override fun isTrustedForBridge(location: String, startLocation: String): Boolean {
        return hasSameOrigin(location, startLocation)
    }

    private fun hasSameOrigin(location: String, startLocation: String): Boolean {
        val url = location.toHttpUrlOrNull() ?: return false
        val startUrl = startLocation.toHttpUrlOrNull() ?: return false

        return url.hasSameOriginAs(startUrl)
    }

    private fun HttpUrl.hasSameOriginAs(other: HttpUrl): Boolean {
        return scheme == other.scheme &&
            host == other.host &&
            port == other.port
    }
}
