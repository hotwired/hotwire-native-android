package dev.hotwire.demo.bridge

import dev.hotwire.core.bridge.BridgeComponentFactory

val bridgeComponentFactories = listOf(
    BridgeComponentFactory("form", ::FormComponent),
    BridgeComponentFactory("menu", ::MenuComponent),
    BridgeComponentFactory("overflow-menu", ::OverflowMenuComponent)
)
