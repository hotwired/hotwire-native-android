package dev.hotwire.demo

import android.app.Application
import dev.hotwire.core.BuildConfig
import dev.hotwire.core.bridge.BridgeComponentFactory
import dev.hotwire.core.bridge.KotlinXJsonConverter
import dev.hotwire.core.config.Hotwire
import dev.hotwire.core.turbo.config.PathConfiguration
import dev.hotwire.demo.bridge.FormComponent
import dev.hotwire.demo.bridge.MenuComponent
import dev.hotwire.demo.bridge.OverflowMenuComponent
import dev.hotwire.demo.features.imageviewer.ImageViewerFragment
import dev.hotwire.demo.features.numbers.NumbersFragment
import dev.hotwire.demo.features.web.WebBottomSheetFragment
import dev.hotwire.demo.features.web.WebFragment
import dev.hotwire.navigation.config.defaultFragmentDestination
import dev.hotwire.navigation.config.registerBridgeComponents
import dev.hotwire.navigation.config.registerFragmentDestinations

class DemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        configureApp()
    }

    private fun configureApp() {
        // Loads the path configuration
        Hotwire.loadPathConfiguration(
            context = this,
            location = PathConfiguration.Location(
                assetFilePath = "json/path-configuration.json",
                remoteFileUrl = "${Demo.current.url}/configurations/android_v1.json"
            )
        )

        // Set the default fragment destination
        Hotwire.defaultFragmentDestination = WebFragment::class

        // Register fragment destinations
        Hotwire.registerFragmentDestinations(
            WebFragment::class,
            WebBottomSheetFragment::class,
            NumbersFragment::class,
            ImageViewerFragment::class
        )

        // Register bridge components
        Hotwire.registerBridgeComponents(
            BridgeComponentFactory("form", ::FormComponent),
            BridgeComponentFactory("menu", ::MenuComponent),
            BridgeComponentFactory("overflow-menu", ::OverflowMenuComponent)
        )

        // Set configuration options
        Hotwire.config.debugLoggingEnabled = BuildConfig.DEBUG
        Hotwire.config.webViewDebuggingEnabled = BuildConfig.DEBUG
        Hotwire.config.jsonConverter = KotlinXJsonConverter()
        Hotwire.config.applicationUserAgentPrefix = "Hotwire Demo;"
    }
}
