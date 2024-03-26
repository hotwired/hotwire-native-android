package dev.hotwire.core.turbo.webview

import android.content.Context
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import dev.hotwire.core.turbo.nav.HotwireNavDestination
import dev.hotwire.core.turbo.session.Session
import kotlinx.serialization.json.JsonNull.content

@Composable
fun WebViewScreen(
    state: WebViewState,
    navDestination: HotwireNavDestination,
    onWebViewAttached: (WebView) -> Unit = {},
    onWebViewDetached: (WebView) -> Unit = {},
    modifier: Modifier = Modifier
) {
    fun session() = navDestination.session

    BoxWithConstraints(modifier) {
        LaunchedEffect(session(), state) {
            snapshotFlow { state.currentVisit }.collect { visit ->
                visit?.let {
                    session().visit(it)
                }
            }
        }

        if (state.webViewIsAttached) {
            AndroidView(
                factory = { session().webView },
                modifier = modifier.fillMaxSize(),
                onRelease = {
                    //onDispose(it)
                }
            )
        }
    }
}

/*@Composable
fun WebView(
    state: WebViewState,
    navigator: WebViewNavigator = rememberWebViewNavigator(),
    modifier: Modifier = Modifier,
    onCreated: (WebView) -> Unit = {},
    onDispose: (WebView) -> Unit = {},
    chromeClient: HotwireWebChromeClient = remember { HotwireWebChromeClient() },
    factory: ((Context) -> HotwireWebView)? = null,
) {
    val webView = state.webView
    val client = remember { HotwireWebViewClient() }

    BoxWithConstraints(modifier) {
        // WebView changes it's layout strategy based on
        // it's layoutParams. We convert from Compose Modifier to
        // layout params here.
        val width =
            if (constraints.hasFixedWidth)
                FrameLayout.LayoutParams.MATCH_PARENT
            else
                FrameLayout.LayoutParams.WRAP_CONTENT
        val height =
            if (constraints.hasFixedHeight)
                FrameLayout.LayoutParams.MATCH_PARENT
            else
                FrameLayout.LayoutParams.WRAP_CONTENT

        val layoutParams = FrameLayout.LayoutParams(
            width,
            height
        )

        webView?.let { wv ->
            LaunchedEffect(wv, navigator) {
                with(navigator) {
                    wv.handleNavigationEvents()
                }
            }

            LaunchedEffect(wv, state) {
                snapshotFlow { state.currentVisit }.collect { visit ->
                    when (content) {
                        is WebContent.Url -> {
                            wv.loadUrl(content.url)
                        }
                    }
                }
            }
        }

        // Set the state of the client and chrome client
        // This is done internally to ensure they always are the same instance as the
        // parent Web composable
        client.state = state
        chromeClient.state = state

        AndroidView(
            factory = { context ->
                (factory?.invoke(context) ?: HotwireWebView(context)).apply {
                    onCreated(this)

                    this.layoutParams = layoutParams

                    webChromeClient = chromeClient
                    webViewClient = client
                }.also { state.webView = it }
            },
            modifier = modifier,
            onRelease = {
                onDispose(it)
            }
        )
    }
}
*/