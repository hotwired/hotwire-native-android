package dev.hotwire.core.turbo.delegates

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import dev.hotwire.core.navigation.session.NavigatorHost
import dev.hotwire.core.turbo.nav.HotwireNavDestination
import dev.hotwire.core.turbo.visit.VisitOptions

/**
 * A simplified delegate that can be used when a [NavigatorHost] is nested
 * within a Fragment. This can be useful when you want a portion of the screen to have
 * sub-navigation destinations within the current Fragment.
 *
 * Example: A search screen with a search bar at the top that stays fixed, but search
 * results load in a section of the view below the search bar.
 *
 * @property fragment The Fragment to bind this delegate to.
 * @param navigatorHostId The resource ID of the [NavigatorHost]
 *  instance hosted in your Activity's layout resource.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class TurboNestedFragmentDelegate(val fragment: Fragment, navigatorHostId: Int) {
    val navigatorHost by lazy { findNavigatorHost(navigatorHostId) }

    val currentNavDestination: HotwireNavDestination
        get() = currentFragment as HotwireNavDestination

    /**
     * Resets the navigator via [NavigatorHost.navigator.reset]
     */
    fun resetNavigator() {
        navigatorHost.navigator.reset()
    }

    /**
     * Resets the Turbo session associated with the nav host fragment.
     */
    fun resetSession() {
        navigatorHost.navigator.session.reset()
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
        currentNavDestination.navigate(location, options, bundle)
    }

    /**
     * Navigates up to the previous destination. See [NavController.navigateUp] for
     * more details.
     */
    fun navigateUp() {
        currentNavDestination.navigateUp()
    }

    /**
     * Navigates back to the previous destination. See [NavController.popBackStack] for
     * more details.
     */
    fun navigateBack() {
        currentNavDestination.navigateBack()
    }

    /**
     * Clears the navigation back stack to the start destination.
     */
    fun clearBackStack() {
        currentNavDestination.clearBackStack()
    }

    private val currentFragment: Fragment
        get() = navigatorHost.childFragmentManager.primaryNavigationFragment as Fragment

    private fun findNavigatorHost(@IdRes navHostFragmentId: Int): NavigatorHost {
        return fragment.childFragmentManager.findFragmentById(navHostFragmentId) as? NavigatorHost
            ?: throw IllegalStateException("No NavigatorHost found with ID: $navHostFragmentId")
    }
}
