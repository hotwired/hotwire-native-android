package dev.hotwire.core.bridge

object Strada {
    val config: StradaConfig = StradaConfig()

    fun userAgentSubstring(componentFactories: List<BridgeComponentFactory<*,*>>): String {
        val components = componentFactories.joinToString(" ") { it.name }
        return "bridge-components: [$components]"
    }
}
