package dev.hotwire.core.security

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Scheme, host, and effective port of an http(s) URL, so `https://a.com`
 * and `https://a.com:443/path` are equal. Only parsing creates an origin,
 * so its parts are always normalized.
 */
@ConsistentCopyVisibility
data class Origin private constructor(val scheme: String, val host: String, val port: Int) {
    override fun toString(): String {
        val authority = if (':' in host) "[$host]" else host
        return if (port == HttpUrl.defaultPort(scheme)) "$scheme://$authority" else "$scheme://$authority:$port"
    }

    companion object {
        /**
         * @throws IllegalArgumentException if [location] is not an http(s) URL.
         */
        fun parse(location: String): Origin {
            return requireNotNull(parseOrNull(location)) { "Not an http(s) URL: $location" }
        }

        fun parseOrNull(location: String): Origin? {
            val url = location.toHttpUrlOrNull() ?: return null
            return Origin(url.scheme, url.host, url.port)
        }
    }
}
