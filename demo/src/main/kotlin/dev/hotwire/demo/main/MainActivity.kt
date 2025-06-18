package dev.hotwire.demo.main

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.google.android.material.bottomnavigation.BottomNavigationView
import dev.hotwire.core.turbo.webview.WebViewInfo
import dev.hotwire.core.turbo.webview.WebViewVersionCompatibility
import dev.hotwire.demo.R
import dev.hotwire.navigation.activities.HotwireActivity
import dev.hotwire.navigation.tabs.HotwireBottomNavigationController
import dev.hotwire.navigation.tabs.navigatorConfigurations
import dev.hotwire.navigation.util.applyDefaultImeWindowInsets

class MainActivity : HotwireActivity() {
    private lateinit var bottomNavigationController: HotwireBottomNavigationController
    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.root).applyDefaultImeWindowInsets()

        initializeBottomTabs()

        WebViewVersionCompatibility.displayUpdateDialogIfOutdated(
            activity = this,
            requiredVersion = WebViewInfo.REQUIRED_WEBVIEW_VERSION
        )
    }

    private fun initializeBottomTabs() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_nav)

        bottomNavigationController = HotwireBottomNavigationController(this, bottomNavigationView)
        bottomNavigationController.load(mainTabs, viewModel.selectedTabIndex)
        bottomNavigationController.setOnTabSelectedListener { index, _ ->
            viewModel.selectedTabIndex = index
        }
    }

    override fun navigatorConfigurations() = mainTabs.navigatorConfigurations
}
