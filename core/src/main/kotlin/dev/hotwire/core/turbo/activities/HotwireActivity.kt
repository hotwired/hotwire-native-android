package dev.hotwire.core.turbo.activities

import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import dev.hotwire.core.turbo.delegates.HotwireActivityDelegate
import dev.hotwire.core.turbo.session.SessionNavHostFragment

/**
 * Interface that should be implemented by any Activity using Turbo. Ensures that the
 * Activity provides a [HotwireActivityDelegate] so the framework can initialize the
 * [SessionNavHostFragment] hosted in your Activity's layout resource.
 */
interface HotwireActivity {
    val delegate: HotwireActivityDelegate
    val appCompatActivity: AppCompatActivity
    fun sessionConfigurations(): List<SessionConfiguration>
}

data class SessionConfiguration(
    val name: String,
    val startLocation: String,
    @IdRes val navHostFragmentId: Int,
)
