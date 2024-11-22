package dev.hotwire.navigation.config

import androidx.fragment.app.Fragment
import dev.hotwire.core.bridge.BridgeComponent
import dev.hotwire.core.bridge.BridgeComponentFactory
import dev.hotwire.core.config.Hotwire
import dev.hotwire.navigation.destinations.HotwireDestination
import dev.hotwire.navigation.fragments.HotwireWebBottomSheetFragment
import dev.hotwire.navigation.fragments.HotwireWebFragment
import dev.hotwire.navigation.routing.AppNavigationRouteDecisionHandler
import dev.hotwire.navigation.routing.BrowserRouteDecisionHandler
import dev.hotwire.navigation.routing.Router
import kotlin.reflect.KClass

internal object HotwireNavigation {
    var router = Router(listOf(
        AppNavigationRouteDecisionHandler(),
        BrowserRouteDecisionHandler()
    ))

    var defaultFragmentDestination: KClass<out Fragment> = HotwireWebFragment::class

    var registeredFragmentDestinations: List<KClass<out Fragment>> = listOf(
        HotwireWebFragment::class,
        HotwireWebBottomSheetFragment::class
    )

    @Suppress("UNCHECKED_CAST")
    var registeredBridgeComponentFactories: List<BridgeComponentFactory<HotwireDestination, BridgeComponent<HotwireDestination>>>
        get() = Hotwire.config.registeredBridgeComponentFactories as List<BridgeComponentFactory<HotwireDestination, BridgeComponent<HotwireDestination>>>
        set(value) { Hotwire.config.registeredBridgeComponentFactories = value }
}

/**
 * Registers the [Router.RouteDecisionHandler] instances that determine whether to route location
 * urls within in-app navigation or with alternative custom behaviors.
 */
fun Hotwire.registerRouteDecisionHandlers(vararg decisionHandlers: Router.RouteDecisionHandler) {
    HotwireNavigation.router = Router(decisionHandlers.toList())
}

/**
 * Register bridge components that the app supports. Every possible bridge
 * component, wrapped in a [BridgeComponentFactory], must be provided here.
 */
fun Hotwire.registerBridgeComponents(
    vararg factories: BridgeComponentFactory<HotwireDestination, BridgeComponent<HotwireDestination>>
) {
    config.registeredBridgeComponentFactories = factories.toList()
}

/**
 * The default fragment destination for web requests. If you have not
 * loaded a path configuration with a matching rule and a `uri` available
 * for all possible paths, this destination will be used as the default.
 */
var Hotwire.defaultFragmentDestination: KClass<out Fragment>
    get() = HotwireNavigation.defaultFragmentDestination
    set(value) { HotwireNavigation.defaultFragmentDestination = value }

/**
 * Register fragment destinations that can be navigated to. Every possible
 * destination must be provided here, including one set via [defaultFragmentDestination].
 */
fun Hotwire.registerFragmentDestinations(vararg destinations: KClass<out Fragment>) {
    HotwireNavigation.registeredFragmentDestinations = destinations.toList()
}
