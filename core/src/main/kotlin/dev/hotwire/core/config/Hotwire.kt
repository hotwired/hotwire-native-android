package dev.hotwire.core.config

import dev.hotwire.core.bridge.BridgeComponent
import dev.hotwire.core.bridge.BridgeComponentFactory

object Hotwire {
    internal var registeredBridgeComponentFactories:
        List<BridgeComponentFactory<BridgeComponent>> = emptyList()
        private set

    val config: HotwireConfig = HotwireConfig()

    fun registerBridgeComponentFactories(factories: List<BridgeComponentFactory<BridgeComponent>>) {
        registeredBridgeComponentFactories = factories
    }

    /**
     * Provides a standard substring to be included in your WebView's user agent
     * to identify itself as a Hotwire Native app.
     */
    fun userAgentSubstring(): String {
        val components = registeredBridgeComponentFactories.joinToString(" ") { it.name }
        return "Turbo Native Android; bridge-components: [$components];"
    }
}
