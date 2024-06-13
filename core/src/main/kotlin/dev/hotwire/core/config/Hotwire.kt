package dev.hotwire.core.config

import android.content.Context
import dev.hotwire.core.turbo.config.PathConfiguration

object Hotwire {
    val config: HotwireConfig = HotwireConfig()

    /**
     * Loads the [PathConfiguration] JSON file(s) from the provided location to
     * configure navigation rules.
     */
    fun loadPathConfiguration(context: Context, location: PathConfiguration.Location) {
        config.pathConfiguration.load(context, location)
    }
}
