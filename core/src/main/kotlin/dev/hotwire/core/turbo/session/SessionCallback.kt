package dev.hotwire.core.turbo.session

import android.webkit.HttpAuthHandler
import dev.hotwire.core.turbo.errors.VisitError
import dev.hotwire.core.turbo.visit.VisitDestination
import dev.hotwire.core.turbo.visit.VisitOptions

interface SessionCallback {
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
    fun visitRequestFinished()
    fun visitCompleted(completedOffline: Boolean)
    fun visitLocationStarted(location: String)
    fun visitProposedToLocation(location: String, options: VisitOptions)
    fun visitProposedToCrossOriginRedirect(location: String)
    fun visitDestination(): VisitDestination
    fun formSubmissionStarted(location: String)
    fun formSubmissionFinished(location: String)
}
