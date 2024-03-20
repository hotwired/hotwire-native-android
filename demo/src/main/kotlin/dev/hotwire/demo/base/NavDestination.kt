package dev.hotwire.demo.base

import android.view.MenuItem
import androidx.navigation.NavOptions
import androidx.navigation.navOptions
import dev.hotwire.core.turbo.config.TurboPathConfigurationProperties
import dev.hotwire.core.turbo.config.context
import dev.hotwire.core.turbo.nav.TurboNavDestination
import dev.hotwire.core.turbo.nav.TurboNavPresentationContext.MODAL
import dev.hotwire.demo.R

interface NavDestination : TurboNavDestination {


    override fun getNavigationOptions(
        newLocation: String,
        newPathProperties: TurboPathConfigurationProperties
    ): NavOptions {
        return when (newPathProperties.context) {
            MODAL -> slideAnimation()
            else -> super.getNavigationOptions(newLocation, newPathProperties)
        }
    }

    private fun slideAnimation(): NavOptions {
        return navOptions {
            anim {
                enter = R.anim.nav_slide_enter
                exit = R.anim.nav_slide_exit
                popEnter = R.anim.nav_slide_pop_enter
                popExit = R.anim.nav_slide_pop_exit
            }
        }
    }
}
