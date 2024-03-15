package dev.hotwire.demo.main

import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import dev.hotwire.core.BuildConfig
import dev.hotwire.core.bridge.KotlinXJsonConverter
import dev.hotwire.core.bridge.Strada
import dev.hotwire.core.turbo.activities.TurboActivity
import dev.hotwire.core.turbo.config.Turbo
import dev.hotwire.core.turbo.delegates.TurboActivityDelegate
import dev.hotwire.demo.R

class MainActivity : AppCompatActivity(), TurboActivity {
    override lateinit var delegate: TurboActivityDelegate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        delegate = TurboActivityDelegate(this, R.id.main_nav_host)
        configApp()
    }

    private fun configApp() {
        Strada.config.jsonConverter = KotlinXJsonConverter()

        if (BuildConfig.DEBUG) {
            Turbo.config.debugLoggingEnabled = true
            Strada.config.debugLoggingEnabled = true
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }
}
