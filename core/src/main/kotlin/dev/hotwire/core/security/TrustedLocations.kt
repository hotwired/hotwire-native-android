package dev.hotwire.core.security

import java.util.concurrent.CopyOnWriteArraySet

/**
 * Owns the set of start locations registered by navigator hosts. The public
 * surface lives on [dev.hotwire.core.config.HotwireConfig]: a read-only
 * snapshot for custom [HostVerifier] implementations and library-restricted
 * registration functions.
 */
internal class TrustedLocations {
    private val startLocations = CopyOnWriteArraySet<String>()

    fun register(startLocation: String) {
        startLocations.add(startLocation)
    }

    fun clear() {
        startLocations.clear()
    }

    val snapshot: Set<String> get() = startLocations.toSet()

    fun anyRegistered(predicate: (String) -> Boolean): Boolean =
        startLocations.any(predicate)
}
