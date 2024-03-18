package dev.hotwire.demo.main

import dev.hotwire.core.turbo.session.TurboSessionNavHostFragment
import dev.hotwire.demo.util.HOME_URL
import dev.hotwire.demo.util.initDayNightTheme

@Suppress("unused")
class MainSessionNavHostFragment : TurboSessionNavHostFragment() {
    override val sessionName = "main"

    override val startLocation = HOME_URL

    override fun onSessionCreated() {
        super.onSessionCreated()

        // Configure WebView
        session.webView.initDayNightTheme()
    }
}
