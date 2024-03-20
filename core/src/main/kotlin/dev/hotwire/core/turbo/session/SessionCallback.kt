package dev.hotwire.core.turbo.session

import android.webkit.HttpAuthHandler
import dev.hotwire.core.turbo.errors.VisitError
import dev.hotwire.core.turbo.nav.HotwireNavDestination
import dev.hotwire.core.turbo.visit.VisitOptions

internal interface SessionCallback {
    fun onPageStarted(location: String)
    fun onPageFinished(location: String)
    fun onReceivedError(error: VisitError)
    fun onRenderProcessGone()
    fun onZoomed(newScale: Float)
    fun onZoomReset(newScale: Float)
    fun pageInvalidated()
    fun requestFailedWithError(visitHasCachedSnapshot: Boolean, error: VisitError)
    fun onReceivedHttpAuthRequest(handler: HttpAuthHandler, host: String, realm: String)
    fun visitRendered()
    fun visitCompleted(completedOffline: Boolean)
    fun visitLocationStarted(location: String)
    fun visitProposedToLocation(location: String, options: VisitOptions)
    fun visitNavDestination(): HotwireNavDestination
    fun formSubmissionStarted(location: String)
    fun formSubmissionFinished(location: String)
}
