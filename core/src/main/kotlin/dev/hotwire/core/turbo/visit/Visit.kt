package dev.hotwire.core.turbo.visit

import dev.hotwire.core.turbo.session.SessionCallback

data class Visit(
    val location: String,
    val destinationIdentifier: Int,
    val restoreWithCachedSnapshot: Boolean,
    val reload: Boolean,
    var callback: SessionCallback?,             // Available while current visit
    var identifier: String = "",                // Updated after visitStarted()
    var completedOffline: Boolean = false,      // Updated from shouldInterceptRequest()
    val options: VisitOptions
)
