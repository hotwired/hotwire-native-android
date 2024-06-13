package dev.hotwire.demo.features.web

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import dev.hotwire.demo.R
import dev.hotwire.navigation.destinations.HotwireDestinationDeepLink
import dev.hotwire.navigation.fragments.HotwireWebBottomSheetFragment

@HotwireDestinationDeepLink(uri = "hotwire://fragment/web/modal/sheet")
class WebBottomSheetFragment : HotwireWebBottomSheetFragment() {
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

    private fun setupMenu() {
        toolbarForNavigation()?.inflateMenu(R.menu.web)
    }

    private val menuProgress: MenuItem?
        get() = toolbarForNavigation()?.menu?.findItem(R.id.menu_progress)
}
