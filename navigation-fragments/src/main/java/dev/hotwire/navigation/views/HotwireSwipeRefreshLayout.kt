package dev.hotwire.navigation.views

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.view.children
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dev.hotwire.core.turbo.webview.HotwireWebView

internal class HotwireSwipeRefreshLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
        SwipeRefreshLayout(context, attrs) {

    /**
     * SwipeRefreshLayout only measures and lays out the first child it finds, even after
     * that child is removed. The WebView is attached to this container instead, which
     * stays that child while WebView instances come and go (such as a new WebView that
     * replaces one whose render process is gone).
     */
    internal val webViewContainer: FrameLayout = WebViewContainer(context)

    init {
        disableCustomDrawingOrder()
        addView(webViewContainer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    override fun canChildScrollUp(): Boolean {
        val webView = webViewContainer.children.firstOrNull() as? HotwireWebView

        return if (webView != null) {
            webView.scrollY > 0 || webView.elementTouchPreventsPullsToRefresh
        } else {
            false
        }
    }

    /**
     * Disable custom child drawing order. This fixes a crash while using a
     * stylus that dispatches hover events when the WebView is being removed.
     * This doesn't have any unintended consequences, since the WebView
     * container is the only possible child of this view.
     */
    private fun disableCustomDrawingOrder() {
        isChildrenDrawingOrderEnabled = false
    }

    private class WebViewContainer(context: Context) : FrameLayout(context) {
        /**
         * SwipeRefreshLayout only applies requestDisallowInterceptTouchEvent() to itself
         * when its child has nested scrolling enabled, so report the WebView's setting.
         */
        override fun isNestedScrollingEnabled(): Boolean {
            return getChildAt(0)?.isNestedScrollingEnabled ?: super.isNestedScrollingEnabled()
        }
    }
}
