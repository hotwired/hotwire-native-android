package dev.hotwire.core.security

import androidx.annotation.RestrictTo
import dev.hotwire.core.config.Hotwire

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun String.hasSameOriginAs(other: String): Boolean {
    val origin = Origin.parseOrNull(this) ?: return false
    return origin == Origin.parseOrNull(other)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun isTrustedForNavigation(location: String?): Boolean {
    val origin = location?.let { Origin.parseOrNull(it) } ?: return false
    return Hotwire.config.originTrustPolicy.isTrustedForNavigation(origin)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun isTrustedForNativeAccess(location: String?): Boolean {
    val origin = location?.let { Origin.parseOrNull(it) } ?: return false
    val policy = Hotwire.config.originTrustPolicy
    return policy.isTrustedForNavigation(origin) && policy.isTrustedForNativeAccess(origin)
}
