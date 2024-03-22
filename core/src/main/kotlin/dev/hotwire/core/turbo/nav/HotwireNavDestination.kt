package dev.hotwire.core.turbo.nav

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import dev.hotwire.core.R
import dev.hotwire.core.config.Hotwire
import dev.hotwire.core.config.Hotwire.pathConfiguration
import dev.hotwire.core.navigation.routing.Router
import dev.hotwire.core.turbo.activities.HotwireActivity
import dev.hotwire.core.turbo.config.PathConfigurationProperties
import dev.hotwire.core.turbo.config.context
import dev.hotwire.core.turbo.delegates.TurboFragmentDelegate
import dev.hotwire.core.turbo.delegates.TurboNestedFragmentDelegate
import dev.hotwire.core.turbo.fragments.TurboFragment
import dev.hotwire.core.turbo.fragments.TurboFragmentViewModel
import dev.hotwire.core.turbo.fragments.TurboWebFragment
import dev.hotwire.core.turbo.session.Session
import dev.hotwire.core.turbo.session.SessionNavHostFragment
import dev.hotwire.core.turbo.visit.VisitAction
import dev.hotwire.core.turbo.visit.VisitOptions

/**
 * The primary interface that a navigable Fragment implements to provide the library with
 * the information it needs to properly navigate.
 */
interface HotwireNavDestination {
    /**
     * Gets the fragment instance for this destination.
     */
    val fragment: Fragment
        get() = this as Fragment

    /**
     * Gets the Turbo session's nav host fragment associated with this destination.
     */
    val sessionNavHostFragment: SessionNavHostFragment
        get() = fragment.parentFragment as SessionNavHostFragment

    /**
     * Gets the location for this destination.
     */
    val location: String
        get() = requireNotNull(fragment.arguments?.location)

    /**
     * Gets the previous back stack entry's location from the nav controller.
     */
    val previousLocation: String?
        get() = navController()?.previousBackStackEntry?.arguments?.location

    /**
     * Gets the path configuration properties for the location associated with this
     * destination.
     */
    val pathProperties: PathConfigurationProperties
        get() = pathConfiguration.properties(location)

    /**
     * Gets the [Session] associated with this destination.
     */
    val session: Session
        get() = sessionNavHostFragment.session

    /**
     * Gets the [TurboFragmentViewModel] associated with this destination.
     */
    val fragmentViewModel: TurboFragmentViewModel
        get() = delegate().fragmentViewModel

    /**
     * Specifies whether the destination fragment is currently active and
     * added to its parent activity.
     */
    val isActive: Boolean
        get() = fragment.isAdded && !fragment.isDetached

    /**
     * Specifies whether the destination was presented in a modal context.
     */
    val isModal: Boolean
        get() = pathProperties.context == TurboNavPresentationContext.MODAL

    /**
     * Gets the delegate instance that handles the Fragment's lifecycle events.
     */
    fun delegate(): TurboFragmentDelegate

    /**
     * Returns the [Toolbar] used for navigation by the given view.
     */
    fun toolbarForNavigation(): Toolbar?

    /**
     * Specifies whether title changes should be automatically observed and update
     * the title in the Toolbar provided from toolbarForNavigation(), if available.
     * Default is true.
     */
    fun shouldObserveTitleChanges(): Boolean {
        return true
    }

    /**
     * Called before any navigation action takes places. This is a useful place
     * for state cleanup in your Fragment if necessary.
     */
    fun onBeforeNavigation()

    /**
     * Refresh the destination's contents. In a [TurboWebFragment], this will perform
     * a cold boot reload of the WebView location. In an all-native [TurboFragment]
     * each subclass is responsible for implementing how to refresh its contents.
     *
     * @param displayProgress Whether progress should be displayed while refreshing.
     */
    fun refresh(displayProgress: Boolean = true)

    /**
     * Gets the nav host fragment that will be used for navigating to `newLocation`. You should
     * not have to override this, unless you're using a [TurboNestedFragmentDelegate] to provide
     * sub-navigation within your current Fragment destination and would like custom behavior.
     */
    fun navHostForNavigation(newLocation: String): SessionNavHostFragment {
        return sessionNavHostFragment
    }

