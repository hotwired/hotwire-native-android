package dev.hotwire.core.security

import androidx.annotation.RestrictTo
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * The bare origin (scheme, host, effective port) of an http(s) URL, or null
 * when the string doesn't parse as one.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun String.toOriginOrNull(): HttpUrl? {
    val url = toHttpUrlOrNull() ?: return null
    return HttpUrl.Builder()
        .scheme(url.scheme)
        .host(url.host)
        .port(url.port)
        .build()
}

/**
 * True when both strings parse as http(s) URLs with an equal origin —
 * scheme, host, and effective port.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun String.hasSameOriginAs(other: String): Boolean {
    val origin = toOriginOrNull() ?: return false
    return origin == other.toOriginOrNull()
}
