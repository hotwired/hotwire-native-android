package dev.hotwire.core.bridge

interface BridgeDestination {
    fun bridgeWebViewIsReady(): Boolean
    fun onBridgeComponentInitialized(component: BridgeComponent<*>) {}
}
