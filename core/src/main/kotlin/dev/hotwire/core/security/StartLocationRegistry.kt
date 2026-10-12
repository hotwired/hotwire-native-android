package dev.hotwire.core.security

import androidx.annotation.VisibleForTesting
import dev.hotwire.core.logging.logError
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * Registrations are counted so one navigator host's teardown cannot drop an
 * origin another host still uses.
 */
internal class StartLocationRegistry {
    private val registrationCounts = ConcurrentHashMap<Origin, Int>()

    val origins: Set<Origin> = Collections.unmodifiableSet(registrationCounts.keys)

    fun register(startLocation: String) {
        val origin = Origin.parseOrNull(startLocation) ?: run {
            logError("startLocationNotHttp", startLocation)
            return
        }
        registrationCounts.merge(origin, 1, Int::plus)
    }

    fun unregister(startLocation: String) {
        val origin = Origin.parseOrNull(startLocation) ?: return
        registrationCounts.computeIfPresent(origin) { _, count -> (count - 1).takeIf { it > 0 } }
    }

    @VisibleForTesting
    fun clear() {
        registrationCounts.clear()
    }
}
