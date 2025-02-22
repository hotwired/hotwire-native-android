package dev.hotwire.core.turbo.config

object PathConfigurationClient {
    private var customHeaders: Map<String, String>? = null

    /**
     * Allows setting custom headers specifically for path configuration requests.
     * If this is never called, the original functionality remains unchanged.
     * @param headers A map of header names to header values.
     *
     *
     */
    fun setPathConfigurationHeaders(headers: Map<String, String>) {
            customHeaders = headers.ifEmpty { null }
    }

    /**
     * @return map of custom headers, or null if none have been set.
     */
    internal fun getHeaders(): Map<String, String>? = customHeaders
}