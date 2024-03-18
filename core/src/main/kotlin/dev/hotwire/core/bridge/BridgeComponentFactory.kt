package dev.hotwire.core.bridge

class BridgeComponentFactory<out C : BridgeComponent> constructor(
    val name: String,
    private val creator: (name: String, delegate: BridgeDelegate) -> C
) {
    fun create(delegate: BridgeDelegate) = creator(name, delegate)
}
