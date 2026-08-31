package dev.hotwire.core.security

import java.util.concurrent.CopyOnWriteArraySet

/**
 * Owns the registered start locations; the public surface lives on
 * [dev.hotwire.core.config.HotwireConfig].
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
