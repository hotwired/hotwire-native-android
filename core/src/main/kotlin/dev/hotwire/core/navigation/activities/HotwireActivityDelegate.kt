package dev.hotwire.core.navigation.activities

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IdRes
import androidx.navigation.NavController
import dev.hotwire.core.navigation.session.NavigatorHost
import dev.hotwire.core.navigation.session.SessionConfiguration
import dev.hotwire.core.turbo.nav.HotwireNavDestination
import dev.hotwire.core.turbo.nav.Navigator
import dev.hotwire.core.turbo.observers.HotwireActivityObserver
import dev.hotwire.core.turbo.visit.VisitOptions

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
            navigateBack()
        }
    }

    private var currentNavigatorHostId = activity.sessionConfigurations().first().navHostFragmentId
        set(value) {
            field = value
            updateOnBackPressedCallback(currentNavigatorHost.navController)
        }

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
     */
    val currentNavigator: Navigator?
        get() {
            return if (currentNavigatorHost.isAdded && !currentNavigatorHost.isDetached) {
                currentNavigatorHost.navigator
            } else {
                null
            }
        }

    /**
     * Sets the currently active session in your Activity. If you use multiple
     *  [NavigatorHost] instances in your app (such as for bottom tabs),
     *  you must update this whenever the current session changes.
     */
    fun setCurrentSession(sessionConfiguration: SessionConfiguration) {
        currentNavigatorHostId = sessionConfiguration.navHostFragmentId
    }

    internal fun registerNavigatorHost(host: NavigatorHost) {
        if (navigatorHosts[host.id] == null) {
            navigatorHosts[host.id] = host
            listenToDestinationChanges(host.navController)
        }
    }

    internal fun unregisterNavigatorHost(host: NavigatorHost) {
        navigatorHosts.remove(host.id)
    }

    /**
     * Finds the navigator host associated with the provided resource ID.
     *
     * @param navigatorHostId
     * @return
     */
    fun navigatorHost(@IdRes navigatorHostId: Int): NavigatorHost {
        return requireNotNull(navigatorHosts[navigatorHostId]) {
            "No registered NavigatorHost found"
        }
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

    /**
     * Navigates to the specified location. The resulting destination and its presentation
     * will be determined using the path configuration rules.
     *
     * @param location The location to navigate to.
     * @param options Visit options to apply to the visit. (optional)
     * @param bundle Bundled arguments to pass to the destination. (optional)
     */
    fun navigate(
        location: String,
        options: VisitOptions = VisitOptions(),
        bundle: Bundle? = null
    ) {
        currentNavigator?.navigate(location, options, bundle)
    }

    /**
     * Navigates up to the previous destination. See [NavController.navigateUp] for
     * more details.
     */
    fun navigateUp() {
        currentNavigator?.navigateUp()
    }

    /**
     * Navigates back to the previous destination. See [NavController.popBackStack] for
     * more details.
     */
    fun navigateBack() {
        currentNavigator?.navigateBack()
    }

    /**
     * Clears the navigation back stack to the start destination.
     */
    fun clearBackStack(onCleared: () -> Unit = {}) {
        currentNavigator?.clearBackStack(onCleared)
    }

    /**
     * Refresh the current destination. See [HotwireNavDestination.refresh] for
     * more details.
     */
    fun refresh(displayProgress: Boolean = true) {
        currentNavigator?.currentDestination?.refresh(displayProgress)
    }

    private fun listenToDestinationChanges(navController: NavController) {
        navController.addOnDestinationChangedListener { controller, _, _ ->
            updateOnBackPressedCallback(controller)
        }
    }

    private fun updateOnBackPressedCallback(navController: NavController) {
        if (navController == currentNavigatorHost.navController)  {
            onBackPressedCallback.isEnabled = navController.previousBackStackEntry != null
        }
    }

    private val currentNavigatorHost: NavigatorHost
        get() = navigatorHost(currentNavigatorHostId)
}
