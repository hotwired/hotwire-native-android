package dev.hotwire.core.turbo.session

import android.webkit.HttpAuthHandler
import dev.hotwire.core.turbo.errors.TurboVisitError
import dev.hotwire.core.turbo.nav.HotwireNavDestination
import dev.hotwire.core.turbo.visit.TurboVisitOptions

internal interface TurboSessionCallback {
    fun onPageStarted(location: String)
    fun onPageFinished(location: String)
    fun onReceivedError(error: TurboVisitError)
    fun onRenderProcessGone()
    fun onZoomed(newScale: Float)
    fun onZoomReset(newScale: Float)
    fun pageInvalidated()
    fun requestFailedWithError(visitHasCachedSnapshot: Boolean, error: TurboVisitError)
    fun onReceivedHttpAuthRequest(handler: HttpAuthHandler, host: String, realm: String)
    fun visitRendered()
    fun visitCompleted(completedOffline: Boolean)
    fun visitLocationStarted(location: String)
    fun visitProposedToLocation(location: String, options: TurboVisitOptions)
    fun visitNavDestination(): HotwireNavDestination
    fun formSubmissionStarted(location: String)
    fun formSubmissionFinished(location: String)
}
