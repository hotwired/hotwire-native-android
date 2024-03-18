package dev.hotwire.core.config

import androidx.fragment.app.Fragment
import dev.hotwire.core.bridge.BridgeComponent
import dev.hotwire.core.bridge.BridgeComponentFactory
import kotlin.reflect.KClass

object Hotwire {
    internal var registeredBridgeComponentFactories:
        List<BridgeComponentFactory<BridgeComponent>> = emptyList()
        private set

    internal var registeredFragmentDestinations: List<KClass<out Fragment>> = emptyList()
        private set

    val config: HotwireConfig = HotwireConfig()

    /**
     * Register bridge component factories that the app supports. Every possible bridge
     * component must be provided here.
     */
    fun registerBridgeComponentFactories(factories: List<BridgeComponentFactory<BridgeComponent>>) {
        registeredBridgeComponentFactories = factories
    }

    /**
     * Register fragment destinations that can be navigated to. Every possible
     * destination must be provided here.
     */
    fun registerFragmentDestinations(destinations: List<KClass<out Fragment>>) {
        registeredFragmentDestinations = destinations
    }
}
