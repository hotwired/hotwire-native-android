package dev.hotwire.core.security

import androidx.annotation.RestrictTo
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * True when both strings parse as http(s) URLs with an equal origin —
 * scheme, host, and effective port.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun String.hasSameOriginAs(other: String): Boolean {
    val origin = toHttpUrlOrNull() ?: return false
    val otherOrigin = other.toHttpUrlOrNull() ?: return false

    return origin.scheme == otherOrigin.scheme &&
        origin.host == otherOrigin.host &&
        origin.port == otherOrigin.port
}
