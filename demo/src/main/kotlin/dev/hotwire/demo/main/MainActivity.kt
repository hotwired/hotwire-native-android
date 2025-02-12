package dev.hotwire.demo.main

import android.os.Bundle
import android.view.View
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import dev.hotwire.demo.R
import dev.hotwire.demo.Urls
import dev.hotwire.navigation.activities.HotwireActivity
import dev.hotwire.navigation.navigator.NavigatorConfiguration

class MainActivity : HotwireActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // handle window insets:
        val rootView = findViewById<View>(R.id.root_view)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val insetTypes = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            insets.getInsets(insetTypes).apply { v.setPadding(left, top, right, bottom) }
            insets
        }
    }

    override fun navigatorConfigurations() = listOf(
        NavigatorConfiguration(
            name = "main",
            startLocation = Urls.homeUrl,
            navigatorHostId = R.id.main_navigator_host
        )
    )
}
