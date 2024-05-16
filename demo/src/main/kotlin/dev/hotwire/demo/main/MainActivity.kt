package dev.hotwire.demo.main

import android.os.Bundle
import dev.hotwire.core.navigation.activities.HotwireActivity
import dev.hotwire.core.navigation.session.SessionConfiguration
import dev.hotwire.demo.R
import dev.hotwire.demo.Urls

class MainActivity : HotwireActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun sessionConfigurations() = listOf(
        SessionConfiguration(
            name = "main",
            startLocation = Urls.homeUrl,
            navHostFragmentId = R.id.main_nav_host
        )
    )
}
