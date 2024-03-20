package dev.hotwire.core.navigation.routing

import androidx.appcompat.app.AppCompatActivity
import dev.hotwire.core.config.Hotwire
import dev.hotwire.core.lib.logging.logEvent
import dev.hotwire.core.navigation.routing.Router.Route

/**
 * Routes location urls within in-app navigation or with custom behaviors
 * provided in [Route] instances.
 */
class Router(private val routes: List<Route>) {

    /**
     * An interface to implement to provide custom route behaviors in your app.
     */
    interface Route {
        /**
         * The configured app url. You can use this to determine if a location
         * exists on the same domain.
         */
        val appUrl get() = Hotwire.appUrl

        /**
         * The route name used in debug logging.
         */
        val name: String

        /**
         * To permit in-app navigation when the location matches this route,
         * return [RouteResult.NAVIGATE]. To prevent in-app navigation return
         * [RouteResult.STOP].
         */
        val result: RouteResult

        /**
         * Determines whether the location matches this route. Use your own custom
         * rules based on the location's domain, protocol, path, or any other
         * factors.
         */
        fun matches(location: String): Boolean

        /**
         * Perform custom routing behavior when a match is found. For example,
         * open an external browser or app for external domain urls.
         */
        fun perform(location: String, activity: AppCompatActivity)
    }

    enum class RouteResult {
        /**
         * Permit in-app navigation with your app's domain urls.
         */
        NAVIGATE,

        /**
         * Prevent in-app navigation. Always use this for external domain urls.
         */
        STOP
    }

    internal fun route(location: String, activity: AppCompatActivity): RouteResult {
        routes.forEach { route ->
            if (route.matches(location)) {
                logEvent("routeMatch", listOf(
                    "route" to route.name,
                    "location" to location
                ))

                route.perform(location, activity)
                return route.result
            }
        }

        logEvent("noRouteForLocation", location)
        return RouteResult.STOP
    }
}
