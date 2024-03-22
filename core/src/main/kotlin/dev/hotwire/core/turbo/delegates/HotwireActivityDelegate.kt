package dev.hotwire.core.turbo.delegates

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import dev.hotwire.core.turbo.activities.HotwireActivity
import dev.hotwire.core.turbo.activities.SessionConfiguration
import dev.hotwire.core.turbo.nav.HotwireNavDestination
import dev.hotwire.core.turbo.observers.HotwireActivityObserver
import dev.hotwire.core.turbo.session.SessionNavHostFragment
import dev.hotwire.core.turbo.visit.VisitOptions

/**
 * Initializes the Activity for Hotwire navigation and provides all the hooks for an
 * Activity to communicate with Hotwire Native (and vice versa).
 *
 * @property activity The Activity to bind this delegate to.
 * @property currentNavHostFragmentId The resource ID of the [SessionNavHostFragment]
 *  instance hosted in your Activity's layout resource.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class HotwireActivityDelegate(val activity: HotwireActivity) {
    private val appCompatActivity = activity.appCompatActivity
    private val navHostFragments = mutableMapOf<Int, SessionNavHostFragment>()

    private val onBackPressedCallback = object : OnBackPressedCallback(enabled = true) {
        override fun handleOnBackPressed() {
            navigateBack()
        }
    }

    private var currentNavHostFragmentId = activity.sessionConfigurations().first().navHostFragmentId
        set(value) {
            field = value
            updateOnBackPressedCallback(currentNavHostFragment.navController)
        }

    /**
     * Initializes the Activity with a BackPressedDispatcher that properly
     * handles Fragment navigation with the back button.
     */
    init {
        appCompatActivity.lifecycle.addObserver(HotwireActivityObserver())
        appCompatActivity.onBackPressedDispatcher.addCallback(
            owner = appCompatActivity,
            onBackPressedCallback = onBackPressedCallback
        )
    }

    /**
     * Gets the Activity's currently active [SessionNavHostFragment].
     */
    val currentNavHostFragment: SessionNavHostFragment
        get() = navHostFragment(currentNavHostFragmentId)

    /**
     * Gets the currently active Fragment destination hosted in the current
     * [SessionNavHostFragment].
     */
    val currentNavDestination: HotwireNavDestination?
        get() = currentFragment as HotwireNavDestination?

    /**
     * Sets the currently active session in your Activity. If you use multiple
     *  [SessionNavHostFragment] instances in your app (such as for bottom tabs),
     *  you must update this whenever the current session changes.
     */
    fun setCurrentSession(sessionConfiguration: SessionConfiguration) {
        currentNavHostFragmentId = sessionConfiguration.navHostFragmentId
    }

    internal fun registerNavHostFragment(navHostFragment: SessionNavHostFragment) {
        if (navHostFragments[navHostFragment.id] == null) {
            navHostFragments[navHostFragment.id] = navHostFragment
            listenToDestinationChanges(navHostFragment.navController)
        }
    }

    internal fun unregisterNavHostFragment(navHostFragment: SessionNavHostFragment) {
        navHostFragments.remove(navHostFragment.id)
    }

    /**
     * Finds the nav host fragment associated with the provided resource ID.
     *
     * @param navHostFragmentId
     * @return
     */
    fun navHostFragment(@IdRes navHostFragmentId: Int): SessionNavHostFragment {
        return requireNotNull(navHostFragments[navHostFragmentId]) {
            "No registered SessionNavHostFragment found"
        }
    }

    /**
     * Resets the Turbo sessions associated with all registered nav host fragments.
     */
    fun resetSessions() {
        navHostFragments.forEach { it.value.session.reset() }
    }

    /**
     * Resets all registered nav host fragments via [SessionNavHostFragment.reset].
     */
    fun resetNavHostFragments() {
        navHostFragments.forEach { it.value.reset() }
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
        currentNavDestination?.navigate(location, options, bundle)
    }

    /**
     * Navigates up to the previous destination. See [NavController.navigateUp] for
     * more details.
     */
    fun navigateUp() {
        currentNavDestination?.navigateUp()
    }

    /**
     * Navigates back to the previous destination. See [NavController.popBackStack] for
     * more details.
     */
    fun navigateBack() {
        currentNavDestination?.navigateBack()
    }

    /**
     * Clears the navigation back stack to the start destination.
     */
    fun clearBackStack(onCleared: () -> Unit = {}) {
        currentNavDestination?.clearBackStack(onCleared)
    }

    /**
     * Refresh the current destination. See [HotwireNavDestination.refresh] for
     * more details.
     */
    fun refresh(displayProgress: Boolean = true) {
        currentNavDestination?.refresh(displayProgress)
    }

    private fun listenToDestinationChanges(navController: NavController) {
        navController.addOnDestinationChangedListener { controller, _, _ ->
            updateOnBackPressedCallback(controller)
        }
    }

    private fun updateOnBackPressedCallback(navController: NavController) {
        if (navController == currentNavHostFragment.navController)  {
            onBackPressedCallback.isEnabled = navController.previousBackStackEntry != null
        }
    }

    private val currentFragment: Fragment?
        get() {
            return if (currentNavHostFragment.isAdded && !currentNavHostFragment.isDetached) {
                currentNavHostFragment.childFragmentManager.primaryNavigationFragment
            } else {
                null
            }
        }
}
