package dev.hotwire.core.turbo.webview

import android.content.Context
import android.content.pm.PackageInfo
import android.net.Uri
import android.webkit.WebSettings
import androidx.webkit.WebViewCompat

private const val PACKAGE_SYSTEM_WEBVIEW = "com.google.android.webview"
private const val PACKAGE_CHROME_WEBVIEW = "com.android.chrome"
private const val PACKAGE_CHROME_PRE_RELEASE_WEBVIEW = "com.chrome"

class WebViewInfo internal constructor(context: Context) {
    enum class WebViewType {
        ANDROID_SYSTEM,
        CHROME,
        UNKNOWN
    }

    /**
     * The system WebView's package info (corresponds to Chrome or Android System WebView).
     */
    val packageInfo: PackageInfo? = WebViewCompat.getCurrentWebViewPackage(context)

    /**
     * The system WebView's major version (corresponds to Chrome or Android System WebView).
     * Returns null if the major version cannot be determined.
     */
    val majorVersion: Int? = packageInfo?.versionName?.substringBefore(".")?.toIntOrNull()

    /**
     * The default User-Agent provided by the system WebView before a call is made
     * to WebView.settings.setUserAgentString(String).
     */
    val defaultUserAgent: String = WebSettings.getDefaultUserAgent(context)

    /**
     * The system WebView's origin type. Different OS versions have the WebView component
     * backed by either Google Chrome or the Android System WebView component. These are updatable
     * through the Play Store.
     */
    val webViewType = when {
        packageInfo?.packageName?.contains(PACKAGE_CHROME_WEBVIEW) == true -> WebViewType.CHROME
        packageInfo?.packageName?.contains(PACKAGE_CHROME_PRE_RELEASE_WEBVIEW) == true -> WebViewType.CHROME
        packageInfo?.packageName?.contains(PACKAGE_SYSTEM_WEBVIEW) == true -> WebViewType.ANDROID_SYSTEM
        else -> WebViewType.UNKNOWN
    }

    /**
     * The system WebView's origin type as a human readable string.
     */
    val webViewTypeName = when (webViewType) {
        WebViewType.ANDROID_SYSTEM -> "Android System WebView"
        WebViewType.CHROME -> "Google Chrome"
        WebViewType.UNKNOWN -> "Unknown"
    }

    /**
     * The Play Store app Uri for the system WebView type. This is useful to point users to the
     * Play Store if their WebView version is outdated.
     */
    val playStoreWebViewAppUri = when (webViewType) {
        WebViewType.ANDROID_SYSTEM -> Uri.parse("market://details?id=${PACKAGE_SYSTEM_WEBVIEW}")
        WebViewType.CHROME -> Uri.parse("market://details?id=${PACKAGE_CHROME_WEBVIEW}")
        else -> null
    }
}
