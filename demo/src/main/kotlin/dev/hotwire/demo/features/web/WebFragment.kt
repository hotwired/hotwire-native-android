package dev.hotwire.demo.features.web

import android.os.Bundle
import android.view.View
import dev.hotwire.core.bridge.BridgeDelegate
import dev.hotwire.core.turbo.errors.HttpError
import dev.hotwire.core.turbo.errors.TurboVisitError
import dev.hotwire.core.turbo.fragments.TurboWebFragment
import dev.hotwire.core.turbo.nav.TurboNavGraphDestination
import dev.hotwire.core.turbo.views.TurboWebView
import dev.hotwire.core.turbo.visit.TurboVisitAction.REPLACE
import dev.hotwire.core.turbo.visit.TurboVisitOptions
import dev.hotwire.demo.R
import dev.hotwire.demo.base.NavDestination
import dev.hotwire.demo.strada.bridgeComponentFactories
import dev.hotwire.demo.util.SIGN_IN_URL

@TurboNavGraphDestination(uri = "turbo://fragment/web")
open class WebFragment : TurboWebFragment(), NavDestination {
    private val bridgeDelegate by lazy {
        BridgeDelegate(
            location = location,
            destination = this,
            componentFactories =  bridgeComponentFactories
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenu()
        viewLifecycleOwner.lifecycle.addObserver(bridgeDelegate)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewLifecycleOwner.lifecycle.removeObserver(bridgeDelegate)
    }

    override fun onColdBootPageStarted(location: String) {
        bridgeDelegate.onColdBootPageStarted()
    }

    override fun onColdBootPageCompleted(location: String) {
        bridgeDelegate.onColdBootPageCompleted()
    }

    override fun onWebViewAttached(webView: TurboWebView) {
        bridgeDelegate.onWebViewAttached(webView)
    }

    override fun onWebViewDetached(webView: TurboWebView) {
        bridgeDelegate.onWebViewDetached()
    }

    override fun onFormSubmissionStarted(location: String) {
        menuProgress?.isVisible = true
    }

    override fun onFormSubmissionFinished(location: String) {
        menuProgress?.isVisible = false
    }

    override fun onVisitErrorReceived(location: String, error: TurboVisitError) {
        if (error is HttpError.ClientError.Unauthorized) {
            navigate(SIGN_IN_URL, TurboVisitOptions(action = REPLACE))
        } else {
            super.onVisitErrorReceived(location, error)
        }
    }

    private fun setupMenu() {
        toolbarForNavigation()?.inflateMenu(R.menu.web)
    }
}
