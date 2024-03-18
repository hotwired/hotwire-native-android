package dev.hotwire.demo.features.web

import android.os.Bundle
import android.view.View
import dev.hotwire.core.navigation.fragments.HotwireWebBottomSheetFragment
import dev.hotwire.core.turbo.fragments.TurboWebBottomSheetDialogFragment
import dev.hotwire.core.turbo.nav.TurboNavGraphDestination
import dev.hotwire.demo.R
import dev.hotwire.demo.base.NavDestination

@TurboNavGraphDestination(uri = "turbo://fragment/web/modal/sheet")
class WebBottomSheetFragment : HotwireWebBottomSheetFragment(), NavDestination {
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
}
