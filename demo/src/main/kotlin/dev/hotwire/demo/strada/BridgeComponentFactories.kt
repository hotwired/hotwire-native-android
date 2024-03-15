package dev.hotwire.demo.strada

import dev.hotwire.core.bridge.BridgeComponentFactory

val bridgeComponentFactories = listOf(
    BridgeComponentFactory("form", ::FormComponent),
    BridgeComponentFactory("menu", ::MenuComponent),
    BridgeComponentFactory("overflow-menu", ::OverflowMenuComponent)
)
