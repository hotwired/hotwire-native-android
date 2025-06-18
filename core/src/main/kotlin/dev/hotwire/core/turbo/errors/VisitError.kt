package dev.hotwire.core.turbo.errors

/**
 * Represents all possible errors received when attempting to load a page.
 */
sealed interface VisitError {
    fun description() = when (this) {
        is HttpError -> reasonPhrase
        is LoadError -> description
        is WebError -> description
        is WebSslError -> description
    }
}
