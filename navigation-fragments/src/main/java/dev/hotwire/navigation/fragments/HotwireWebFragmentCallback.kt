package dev.hotwire.navigation.fragments

import android.view.View
import android.webkit.HttpAuthHandler
import dev.hotwire.core.turbo.errors.VisitError
import dev.hotwire.navigation.views.TurboView
import dev.hotwire.core.turbo.webview.HotwireWebChromeClient
import dev.hotwire.core.turbo.webview.HotwireWebView

/**
 * Callback interface to be implemented by a [HotwireWebFragment],
 * [HotwireWebBottomSheetFragment], or subclass.
 */
interface HotwireWebFragmentCallback {
    /**
     * The TurboView instance located in the Fragment's view.
     */
    val turboView: TurboView?

    /**
     * Inflate and return a new view to serve as an error view.
     */
    fun createErrorView(error: VisitError): View

    /**z
     * Inflate and return a new view to serve as a progress view.
     */
    fun createProgressView(location: String): View

    /**
     * Create and return a new web chrome client instance.
     */
    fun createWebChromeClient(): HotwireWebChromeClient

    /**
     * Called when the WebView has been attached to the current destination.
     */
    fun onWebViewAttached(webView: HotwireWebView) {}

    /**
     * Called when the WebView has been detached from the current destination.
     */
    fun onWebViewDetached(webView: HotwireWebView) {}

    /**
     * Called when Turbo begins a WebView cold boot (fresh resources).
     */
    fun onColdBootPageStarted(location: String) {}

    /**
     * Called when Turbo completes a WebView cold boot (fresh resources).
     */
    fun onColdBootPageCompleted(location: String) {}

    /**
     * Called when a Turbo visit has started.
     */
    fun onVisitStarted(location: String) {}

    /**
     * Called when a Turbo visit has rendered (from a cached snapshot or
     * from a fresh network request). This may be called multiple times
     * during a normal visit lifecycle.
     */
    fun onVisitRendered(location: String) {}

    /**
     * Called when a Turbo visit has completed.
     */
    fun onVisitCompleted(location: String, completedOffline: Boolean) {}

    /**
     * Called when a Turbo visit resulted in an error.
     */
    fun onVisitErrorReceived(location: String, error: VisitError) {}

    /**
     * Called when a Turbo form submission has started.
     */
    fun onFormSubmissionStarted(location: String) {}

    /**
     * Called when a Turbo form submission has finished.
     */
    fun onFormSubmissionFinished(location: String) {}

    /**
     * Called when the Turbo visit resulted in an error, but a cached
     * snapshot is being displayed, which may be stale.
     */
    fun onVisitErrorReceivedWithCachedSnapshotAvailable(location: String, error: VisitError) {}

    /**
     * Called when the WebView has received an HTTP authentication request.
     */
    fun onReceivedHttpAuthRequest(handler: HttpAuthHandler, host: String, realm: String) {
        handler.cancel()
    }
}
