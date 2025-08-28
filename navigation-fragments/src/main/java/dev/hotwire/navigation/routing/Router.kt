package dev.hotwire.navigation.routing

import dev.hotwire.navigation.activities.HotwireActivity
import dev.hotwire.navigation.logging.logEvent
import dev.hotwire.navigation.navigator.NavigatorConfiguration

/**
 * Routes location urls within in-app navigation or with custom behaviors
 * provided in [RouteDecisionHandler] instances.
 */
class Router(private val decisionHandlers: List<RouteDecisionHandler>) {

    /**
     * An interface to implement to provide custom route decision handling
     * behaviors in your app.
     */
    interface RouteDecisionHandler {
        /**
         * The decision handler name used in debug logging.
         */
        val name: String

        /**
         * Determines whether the location matches this decision handler. Use
         * your own custom rules based on the location's domain, protocol,
         * path, or any other factors.
         */
        fun matches(
            location: String,
            configuration: NavigatorConfiguration
        ): Boolean

        /**
         * Handle custom routing behavior when a match is found. To continue with in-app
         * navigation, return [Decision.NAVIGATE]. To prevent in-app navigation return
         * [Decision.CANCEL].
         */
        fun handle(
            location: String,
            configuration: NavigatorConfiguration,
            activity: HotwireActivity
        ): Decision
    }

    enum class Decision {
        /**
         * Permit in-app navigation with your app's domain urls.
         */
        NAVIGATE,

        /**
         * Prevent in-app navigation. Always use this for external domain urls.
         */
        CANCEL
    }

    internal fun decideRoute(
        location: String,
        configuration: NavigatorConfiguration,
        activity: HotwireActivity
    ): Decision {
        decisionHandlers.forEach { handler ->
            if (handler.matches(location, configuration)) {
                logEvent("handlerMatch", listOf(
                    "handler" to handler.name,
                    "location" to location
                ))

                return handler.handle(location, configuration, activity)
            }
        }

        logEvent("noHandlerForLocation", location)
        return Decision.CANCEL
    }
}
