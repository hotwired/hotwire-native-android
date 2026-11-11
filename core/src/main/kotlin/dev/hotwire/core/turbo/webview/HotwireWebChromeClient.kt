package dev.hotwire.core.turbo.webview

import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.webkit.GeolocationPermissions
import android.webkit.JsResult
import android.webkit.PermissionRequest
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.hotwire.core.R
import dev.hotwire.core.turbo.session.Session
import dev.hotwire.core.turbo.visit.VisitOptions

open class HotwireWebChromeClient(val session: Session) : WebChromeClient() {
    private var windowWebView: WebView? = null

    override fun onJsAlert(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        val context = view?.context ?: return false

        MaterialAlertDialogBuilder(context)
            .setMessage(message)
            .setPositiveButton(context.getString(R.string.hotwire_dialog_ok)) { dialog, _ ->
                dialog.cancel()
                result?.confirm()
            }
            .setCancelable(false)
            .create()
            .show()

        return true
    }

    override fun onJsConfirm(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        val context = view?.context ?: return false

        MaterialAlertDialogBuilder(context)
            .setMessage(message)
            .setNegativeButton(context.getString(R.string.hotwire_dialog_cancel)) { dialog, _ ->
                dialog.cancel()
                result?.cancel()
            }
            .setPositiveButton(context.getString(R.string.hotwire_dialog_ok)) { dialog, _ ->
                dialog.cancel()
                result?.confirm()
            }
            .setCancelable(false)
            .create()
            .show()

        return true
    }

    override fun onShowFileChooser(
        webView: WebView,
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: FileChooserParams
    ): Boolean {
        return session.fileChooserDelegate.onShowFileChooser(
            filePathCallback = filePathCallback,
            params = fileChooserParams
        )
    }

    /**
     * Handles `target="_blank"` links and `window.open()` calls. The new window
     * is never displayed. Instead, it's backed by a placeholder WebView that
     * captures the location the window navigates to and proposes a visit to it.
     */
    override fun onCreateWindow(
        webView: WebView,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message?
    ): Boolean {
        val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false

        // Only one window is tracked at a time. A previous window that
        // never navigated anywhere is no longer needed.
        destroyWindowWebView()

        val windowWebView = WebView(webView.context).apply {
            webViewClient = WindowWebViewClient()
            webChromeClient = WindowWebChromeClient()
        }

        this.windowWebView = windowWebView
        transport.webView = windowWebView
        resultMsg.sendToTarget()

        return true
    }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        callback: GeolocationPermissions.Callback?
    ) {
        session.geolocationPermissionDelegate.onRequestPermission(origin, callback)
    }

    override fun onGeolocationPermissionsHidePrompt() {
        session.geolocationPermissionDelegate.onHidePrompt()
    }

    override fun onPermissionRequest(request: PermissionRequest) {
        if (request.requestsMediaCapture()) {
            session.webViewPermissionDelegate.onRequest(request)
        } else {
            super.onPermissionRequest(request)
        }
    }

    override fun onPermissionRequestCanceled(request: PermissionRequest) {
        // Always forward the cancel; the delegate is a no-op when the request
        // doesn't match the one it's currently tracking.
        session.webViewPermissionDelegate.onCancel(request)
        super.onPermissionRequestCanceled(request)
    }

    private fun windowLocationRequested(window: WebView, location: String) {
        // A window opened without a location starts on about:blank and
        // may still navigate somewhere, e.g. via `window.location`.
        if (window != windowWebView || URLUtil.isAboutUrl(location)) return

        destroyWindowWebView()
        session.visitProposedToLocation(
            location = location,
            options = VisitOptions()
        )
    }

    private fun destroyWindowWebView() {
        val window = windowWebView ?: return
        windowWebView = null

        // The window may be destroyed from within one of its own client
        // callbacks, so defer destroying it until the callback returns.
        Handler(Looper.getMainLooper()).post { window.destroy() }
    }

    private inner class WindowWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            windowLocationRequested(view, request.url.toString())
            return true
        }

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            url?.let { windowLocationRequested(view, it) }
        }
    }

    private inner class WindowWebChromeClient : WebChromeClient() {
        override fun onCloseWindow(window: WebView) {
            if (window == windowWebView) {
                destroyWindowWebView()
            }
        }
    }

    private fun PermissionRequest.requestsMediaCapture(): Boolean {
        val resources = resources ?: return false
        return resources.isNotEmpty() && resources.all { resource ->
            resource == PermissionRequest.RESOURCE_AUDIO_CAPTURE ||
                    resource == PermissionRequest.RESOURCE_VIDEO_CAPTURE
        }
    }
}
