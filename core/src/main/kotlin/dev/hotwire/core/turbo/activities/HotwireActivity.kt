package dev.hotwire.core.turbo.activities

import dev.hotwire.core.turbo.delegates.HotwireActivityDelegate
import dev.hotwire.core.turbo.session.TurboSessionNavHostFragment

/**
 * Interface that should be implemented by any Activity using Turbo. Ensures that the
 * Activity provides a [HotwireActivityDelegate] so the framework can initialize the
 * [TurboSessionNavHostFragment] hosted in your Activity's layout resource.
 */
interface HotwireActivity {
    var delegate: HotwireActivityDelegate
}
