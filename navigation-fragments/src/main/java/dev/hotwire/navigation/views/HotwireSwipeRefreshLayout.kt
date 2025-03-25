package dev.hotwire.navigation.views

import android.content.Context
import android.util.AttributeSet
import androidx.core.view.children
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dev.hotwire.core.turbo.webview.HotwireWebView

internal class HotwireSwipeRefreshLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
        SwipeRefreshLayout(context, attrs) {

    init {
        disableCustomDrawingOrder()
    }

    override fun canChildScrollUp(): Boolean {
        val webView = children.firstOrNull() as? HotwireWebView

        return if (webView != null) {
            webView.scrollY > 0 || webView.elementTouchPreventsPullsToRefresh
        } else {
            false
        }
    }

    /**
     * Disable custom child drawing order. This fixes a crash while using a
     * stylus that dispatches hover events when the WebView is being removed.
     * This doesn't have any unintended consequences, since the WebView is the
     * only possible child of this view.
     */
    private fun disableCustomDrawingOrder() {
        isChildrenDrawingOrderEnabled = false
    }
}
