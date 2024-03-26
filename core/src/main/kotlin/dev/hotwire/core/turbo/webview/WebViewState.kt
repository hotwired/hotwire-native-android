package dev.hotwire.core.turbo.webview

import android.webkit.WebView
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import dev.hotwire.core.turbo.errors.VisitError
import dev.hotwire.core.turbo.visit.Visit

class WebViewState {
    var webViewIsAttached = true
        internal set

    var isRenderProcessGone = false
        internal set

    var isReady = false
        internal set

    /**
     *  The visit being loaded by the WebView
     */
    var currentVisit: Visit? by mutableStateOf(null)

    /**
     * Whether the WebView is currently [LoadingState.Loading] data in its main frame (along with
     * progress) or the data loading has [LoadingState.Finished]. See [LoadingState]
     */
    var loadingState: LoadingState by mutableStateOf(LoadingState.Initializing)
        internal set

    /**
     * The title received from the loaded content of the current page
     */
    var pageTitle: String? by mutableStateOf(null)
        internal set

    /**
     * A list for errors captured in the last load. Reset when a new page is loaded.
     * Errors could be from any resource (iframe, image, etc.), not just for the main page.
     * For more fine grained control use the OnError callback of the WebView.
     */
    val errorsForCurrentRequest: SnapshotStateList<VisitError> = mutableStateListOf()

    // We need access to this in the state saver. An internal DisposableEffect or AndroidView
    // onDestroy is called after the state saver and so can't be used.
    internal var webView by mutableStateOf<WebView?>(null)
}

@Composable
internal fun rememberWebViewState(visit: Visit): WebViewState =
    // Rather than using .apply {} here we will recreate the state, this prevents
    // a recomposition loop when the webview updates the url itself.
    remember {
        WebViewState()
    }.apply {
        this.currentVisit = visit
    }