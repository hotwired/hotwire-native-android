package dev.hotwire.core.turbo.webview

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.AttributeSet
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams.MATCH_PARENT
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.google.gson.GsonBuilder
import dev.hotwire.core.config.Hotwire
import dev.hotwire.core.turbo.util.contentFromAsset
import dev.hotwire.core.turbo.util.runOnUiThread
import dev.hotwire.core.turbo.util.toJson
import dev.hotwire.core.turbo.visit.VisitOptions

/**
 * A Turbo-specific WebView that configures required settings and exposes some helpful info.
 *
 * Generally, you are not creating this view manually — it will be automatically created
 * and available from the Turbo session.
 */
@SuppressLint("SetJavaScriptEnabled")
open class HotwireWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : WebView(context, attrs) {
    private val gson = GsonBuilder().disableHtmlEscaping().create()

    var elementTouchPreventsPullsToRefresh = false
        internal set

    init {
        id = generateViewId()
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.userAgentString = Hotwire.config.userAgentWithWebViewDefault(context)
        settings.setSupportMultipleWindows(true)
        layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        initDayNightTheming()
    }

    /**
     * Provides the WebView's package name (corresponds to Chrome or Android System WebView).
     */
    val packageName: String?
        get() = WebViewCompat.getCurrentWebViewPackage(context)?.packageName

    /**
     * Provides the WebView's version name (corresponds to Chrome or Android System WebView).
     */
    val versionName: String?
        get() = WebViewCompat.getCurrentWebViewPackage(context)?.versionName

    /**
     * Provides the WebView's major version (corresponds to Chrome or Android System WebView).
     */
    val majorVersion: Int?
        get() = versionName?.substringBefore(".")?.toIntOrNull()

    internal fun visitLocation(location: String, options: VisitOptions, restorationIdentifier: String) {
        val args = encodeArguments(location, options.toJson(), restorationIdentifier)
        runJavascript("turboNative.visitLocationWithOptionsAndRestorationIdentifier($args)")
    }

    internal fun visitRenderedForColdBoot(coldBootVisitIdentifier: String) {
        runJavascript("turboNative.visitRenderedForColdBoot('$coldBootVisitIdentifier')")
    }

    internal fun installBridge(onBridgeInstalled: () -> Unit) {
        val script = "window.turboNative == null"
        val bridge = context.contentFromAsset("js/turbo.js")

        runJavascript(script) { s ->
            if (s?.toBoolean() == true) {
                runJavascript(bridge) {
                    onBridgeInstalled()
                }
            }
        }
    }

    private fun WebView.runJavascript(javascript: String, onComplete: (String?) -> Unit = {}) {
        context.runOnUiThread {
            evaluateJavascript(javascript) {
                onComplete(it)
            }
        }
    }

    private fun encodeArguments(vararg args: Any): String? {
        return args.joinToString(",") { gson.toJson(it) }
    }

    @Suppress("DEPRECATION")
    private fun initDayNightTheming() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
                WebSettingsCompat.setForceDarkStrategy(
                    settings,
                    WebSettingsCompat.DARK_STRATEGY_WEB_THEME_DARKENING_ONLY
                )
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
}
