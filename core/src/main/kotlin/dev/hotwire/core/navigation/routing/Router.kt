package dev.hotwire.core.navigation.routing

import androidx.appcompat.app.AppCompatActivity
import dev.hotwire.core.config.Hotwire
import dev.hotwire.core.logging.logEvent
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
         * The route name used in debug logging.
         */
        val name: String

        /**
         * The configured app url. You can use this to determine if a location
         * exists on the same domain.
         */
        val appUrl get() = Hotwire.appUrl

        /**
         * Determines whether the location matches this route. Use your own custom
         * rules based on the location's domain, protocol, path, or any other
         * factors. For example, external domain urls or mailto: links should not
         * be sent through the normal navigation flow.
         */
        fun matches(location: String): Boolean

        /**
         * Perform the custom routing behavior. To permit in-app navigation,
         * return [RouteResult.NAVIGATE]. To prevent in-app navigation, perform
         * your own custom behavior and return [RouteResult.STOP]. External
         * domain urls should always return [RouteResult.STOP].
         */
        fun perform(location: String, activity: AppCompatActivity): RouteResult
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
        routes.forEach {
            if (it.matches(location)) {
                logEvent("routeMatch", listOf(
                    "route" to it.name,
                    "location" to location
                ))
                return it.perform(location, activity)
            }
        }

        logEvent("noRouteForLocation", location)
        return RouteResult.STOP
    }
}
