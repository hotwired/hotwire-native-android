package dev.hotwire.navigation.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.*
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dev.hotwire.navigation.R

/**
 * Turbo view that hosts the shared WebView, a progress view, an error view, and allows
 * pull-to-refresh behavior.
 */
class HotwireView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    FrameLayout(context, attrs, defStyleAttr) {

    private val webViewContainer: ViewGroup get() = findViewById(R.id.hotwire_webView_container)
    private val progressContainer: ViewGroup get() = findViewById(R.id.hotwire_progress_container)
    private val errorContainer: ViewGroup get() = findViewById(R.id.hotwire_error_container)
    private val screenshotView: ImageView get() = findViewById(R.id.hotwire_screenshot)

    internal val webViewRefresh: SwipeRefreshLayout? get() = webViewContainer as? SwipeRefreshLayout
    internal val errorRefresh: SwipeRefreshLayout? get() = findViewById(R.id.hotwire_error_refresh)

    internal fun attachWebView(webView: WebView, onAttachedToNewDestination: (Boolean) -> Unit) {
        if (webView.parent != null) {
            onAttachedToNewDestination(false)
            return
        }

        // Match the WebView background with its new parent
        if (background is ColorDrawable) {
            webView.setBackgroundColor((background as ColorDrawable).color)
        }

        webViewContainer.post {
            // To avoid edge-case lifecycle issues, ensure that the
            // view is still attached to the window and the webview
            // doesn't have a new parent, since we have no control
            // over the message queue.
            if (isAttachedToWindow && webView.parent == null) {
                webView.updateLayoutParams {
                    height = when (webViewContainer) {
                        is ScrollingView -> LayoutParams.WRAP_CONTENT
                        else -> LayoutParams.MATCH_PARENT
                    }
                }

                webViewContainer.addView(webView)
                onAttachedToNewDestination(true)
            }
        }
    }

    internal fun detachWebView(webView: WebView, onDetached: () -> Unit) {
        // If the view is already detached from the window (like
        // when dismissing a bottom sheet), detach immediately,
        // since posting to the message queue will be ignored.
        if (webViewContainer.windowToken == null) {
            webViewContainer.removeView(webView)
            onDetached()
        } else {
            webViewContainer.post {
                webViewContainer.removeView(webView)
                onDetached()
            }
        }
    }

    internal fun webViewIsAttached(webView: WebView): Boolean {
        return webViewContainer.contains(webView)
    }

    internal fun addProgressView(progressView: View) {
        // Don't show the progress view if a screenshot is available
        if (screenshotView.isVisible) return

        check(progressView.parent == null) { "Progress view cannot be attached to another parent" }

        removeProgressView()
        progressContainer.addView(progressView)
        progressContainer.isVisible = true
    }

    internal fun removeProgressView() {
        progressContainer.removeAllViews()
        progressContainer.isVisible = false
    }

    internal fun addScreenshot(screenshot: Bitmap?) {
        if (screenshot == null) return

        screenshotView.setImageBitmap(screenshot)
        screenshotView.isVisible = true
    }

    internal fun removeScreenshot() {
        screenshotView.setImageBitmap(null)
        screenshotView.isVisible = false
    }

    internal fun addErrorView(errorView: View) {
        check(errorView.parent == null) { "Error view cannot be attached to another parent" }

        removeErrorView()
        errorContainer.addView(errorView)
        errorContainer.isVisible = true

        errorRefresh?.let {
            it.isVisible = true
            it.isEnabled = true
            it.isRefreshing = false
        }
    }

    internal fun removeErrorView() {
        errorContainer.removeAllViews()
        errorContainer.isVisible = false

        errorRefresh?.let {
            it.isVisible = false
            it.isEnabled = false
            it.isRefreshing = false
        }
    }

    internal fun currentOrientation(): Int {
        return context.resources.configuration.orientation
    }
}