    /**
     * Determines whether the new location should be routed within in-app navigation from the
     * current destination. By default, the registered [Router.Route] instances are used to
     * determine routing logic. You can override the global behavior for a specific destination,
     * but it's recommend to use dedicated [Router.Route] instances for routing logic.
     */
    fun route(newLocation: String): Router.RouteResult {
        return Hotwire.router.route(
            location = newLocation,
            sessionConfiguration = sessionNavHostFragment.sessionConfiguration,
            activity = fragment.requireActivity() as HotwireActivity
        )
    }

    /**
     * Navigates to the specified location. The resulting destination and its presentation
     * will be determined using the path configuration rules.
     *
     * @param location The location to navigate to.
     * @param options Visit options to apply to the visit. (optional)
     * @param bundle Bundled arguments to pass to the destination. (optional)
     * @param extras Extras that can be passed to enable Fragment specific behavior. (optional)
     */
    fun navigate(
        location: String,
        options: VisitOptions = VisitOptions(),
        bundle: Bundle? = null,
        extras: FragmentNavigator.Extras? = null
    ) {
        navigator.navigate(location, options, bundle, extras)
    }

    /**
     * Gets the default set of navigation options (basic enter/exit animations) for the Android
     * Navigation component to use to execute a navigation event. This can be overridden if
     * you'd like to provide your own.
     */
    fun getNavigationOptions(
        newLocation: String,
        newPathProperties: PathConfigurationProperties,
        action: VisitAction
    ): NavOptions {
        val modal = newPathProperties.context == TurboNavPresentationContext.MODAL
        val replace = action == VisitAction.REPLACE

        return if (modal) {
            navOptions {
                anim {
                    enter = if (replace) 0 else R.anim.enter_slide_in_bottom
                    exit = R.anim.exit_slide_out_bottom
                    popEnter = R.anim.enter_slide_in_bottom
                    popExit = R.anim.exit_slide_out_bottom
                }
            }
        } else {
            navOptions {
                anim {
                    enter = if (replace) 0 else R.anim.enter_slide_in_right
                    exit = R.anim.exit_slide_out_left
                    popEnter = R.anim.enter_slide_in_left
                    popExit = R.anim.exit_slide_out_right
                }
            }
        }
    }

    /**
     * Navigates up to the previous destination. See [NavController.navigateUp] for
     * more details.
     */
    fun navigateUp() {
        navigator.navigateUp()
    }

    /**
     * Navigates back to the previous destination. See [NavController.popBackStack] for
     * more details.
     */
    fun navigateBack() {
        navigator.navigateBack()
    }

    /**
     * Clears the navigation back stack to the start destination.
     */
    fun clearBackStack(onCleared: () -> Unit = {}) {
        navigator.clearBackStack(onCleared)
    }

    /**
     * Gets a registered activity result launcher instance for the given `requestCode`.
     *
     * Override to provide your own [androidx.activity.result.ActivityResultLauncher]
     * instances. If your app doesn't have a matching `requestCode`, you must call
     * `super.activityResultLauncher(requestCode)` to give the Turbo library an
     * opportunity to provide a matching result launcher.
     *
     * @param requestCode The request code for the corresponding result launcher.
     */
    fun activityResultLauncher(requestCode: Int): ActivityResultLauncher<Intent>? {
        return null
    }

    /**
     * Finds the nav host fragment with the given resource ID.
     */
    fun findNavHostFragment(@IdRes navHostFragmentId: Int): SessionNavHostFragment {
        return fragment.parentFragment?.childFragmentManager?.findNavHostFragment(navHostFragmentId)
            ?: fragment.parentFragment?.parentFragment?.childFragmentManager?.findNavHostFragment(navHostFragmentId)
            ?: fragment.requireActivity().supportFragmentManager.findNavHostFragment(navHostFragmentId)
            ?: throw IllegalStateException("No SessionNavHostFragment found with ID: $navHostFragmentId")
    }

    private val Bundle.location
        get() = getString("location")

    private val navigator: TurboNavigator
        get() = delegate().navigator

    /**
     * Retrieve the nav controller indirectly from the parent NavHostFragment,
     * since it's only available when the fragment is attached to its parent.
     */
    private fun navController(): NavController? {
        return fragment.parentFragment?.findNavController()
    }

    private fun FragmentManager.findNavHostFragment(navHostFragmentId: Int): SessionNavHostFragment? {
        return findFragmentById(navHostFragmentId) as? SessionNavHostFragment
    }
}
