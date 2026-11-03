package dev.hotwire.navigation.fragments

// Android's WebView falls back to the formatted URL when a page has no <title>.
// Chromium drops the scheme, a leading "www." and a bare host's trailing slash,
// and decodes spaces, so compare both sides with the same parts removed.
internal fun displayTitle(title: String?, url: String?): String {
    val pageTitle = title.orEmpty()
    return if (pageTitle.asDisplayUrl() == url.orEmpty().asDisplayUrl()) "" else pageTitle
}

private fun String.asDisplayUrl(): String {
    return substringAfter("://")
        .removePrefix("www.")
        .replace("%20", " ")
        .trimEnd('/')
}
