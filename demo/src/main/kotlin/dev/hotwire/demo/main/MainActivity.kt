package dev.hotwire.demo.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dev.hotwire.core.navigation.activities.HotwireActivity
import dev.hotwire.core.navigation.activities.HotwireActivityDelegate
import dev.hotwire.core.navigation.config.SessionConfiguration
import dev.hotwire.demo.R
import dev.hotwire.demo.Urls

class MainActivity : AppCompatActivity(), HotwireActivity {
    override val delegate by lazy { HotwireActivityDelegate(this) }
    override val appCompatActivity = this

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
