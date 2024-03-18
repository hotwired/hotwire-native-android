package dev.hotwire.demo.main

import androidx.fragment.app.Fragment
import dev.hotwire.core.bridge.Bridge
import dev.hotwire.core.turbo.config.TurboPathConfiguration
import dev.hotwire.core.turbo.session.TurboSessionNavHostFragment
import dev.hotwire.demo.features.imageviewer.ImageViewerFragment
import dev.hotwire.demo.features.numbers.NumberBottomSheetFragment
import dev.hotwire.demo.features.numbers.NumbersFragment
import dev.hotwire.demo.features.web.WebBottomSheetFragment
import dev.hotwire.demo.features.web.WebFragment
import dev.hotwire.demo.features.web.WebHomeFragment
import dev.hotwire.demo.features.web.WebModalFragment
import dev.hotwire.demo.util.HOME_URL
import dev.hotwire.demo.util.initDayNightTheme
import kotlin.reflect.KClass

@Suppress("unused")
class MainSessionNavHostFragment : TurboSessionNavHostFragment() {
    override val sessionName = "main"

    override val startLocation = HOME_URL

    override val registeredFragments: List<KClass<out Fragment>>
        get() = listOf(
            WebFragment::class,
            WebHomeFragment::class,
            WebModalFragment::class,
            WebBottomSheetFragment::class,
            NumbersFragment::class,
            NumberBottomSheetFragment::class,
            ImageViewerFragment::class
        )

    override val pathConfigurationLocation: TurboPathConfiguration.Location
        get() = TurboPathConfiguration.Location(
            assetFilePath = "json/configuration.json"
        )

    override fun onSessionCreated() {
        super.onSessionCreated()

        // Configure WebView
        session.webView.initDayNightTheme()

        // Initialize Strada bridge with new WebView instance
        Bridge.initialize(session.webView)
    }
}
