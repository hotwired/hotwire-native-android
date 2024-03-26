package dev.hotwire.core.turbo.webview

import android.net.Uri
import android.os.Message
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import dev.hotwire.core.turbo.util.toJson
import dev.hotwire.core.turbo.visit.VisitOptions

open class HotwireWebChromeClient() : WebChromeClient() {
    open lateinit var state: WebViewState
        internal set

    override fun onShowFileChooser(
        webView: WebView,
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: FileChooserParams
    ): Boolean {
        /*return session.fileChooserDelegate.onShowFileChooser(
            filePathCallback = filePathCallback,
            params = fileChooserParams
        )*/
        return true
    }

    override fun onCreateWindow(webView: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
        val message = webView.handler.obtainMessage()
        webView.requestFocusNodeHref(message)

        /*message.data.getString("url")?.let {
            session.visitProposedToLocation(
                location = it,
                optionsJson = VisitOptions().toJson()
            )
        }*/

        return false
    }
}
