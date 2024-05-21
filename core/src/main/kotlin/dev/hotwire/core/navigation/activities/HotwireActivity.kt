package dev.hotwire.core.navigation.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dev.hotwire.core.navigation.navigator.NavigatorConfiguration

/**
 * Activity that should be implemented by any Activity using Hotwire.
 */
abstract class HotwireActivity : AppCompatActivity() {
    lateinit var delegate: HotwireActivityDelegate
        private set

    abstract fun navigatorConfigurations(): List<NavigatorConfiguration>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        delegate = HotwireActivityDelegate(this)
    }
}
