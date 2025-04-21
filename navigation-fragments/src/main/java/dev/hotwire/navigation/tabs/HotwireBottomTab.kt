package dev.hotwire.navigation.tabs

import androidx.annotation.IdRes
import dev.hotwire.navigation.navigator.NavigatorConfiguration

/**
 * Represents a bottom tab used by the [HotwireBottomNavigationController].
 *
 * @param itemId The [com.google.android.material.bottomnavigation.BottomNavigationView]'s
 *  menu item ID for the corresponding tab.
 *  @param configuration The [NavigatorConfiguration] for the tab.
 */
data class HotwireBottomTab(
    @IdRes val itemId: Int,
    val isVisible: Boolean = true,
    val configuration: NavigatorConfiguration
)

/**
 * Maps the tabs to a list of their navigator configurations.
 */
val List<HotwireBottomTab>.navigatorConfigurations
    get() = map { it.configuration }
