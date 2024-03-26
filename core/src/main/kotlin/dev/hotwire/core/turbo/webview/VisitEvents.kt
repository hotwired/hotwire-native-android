package dev.hotwire.core.turbo.webview

import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WebViewNavigator(private val coroutineScope: CoroutineScope) {
    private sealed interface VisitEvent {
        data object Reload : VisitEvent
        data object StopLoading : VisitEvent
        data class LoadUrl(val url: String) : VisitEvent
    }

    private val navigationEvents: MutableSharedFlow<VisitEvent> = MutableSharedFlow(replay = 1)

    // Use Dispatchers.Main to ensure that the webview methods are called on UI thread
    internal suspend fun WebView.handleNavigationEvents(): Nothing = withContext(Dispatchers.Main) {
        navigationEvents.collect { event ->
            when (event) {
                is VisitEvent.Reload -> reload()
                is VisitEvent.StopLoading -> stopLoading()
                is VisitEvent.LoadUrl -> {
                    loadUrl(event.url)
                }
            }
        }
    }

    fun loadUrl(url: String) {
        coroutineScope.launch {
            navigationEvents.emit(
                VisitEvent.LoadUrl(url)
            )
        }
    }

    fun reload() {
        coroutineScope.launch {
            navigationEvents.emit(VisitEvent.Reload)
        }
    }
}

@Composable
fun rememberWebViewNavigator(
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): WebViewNavigator = remember(coroutineScope) {
    WebViewNavigator(coroutineScope)
}
