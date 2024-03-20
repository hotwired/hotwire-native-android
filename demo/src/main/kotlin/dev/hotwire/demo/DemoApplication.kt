package dev.hotwire.demo

import android.app.Application
import dev.hotwire.core.BuildConfig
import dev.hotwire.core.bridge.BridgeComponentFactory
import dev.hotwire.core.bridge.KotlinXJsonConverter
import dev.hotwire.core.config.Hotwire
import dev.hotwire.core.navigation.routing.BrowserTabRoute
import dev.hotwire.core.navigation.routing.AppNavigationRoute
import dev.hotwire.core.turbo.config.TurboPathConfiguration
import dev.hotwire.demo.bridge.FormComponent
import dev.hotwire.demo.bridge.MenuComponent
import dev.hotwire.demo.bridge.OverflowMenuComponent
import dev.hotwire.demo.features.imageviewer.ImageViewerFragment
import dev.hotwire.demo.features.numbers.NumberBottomSheetFragment
import dev.hotwire.demo.features.numbers.NumbersFragment
import dev.hotwire.demo.features.web.WebBottomSheetFragment
import dev.hotwire.demo.features.web.WebFragment
import dev.hotwire.demo.features.web.WebHomeFragment
import dev.hotwire.demo.features.web.WebModalFragment
import dev.hotwire.demo.util.BASE_URL

class DemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        configureApp()
    }

    private fun configureApp() {
        // Configure debugging
        Hotwire.config.debugLoggingEnabled = BuildConfig.DEBUG
        Hotwire.config.webViewDebuggingEnabled = BuildConfig.DEBUG

        // Set app url
        Hotwire.appUrl = BASE_URL

        // Loads the path configuration
        Hotwire.loadPathConfiguration(
            context = this,
            location = TurboPathConfiguration.Location(
                assetFilePath = "json/configuration.json"
            )
        )

        // Register fragment destinations
        Hotwire.registerFragmentDestinations(listOf(
            WebFragment::class,
            WebHomeFragment::class,
            WebModalFragment::class,
            WebBottomSheetFragment::class,
            NumbersFragment::class,
            NumberBottomSheetFragment::class,
            ImageViewerFragment::class
        ))

        // Register bridge components
        Hotwire.registerBridgeComponents(listOf(
            BridgeComponentFactory("form", ::FormComponent),
            BridgeComponentFactory("menu", ::MenuComponent),
            BridgeComponentFactory("overflow-menu", ::OverflowMenuComponent)
        ))

        // Register routes
        Hotwire.registerRoutes(listOf(
            AppNavigationRoute(),
            BrowserTabRoute()
        ))

        // Set configuration options
        Hotwire.config.jsonConverter = KotlinXJsonConverter()
        Hotwire.config.userAgent = "Hotwire Demo; ${Hotwire.config.userAgentSubstring()}"
    }
}