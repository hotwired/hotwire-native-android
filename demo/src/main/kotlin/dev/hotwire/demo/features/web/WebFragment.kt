package dev.hotwire.demo.features.web

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import dev.hotwire.core.navigation.fragments.HotwireWebFragment
import dev.hotwire.core.turbo.errors.HttpError
import dev.hotwire.core.turbo.errors.TurboVisitError
import dev.hotwire.core.turbo.nav.TurboNavGraphDestination
import dev.hotwire.core.turbo.visit.TurboVisitAction.REPLACE
import dev.hotwire.core.turbo.visit.TurboVisitOptions
import dev.hotwire.demo.R
import dev.hotwire.demo.base.NavDestination
import dev.hotwire.demo.util.SIGN_IN_URL

@TurboNavGraphDestination(uri = "turbo://fragment/web")
open class WebFragment : HotwireWebFragment(), NavDestination {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenu()
    }

    override fun onFormSubmissionStarted(location: String) {
        menuProgress?.isVisible = true
    }

    override fun onFormSubmissionFinished(location: String) {
        menuProgress?.isVisible = false
    }

    override fun onVisitErrorReceived(location: String, error: TurboVisitError) {
        if (error is HttpError.ClientError.Unauthorized) {
            navigate(SIGN_IN_URL, TurboVisitOptions(action = REPLACE))
        } else {
            super.onVisitErrorReceived(location, error)
        }
    }

    private fun setupMenu() {
        toolbarForNavigation()?.inflateMenu(R.menu.web)
    }

    private val menuProgress: MenuItem?
        get() = toolbarForNavigation()?.menu?.findItem(R.id.menu_progress)
}
