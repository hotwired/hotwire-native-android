package dev.hotwire.core.turbo.webview

sealed class LoadingState {
    /**
     * Describes a WebView that has not yet loaded for the first time.
     */
    data object Initializing : LoadingState()

    /**
     *
     */
    data object ColdBooting : LoadingState()

    /**
     *
     */
    data object Loading : LoadingState()

    /**
     * Describes a webview that has finished loading content.
     */
    data object VisitRendered : LoadingState()

    /**
     *
     */
    data object VisitCompleted : LoadingState()
}
