package dev.hotwire.navigation.destinations

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.navOptions
import dev.hotwire.core.R
import dev.hotwire.core.turbo.config.PathConfigurationProperties
import dev.hotwire.core.turbo.config.context
import dev.hotwire.core.turbo.nav.TurboNavPresentationContext
import dev.hotwire.core.turbo.visit.VisitAction
import dev.hotwire.navigation.config.Hotwire
import dev.hotwire.navigation.fragments.HotwireFragmentDelegate
import dev.hotwire.navigation.fragments.HotwireFragmentViewModel
import dev.hotwire.navigation.navigator.Navigator
import dev.hotwire.navigation.routing.Router

/**
 * The primary interface that a navigable Fragment implements to provide the library with
 * the information it needs to properly navigate.
 */
interface HotwireNavDestination {
    /**
     * Gets the navigator instance associated with this destination.
     */
    val navigator: Navigator

    /**
     * Gets the fragment instance for this destination.
     */
    val fragment: Fragment
        get() = this as Fragment

    /**
     * Gets the location for this destination.
     */
    val location: String
        get() = requireNotNull(fragment.arguments?.location)

    /**
     * Gets the path configuration properties for the location associated with this
     * destination.
     */
    val pathProperties: PathConfigurationProperties
        get() = Hotwire.config.pathConfiguration.properties(location)

    /**
     * Gets the [HotwireFragmentViewModel] associated with this destination.
     */
    val fragmentViewModel: HotwireFragmentViewModel
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
    fun delegate(): HotwireFragmentDelegate

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
     * Refresh the destination's contents.
     *
     * @param displayProgress Whether progress should be displayed while refreshing.
     */
    fun refresh(displayProgress: Boolean = true)

    /**
     * Gets the navigator that will be used for navigating to `newLocation`. You should
     * not have to override this, unless you're using a [NestedNavigatorHostDelegate] to provide
     * sub-navigation within your current Fragment destination and would like custom behavior.
     */
    fun navigatorForNavigation(newLocation: String): Navigator {
        return navigator
    }

    /**
     * Determines whether the new location should be routed within in-app navigation from the
     * current destination. By default, the registered [Router.RouteDecisionHandler] instances are used to
     * determine routing logic. You can override the global behavior for a specific destination,
     * but it's recommend to use dedicated [Router.RouteDecisionHandler] instances for routing logic.
     */
    fun decideRoute(newLocation: String): Router.Decision {
        return Hotwire.router.decideRoute(
            location = newLocation,
            configuration = navigator.configuration,
            activity = fragment.requireActivity() as dev.hotwire.navigation.activities.HotwireActivity
        )
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

    fun prepareNavigation(onReady: () -> Unit)

    private val Bundle.location
        get() = getString("location")
}
