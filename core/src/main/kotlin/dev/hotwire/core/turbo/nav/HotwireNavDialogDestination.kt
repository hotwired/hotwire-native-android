package dev.hotwire.core.turbo.nav

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.IdRes
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
import dev.hotwire.core.navigation.activities.HotwireActivity
import dev.hotwire.core.navigation.routing.Router
import dev.hotwire.core.navigation.session.NavigatorHost
import dev.hotwire.core.turbo.config.PathConfigurationProperties
import dev.hotwire.core.turbo.config.context
import dev.hotwire.core.turbo.delegates.TurboFragmentDelegate
import dev.hotwire.core.turbo.delegates.TurboNestedFragmentDelegate
import dev.hotwire.core.turbo.fragments.TurboFragment
import dev.hotwire.core.turbo.fragments.TurboFragmentViewModel
import dev.hotwire.core.turbo.fragments.TurboWebFragment
import dev.hotwire.core.turbo.session.Session
import dev.hotwire.core.turbo.visit.VisitAction
import dev.hotwire.core.turbo.visit.VisitOptions

/**
 * The interface that a navigable DialogFragment implements to provide the library with
 * the information it needs to properly navigate.
 */
interface HotwireNavDialogDestination {
    fun closeDialog()
}
