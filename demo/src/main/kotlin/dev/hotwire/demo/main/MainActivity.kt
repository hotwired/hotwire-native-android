package dev.hotwire.demo.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dev.hotwire.core.turbo.activities.TurboActivity
import dev.hotwire.core.turbo.delegates.TurboActivityDelegate
import dev.hotwire.demo.R

class MainActivity : AppCompatActivity(), TurboActivity {
    override lateinit var delegate: TurboActivityDelegate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        delegate = TurboActivityDelegate(this, R.id.main_nav_host)
    }
}
