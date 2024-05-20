package dev.hotwire.core.turbo.delegates

import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import dev.hotwire.core.navigation.session.NavigatorHost
import dev.hotwire.core.turbo.nav.Navigator

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

    val navigator: Navigator
        get() = navigatorHost.navigator

    private fun findNavigatorHost(@IdRes navHostFragmentId: Int): NavigatorHost {
        return fragment.childFragmentManager.findFragmentById(navHostFragmentId) as? NavigatorHost
            ?: throw IllegalStateException("No NavigatorHost found with ID: $navHostFragmentId")
    }
}
