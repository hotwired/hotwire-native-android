package dev.hotwire.core.security

/**
 * The default [OriginTrustPolicy]: trusts only the origins of the registered
 * start locations.
 */
object DefaultOriginTrustPolicy : OriginTrustPolicy()
