package dev.hotwire.demo.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.webkit.WebView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import dev.hotwire.core.turbo.config.TurboPathConfigurationProperties

val TurboPathConfigurationProperties.description: String?
    get() = get("description")

@Suppress("DEPRECATION")
fun WebView.initDayNightTheme() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
            WebSettingsCompat.setForceDarkStrategy(settings, WebSettingsCompat.DARK_STRATEGY_WEB_THEME_DARKENING_ONLY)
        }

        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            when (isNightModeEnabled(context)) {
                true -> WebSettingsCompat.setForceDark(settings, WebSettingsCompat.FORCE_DARK_ON)
                else -> WebSettingsCompat.setForceDark(settings, WebSettingsCompat.FORCE_DARK_AUTO)
            }
        }
    }
}

private fun isNightModeEnabled(context: Context): Boolean {
    val currentNightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return currentNightMode == Configuration.UI_MODE_NIGHT_YES
}
