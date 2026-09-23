package dev.hotwire.core.security

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import dev.hotwire.core.logging.logError
import java.util.concurrent.ConcurrentHashMap

/**
 * Registrations are counted so one navigator host's teardown cannot drop an
 * origin another host still uses.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class TrustedOrigins {
    private val origins = ConcurrentHashMap<Origin, Int>()

    fun register(startLocation: String) {
        val origin = Origin.parseOrNull(startLocation) ?: run {
            logError("startLocationNotTrustable", "Not an http(s) URL: $startLocation")
            return
        }
        origins.merge(origin, 1, Int::plus)
    }

    fun unregister(startLocation: String) {
        val origin = Origin.parseOrNull(startLocation) ?: return
        origins.computeIfPresent(origin) { _, count -> (count - 1).takeIf { it > 0 } }
    }

    @VisibleForTesting
    fun clear() {
        origins.clear()
    }

    val snapshot: Set<Origin> get() = origins.keys.toSet()

    fun contains(origin: Origin): Boolean = origins.containsKey(origin)
}
