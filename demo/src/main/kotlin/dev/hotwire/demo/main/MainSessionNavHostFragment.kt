package dev.hotwire.demo.main

import dev.hotwire.core.turbo.session.TurboSessionNavHostFragment
import dev.hotwire.demo.Urls

@Suppress("unused")
class MainSessionNavHostFragment : TurboSessionNavHostFragment() {
    override val sessionName = "main"
    override val startLocation = Urls.homeUrl
}
