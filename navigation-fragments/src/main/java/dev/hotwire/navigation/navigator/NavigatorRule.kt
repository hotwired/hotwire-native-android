package dev.hotwire.navigation.navigator

import android.net.Uri
import android.os.Bundle
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.navOptions
import dev.hotwire.core.turbo.config.PathConfiguration
import dev.hotwire.core.turbo.config.context
import dev.hotwire.core.turbo.config.fallbackUri
import dev.hotwire.core.turbo.config.presentation
import dev.hotwire.core.turbo.config.queryStringPresentation
import dev.hotwire.core.turbo.config.uri
import dev.hotwire.core.turbo.nav.Presentation
import dev.hotwire.core.turbo.nav.PresentationContext
import dev.hotwire.core.turbo.nav.QueryStringPresentation
import dev.hotwire.core.turbo.visit.VisitAction
import dev.hotwire.core.turbo.visit.VisitOptions
import dev.hotwire.navigation.config.HotwireNavigation
import dev.hotwire.navigation.destinations.HotwireDestinationDeepLink
import dev.hotwire.navigation.session.SessionModalResult
import dev.hotwire.navigation.util.location

@Suppress("MemberVisibilityCanBePrivate")
internal class NavigatorRule(
    location: String,
    visitOptions: VisitOptions,
    bundle: Bundle?,
    navOptions: NavOptions,
    extras: FragmentNavigator.Extras?,
    pathConfiguration: PathConfiguration,
    val controller: NavController
) {
    val defaultUri = HotwireDestinationDeepLink.from(HotwireNavigation.defaultFragmentDestination).uri.toUri()

    // Current destination
    val previousLocation = controller.previousBackStackEntry.location
    val currentLocation = checkNotNull(controller.currentBackStackEntry.location)
    val currentProperties = pathConfiguration.properties(currentLocation)
    val currentPresentationContext = currentProperties.context
    val isAtStartDestination = controller.previousBackStackEntry == null

    // New destination
    val newLocation = location
    val newProperties = pathConfiguration.properties(newLocation)
    val newPresentationContext = newProperties.context
    val newVisitOptions = visitOptions
    val newBundle = bundle.withNavArguments()
    val newExtras = extras
    val newQueryStringPresentation = newProperties.queryStringPresentation
    val newPresentation = newPresentation()
    val newNavigationMode = newNavigationMode()
    val newModalResult = newModalResult()
    val newDestinationUri = newProperties.uri ?: defaultUri
    val newFallbackUri = newProperties.fallbackUri
    val newDestination = controller.destinationFor(newDestinationUri)
    val newFallbackDestination = controller.destinationFor(newFallbackUri)
    val newNavOptions = newNavOptions(navOptions)

    init {
        verifyNavRules()
    }

    private fun newPresentation(): Presentation {
        // Check if we should use the custom presentation provided in the path configuration
        if (newProperties.presentation != Presentation.DEFAULT) {
            return if (isAtStartDestination && newProperties.presentation == Presentation.POP) {
                // You cannot pop from the start destination, prevent visit
                Presentation.NONE
            } else {
                // Use the custom presentation
                newProperties.presentation
            }
        }

        val locationIsCurrent = locationsAreSame(newLocation, currentLocation)
        val locationIsPrevious = locationsAreSame(newLocation, previousLocation)
        val replace = newVisitOptions.action == VisitAction.REPLACE

        return when {
            locationIsCurrent && isAtStartDestination -> Presentation.REPLACE_ROOT
            locationIsPrevious -> Presentation.POP
            locationIsCurrent || replace -> Presentation.REPLACE
            else -> Presentation.PUSH
        }
    }

    private fun newNavOptions(navOptions: NavOptions): NavOptions {
        // Use separate NavOptions if we need to pop up to the new root destination
        if (newPresentation == Presentation.REPLACE_ROOT && newDestination != null) {
            return navOptions {
                popUpTo(controller.graph.id) { inclusive = true }
                anim {
                    enter = navOptions.enterAnim
                    exit = navOptions.exitAnim
                    popEnter = navOptions.popEnterAnim
                    popExit = navOptions.popExitAnim
                }
            }
        }

        return navOptions
    }

    private fun newNavigationMode(): NavigatorMode {
        val presentationNone = newPresentation == Presentation.NONE
        val presentationRefresh = newPresentation == Presentation.REFRESH

        val dismissModalContext = currentPresentationContext == PresentationContext.MODAL &&
                newPresentationContext == PresentationContext.DEFAULT &&
                newPresentation != Presentation.REPLACE_ROOT

        val navigateToModalContext = currentPresentationContext == PresentationContext.DEFAULT &&
                newPresentationContext == PresentationContext.MODAL &&
                newPresentation != Presentation.REPLACE_ROOT

        return when {
            dismissModalContext -> NavigatorMode.DISMISS_MODAL
            navigateToModalContext -> NavigatorMode.TO_MODAL
            presentationRefresh -> NavigatorMode.REFRESH
            presentationNone -> NavigatorMode.NONE
            else -> NavigatorMode.IN_CONTEXT
        }
    }

    private fun newModalResult(): SessionModalResult? {
        if (newNavigationMode != NavigatorMode.DISMISS_MODAL) {
            return null
        }

        return SessionModalResult(
            location = newLocation,
            options = newVisitOptions,
            bundle = newBundle,
            shouldNavigate = newProperties.presentation != Presentation.NONE
        )
    }

    private fun verifyNavRules() {
        if (newPresentationContext == PresentationContext.MODAL &&
            newPresentation == Presentation.REPLACE_ROOT
        ) {
            throw NavigatorException("A `modal` destination cannot use presentation `REPLACE_ROOT`")
        }
    }

    private fun NavController.destinationFor(uri: Uri?): NavDestination? {
        uri ?: return null
        return graph.find { it.hasDeepLink(uri) }
    }

    private fun Bundle?.withNavArguments(): Bundle {
        val bundle = this ?: bundleOf()
        return bundle.apply {
            putString("location", newLocation)
            putSerializable("presentation-context", newPresentationContext)
        }
    }

    private fun locationsAreSame(first: String?, second: String?): Boolean {
        if (first == null || second == null) {
            return false
        }

        val firstUri = Uri.parse(first)
        val secondUri = Uri.parse(second)

        return when (newQueryStringPresentation) {
            QueryStringPresentation.REPLACE -> {
                firstUri.path == secondUri.path
            }
            QueryStringPresentation.DEFAULT -> {
                firstUri.path == secondUri.path && firstUri.query == secondUri.query
            }
        }
    }
}
