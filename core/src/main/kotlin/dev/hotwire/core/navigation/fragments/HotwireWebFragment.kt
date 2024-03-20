package dev.hotwire.core.navigation.fragments

import android.os.Bundle
import android.view.View
import dev.hotwire.core.bridge.BridgeDelegate
import dev.hotwire.core.turbo.fragments.TurboWebFragment
import dev.hotwire.core.turbo.nav.HotwireDestination
import dev.hotwire.core.turbo.views.TurboWebView

@HotwireDestination(uri = "turbo://fragment/web")
open class HotwireWebFragment : TurboWebFragment() {
    private val bridgeDelegate by lazy {
        BridgeDelegate(location = location, destination = this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
}
