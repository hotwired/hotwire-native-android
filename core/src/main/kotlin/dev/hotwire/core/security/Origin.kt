package dev.hotwire.core.security

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Scheme, host, and effective port of an http(s) URL, so `https://a.com`
 * and `https://a.com:443/path` are equal.
 */
data class Origin(val scheme: String, val host: String, val port: Int) {
    override fun toString(): String {
        val authority = if (':' in host) "[$host]" else host
        return if (port == HttpUrl.defaultPort(scheme)) "$scheme://$authority" else "$scheme://$authority:$port"
    }

    companion object {
        fun parseOrNull(location: String): Origin? {
            val url = location.toHttpUrlOrNull() ?: return null
            return Origin(url.scheme, url.host, url.port)
        }
    }
}
