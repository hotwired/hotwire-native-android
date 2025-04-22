package dev.hotwire.navigation.session

import android.os.Bundle
import dev.hotwire.core.turbo.visit.VisitOptions

/**
 * Wraps all the relevant data returned by the modal after it closes. This allows the fragment
 * underneath the dismissed modal to process the result as necessary.
 *
 * @property location Location that the modal visited.
 * @property options Visit options that the modal used.
 * @property bundle Any additional data used by the navigation library.
 */
data class SessionModalResult(
    val location: String,
    val options: VisitOptions,
    val bundle: Bundle?
)
