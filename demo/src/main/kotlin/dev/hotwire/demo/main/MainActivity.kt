package dev.hotwire.demo.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dev.hotwire.core.turbo.activities.HotwireActivity
import dev.hotwire.core.turbo.delegates.HotwireActivityDelegate
import dev.hotwire.demo.R

class MainActivity : AppCompatActivity(), HotwireActivity {
    override lateinit var delegate: HotwireActivityDelegate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        delegate = HotwireActivityDelegate(this, R.id.main_nav_host)
    }
}
