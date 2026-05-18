package dev.hotwire.navigation.tabs

import androidx.annotation.DrawableRes
import dev.hotwire.navigation.navigator.NavigatorConfiguration

/**
 * Represents a tab used by the [HotwireNavigationController].
 *
 * @param itemId The [com.google.android.material.navigation.NavigationBarView]'s
 *  menu item ID for the corresponding tab.
 *  @param configuration The [NavigatorConfiguration] for the tab.
 */
data class HotwireTab(
    val title: String,
    @DrawableRes val iconResId: Int,
    val isVisible: Boolean = true,
    val configuration: NavigatorConfiguration
)

/**
 * Maps the tabs to a list of their navigator configurations.
 */
val List<HotwireTab>.navigatorConfigurations
    get() = map { it.configuration }
