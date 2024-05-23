package dev.hotwire.navigation.routing

import dev.hotwire.navigation.activities.HotwireActivity
import dev.hotwire.navigation.logging.logEvent
import dev.hotwire.navigation.navigator.NavigatorConfiguration
import dev.hotwire.navigation.routing.Router.RouteDecisionHandler

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
         * To permit in-app navigation when the location matches this decision
         * handler, return [Decision.NAVIGATE]. To prevent in-app navigation
         * return [Decision.CANCEL].
         */
        val decision: Decision

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
         * Handle custom routing behavior when a match is found. For example,
         * open an external browser or app for external domain urls.
         */
        fun handle(
            location: String,
            configuration: NavigatorConfiguration,
            activity: HotwireActivity
        )
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

                handler.handle(location, configuration, activity)
                return handler.decision
            }
        }

        logEvent("noHandlerForLocation", location)
        return Decision.CANCEL
    }
}
