package dev.hotwire.core.security

import dev.hotwire.core.logging.logError
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap

/**
 * Owns the origins of the registered start locations; the public surface
 * lives on [dev.hotwire.core.config.HotwireConfig].
 *
 * Registrations are counted so one host's teardown cannot drop an origin
 * another host still uses.
 */
internal class TrustedLocations {
    private val origins = ConcurrentHashMap<HttpUrl, Int>()

    fun register(startLocation: String) {
        val origin = startLocation.toOriginOrNull() ?: run {
            logError("startLocationNotTrustable", "Not an http(s) URL: $startLocation")
            return
        }
        origins.merge(origin, 1, Int::plus)
    }

    fun unregister(startLocation: String) {
        val origin = startLocation.toOriginOrNull() ?: return
        origins.computeIfPresent(origin) { _, count -> (count - 1).takeIf { it > 0 } }
    }

    fun clear() {
        origins.clear()
    }

    val snapshot: Set<String> get() = origins.keys.mapTo(mutableSetOf()) { it.toString() }

    fun isTrustedOrigin(location: String): Boolean {
        val origin = location.toOriginOrNull() ?: return false
        return origins.containsKey(origin)
    }
}
