package dev.hotwire.navigation.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dev.hotwire.navigation.navigator.Navigator
import dev.hotwire.navigation.navigator.NavigatorConfiguration

/**
 * Activity that should be implemented by any Activity using Hotwire.
 */
abstract class HotwireActivity : AppCompatActivity() {
    lateinit var delegate: HotwireActivityDelegate
        private set

    /**
     * Provide a list of navigator configurations for the Activity. Configurations
     * for all navigator instances available throughout the app should be provided here.
     */
    abstract fun navigatorConfigurations(): List<NavigatorConfiguration>

    /**
     * Called when a navigator has been initialized and is ready for navigation. The
     * root destination for the navigator has already been created.
     */
    open fun onNavigatorReady(navigator: Navigator) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        delegate = HotwireActivityDelegate(this)
    }
}
