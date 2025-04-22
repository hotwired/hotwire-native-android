package dev.hotwire.navigation.activities

import androidx.activity.OnBackPressedCallback
import androidx.annotation.IdRes
import dev.hotwire.navigation.logging.logEvent
import dev.hotwire.navigation.navigator.Navigator
import dev.hotwire.navigation.navigator.NavigatorConfiguration
import dev.hotwire.navigation.navigator.NavigatorHost
import dev.hotwire.navigation.observers.HotwireActivityObserver

/**
 * Initializes the Activity for Hotwire navigation and provides all the hooks for an
 * Activity to communicate with Hotwire Native (and vice versa).
 *
 * @property activity The Activity to bind this delegate to.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class HotwireActivityDelegate(val activity: HotwireActivity) {
    private val navigatorHosts = mutableMapOf<Int, NavigatorHost>()

    private val onBackPressedCallback = object : OnBackPressedCallback(enabled = true) {
        override fun handleOnBackPressed() {
            currentNavigator?.pop()
        }
    }

    private var currentNavigatorHostId = activity.navigatorConfigurations().first().navigatorHostId

    /**
     * Initializes the Activity with a BackPressedDispatcher that properly
     * handles Fragment navigation with the back button.
     */
    init {
        activity.lifecycle.addObserver(HotwireActivityObserver())
        activity.onBackPressedDispatcher.addCallback(
            owner = activity,
            onBackPressedCallback = onBackPressedCallback
        )
    }

    /**
     * Get the Activity's currently active [Navigator].
     *
     * Returns null if the navigator is not ready for navigation.
     */
    val currentNavigator: Navigator?
        get() {
            val host = navigatorHosts[currentNavigatorHostId]

            return if (host?.isReady() == true) {
                host.navigator
            } else {
                null
            }
        }


    /**
     * Sets the currently active navigator in your Activity. If you use multiple
     *  [NavigatorHost] instances in your app (such as for bottom tabs),
     *  you must update this whenever the current navigator changes.
     */
    fun setCurrentNavigator(configuration: NavigatorConfiguration) {
        logEvent("navigatorSetAsCurrent", listOf("navigator" to configuration.name))
        currentNavigatorHostId = configuration.navigatorHostId

        val navigatorHost = navigatorHosts[currentNavigatorHostId]
        if (navigatorHost != null) {
            updateOnBackPressedCallback(navigatorHost)
        }
    }

    internal fun registerNavigatorHost(host: NavigatorHost) {
        logEvent("navigatorRegistered", listOf("navigator" to host.navigator.configuration.name))

        if (navigatorHosts[host.id] == null) {
            navigatorHosts[host.id] = host
            listenToDestinationChanges(host)

            if (currentNavigatorHostId == host.id) {
                updateOnBackPressedCallback(host)
            }
        }
    }

    internal fun unregisterNavigatorHost(host: NavigatorHost) {
        logEvent("navigatorUnregistered", listOf("navigator" to host.navigator.configuration.name))
        navigatorHosts.remove(host.id)
    }

    internal fun onNavigatorHostReady(host: NavigatorHost) {
        logEvent("navigatorReady", listOf("navigator" to host.navigator.configuration.name))
        activity.onNavigatorReady(host.navigator)
    }

    /**
     * Finds the registered navigator host associated with the provided resource ID.
     *
     * @param navigatorHostId
     * @return The [NavigatorHost] instance if it's view has been created and it has
     *  been registered with the Activity, otherwise `null`.
     */
    fun navigatorHost(@IdRes navigatorHostId: Int): NavigatorHost? {
        return navigatorHosts[navigatorHostId]
    }

    /**
     * Resets the sessions associated with all registered navigator hosts.
     */
    fun resetSessions() {
        navigatorHosts.forEach { it.value.navigator.session.reset() }
    }

    /**
     * Resets all registered navigators via [Navigator.reset].
     */
    fun resetNavigators() {
        navigatorHosts.forEach { it.value.navigator.reset() }
    }

    private fun listenToDestinationChanges(host: NavigatorHost) {
        host.navController.addOnDestinationChangedListener { controller, _, _ ->
            updateOnBackPressedCallback(host)
        }
    }

    private fun updateOnBackPressedCallback(host: NavigatorHost) {
        if (host.id == currentNavigatorHostId) {
            onBackPressedCallback.isEnabled = host.navController.previousBackStackEntry != null
        }
    }
}
