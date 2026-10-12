package dev.hotwire.core.turbo.errors

/**
 * Represents all possible errors received when attempting to load a page.
 */
sealed interface VisitError {
    val description: String?
}

// An extension, not a member: a default method on VisitError fails R8's
// bytecode verification for HttpError's nested cases.
@Deprecated("Use the description property.", ReplaceWith("description"))
fun VisitError.description(): String? = description
