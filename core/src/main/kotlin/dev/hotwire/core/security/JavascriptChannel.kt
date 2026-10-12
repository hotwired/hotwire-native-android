package dev.hotwire.core.security

import android.webkit.WebView
import androidx.annotation.VisibleForTesting
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature.WEB_MESSAGE_LISTENER
import androidx.webkit.WebViewFeature.isFeatureSupported
import dev.hotwire.core.logging.logError
import dev.hotwire.core.logging.logWarning

/**
 * The only way the library's bundled JavaScript reaches native code. Every
 * message is gated on its source frame's origin before it is decoded.
 */
internal class JavascriptChannel(
    private val name: String,
    private val onMessage: (JavascriptMessage) -> Unit
) {
    // Robolectric lacks WebMessageListener, so tests set this directly.
    var isInstalled = false
        @VisibleForTesting set

    /**
     * Must run before the page loads.
     */
    fun install(webView: WebView) {
        if (isFeatureSupported(WEB_MESSAGE_LISTENER)) {
            // The policy can change after install, so every frame gets the
            // channel and receive() gates each message.
            WebViewCompat.addWebMessageListener(webView, name, setOf("*")) {
                _, message, sourceOrigin, isMainFrame, _ ->
                receive(message.data.orEmpty(), sourceOrigin.toString(), isMainFrame)
            }
            isInstalled = true
        } else {
            logError(
                "webMessageListenerNotSupported",
                "The WebView version on this device is not supported"
            )
        }
    }

    /**
     * Runs on the main thread, where the message listener delivers.
     */
    fun receive(data: String, sourceOrigin: String, isMainFrame: Boolean) {
        if (!isMainFrame || !isTrustedForNativeAccess(sourceOrigin)) {
            logWarning(
                "javascriptMessageBlockedForUntrustedOrigin",
                listOf("channel" to name, "origin" to sourceOrigin, "isMainFrame" to isMainFrame)
            )
            return
        }

        val message = data.toJavascriptMessageOrNull() ?: run {
            logWarning("javascriptMessageMalformed", listOf("channel" to name))
            return
        }

        try {
            onMessage(message)
        } catch (e: RuntimeException) {
            logError("javascriptMessageFailed", e)
        }
    }
}
