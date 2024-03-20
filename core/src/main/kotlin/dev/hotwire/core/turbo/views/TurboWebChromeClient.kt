package dev.hotwire.core.turbo.views

import android.net.Uri
import android.os.Message
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import dev.hotwire.core.turbo.session.Session
import dev.hotwire.core.turbo.util.toJson
import dev.hotwire.core.turbo.visit.TurboVisitOptions

open class TurboWebChromeClient(val session: Session) : WebChromeClient() {
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

    override fun onCreateWindow(webView: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
        val message = webView.handler.obtainMessage()
        webView.requestFocusNodeHref(message)

        message.data.getString("url")?.let {
            session.visitProposedToLocation(
                location = it,
                optionsJson = TurboVisitOptions().toJson()
            )
        }

        return false
    }
}
