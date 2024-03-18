package dev.hotwire.core.config

import dev.hotwire.core.bridge.BridgeComponentFactory

object Hotwire {
    val config: HotwireConfig = HotwireConfig()

    /**
     * Provides a standard substring to be included in your WebView's user agent
     * to identify itself as a Hotwire Native app.
     */
    fun userAgentSubstring(componentFactories: List<BridgeComponentFactory<*, *>>): String {
        val components = componentFactories.joinToString(" ") { it.name }
        return "Turbo Native Android; bridge-components: [$components];"
    }
}
