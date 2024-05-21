package dev.hotwire.demo.main

import android.os.Bundle
import dev.hotwire.core.navigation.activities.HotwireActivity
import dev.hotwire.core.navigation.navigator.NavigatorConfiguration
import dev.hotwire.demo.R
import dev.hotwire.demo.Urls

class MainActivity : HotwireActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun navigatorConfigurations() = listOf(
        NavigatorConfiguration(
            name = "main",
            startLocation = Urls.homeUrl,
            navigatorHostId = R.id.main_navigator_host
        )
    )
}
