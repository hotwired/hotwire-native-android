package dev.hotwire.demo.main

import dev.hotwire.core.turbo.session.SessionNavHostFragment
import dev.hotwire.demo.Urls

@Suppress("unused")
class MainSessionNavHostFragment : SessionNavHostFragment() {
    override val sessionName = "main"
    override val startLocation = Urls.homeUrl
}
