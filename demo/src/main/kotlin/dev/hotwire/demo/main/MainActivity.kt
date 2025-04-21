package dev.hotwire.demo.main

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import com.google.android.material.bottomnavigation.BottomNavigationView
import dev.hotwire.demo.R
import dev.hotwire.navigation.activities.HotwireActivity
import dev.hotwire.navigation.tabs.HotwireBottomNavigationController
import dev.hotwire.navigation.tabs.navigatorConfigurations
import dev.hotwire.navigation.util.applyDefaultImeWindowInsets

class MainActivity : HotwireActivity() {
    private lateinit var bottomNavigationController: HotwireBottomNavigationController

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.root).applyDefaultImeWindowInsets()

        initializeBottomTabs()
    }

    private fun initializeBottomTabs() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_nav)

        bottomNavigationController = HotwireBottomNavigationController(this, bottomNavigationView)
        bottomNavigationController.load(mainTabs)
    }

    override fun navigatorConfigurations() = mainTabs.navigatorConfigurations
}
