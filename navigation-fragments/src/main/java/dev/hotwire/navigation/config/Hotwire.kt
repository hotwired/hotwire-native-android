package dev.hotwire.navigation.config

import androidx.fragment.app.Fragment
import dev.hotwire.core.bridge.BridgeComponent
import dev.hotwire.core.bridge.BridgeComponentFactory
import dev.hotwire.core.config.HotwireConfig
import dev.hotwire.core.config.HotwireCore
import dev.hotwire.navigation.fragments.HotwireWebBottomSheetFragment
import dev.hotwire.navigation.fragments.HotwireWebFragment
import dev.hotwire.navigation.routing.AppNavigationRouteDecisionHandler
import dev.hotwire.navigation.routing.BrowserRouteDecisionHandler
import dev.hotwire.navigation.routing.Router
import kotlin.reflect.KClass

object Hotwire {
    val config: HotwireConfig = HotwireCore.config

    internal var registeredBridgeComponentFactories:
        List<BridgeComponentFactory<BridgeComponent>> = emptyList()
        private set

    internal var registeredFragmentDestinations:
        List<KClass<out Fragment>> = listOf(
            HotwireWebFragment::class,
            HotwireWebBottomSheetFragment::class
        )
        private set

    internal var router = Router(listOf(
        AppNavigationRouteDecisionHandler(),
        BrowserRouteDecisionHandler()
    ))

    /**
     * Registers the [Router.RouteDecisionHandler] instances that determine whether to route location
     * urls within in-app navigation or with alternative custom behaviors.
     */
    fun registerRouteDecisionHandlers(decisionHandlers: List<Router.RouteDecisionHandler>) {
        router = Router(decisionHandlers)
    }

    /**
     * Register bridge components that the app supports. Every possible bridge
     * component, wrapped in a [BridgeComponentFactory], must be provided here.
     */
    fun registerBridgeComponents(factories: List<BridgeComponentFactory<BridgeComponent>>) {
        registeredBridgeComponentFactories = factories
    }

    /**
     * The default fragment destination for web requests. If you have not
     * loaded a path configuration with a matching rule and a `uri` available
     * for all possible paths, this destination will be used as the default.
     */
    var defaultFragmentDestination: KClass<out Fragment> = HotwireWebFragment::class

    /**
     * Register fragment destinations that can be navigated to. Every possible
     * destination must be provided here.
     */
    fun registerFragmentDestinations(destinations: List<KClass<out Fragment>>) {
        registeredFragmentDestinations = destinations
    }
}
