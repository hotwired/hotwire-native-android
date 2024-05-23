package dev.hotwire.core.config

import android.content.Context
import androidx.fragment.app.Fragment
import dev.hotwire.core.bridge.BridgeComponent
import dev.hotwire.core.bridge.BridgeComponentFactory
import dev.hotwire.core.navigation.fragments.HotwireWebBottomSheetFragment
import dev.hotwire.core.navigation.fragments.HotwireWebFragment
import dev.hotwire.core.navigation.routing.AppNavigationRouteDecisionHandler
import dev.hotwire.core.navigation.routing.BrowserRouteDecisionHandler
import dev.hotwire.core.navigation.routing.Router
import dev.hotwire.core.turbo.config.PathConfiguration
import kotlin.reflect.KClass

object Hotwire {
    var registeredBridgeComponentFactories:
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

    val config: HotwireConfig = HotwireConfig()

    /**
     * The path configuration that defines your navigation rules.
     */
    val pathConfiguration = PathConfiguration()

    /**
     * Loads the [PathConfiguration] JSON file(s) from the provided location to
     * configure navigation rules.
     */
    fun loadPathConfiguration(context: Context, location: PathConfiguration.Location) {
        pathConfiguration.load(context, location)
    }

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
