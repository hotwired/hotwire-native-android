package dev.hotwire.navigation.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import dev.hotwire.core.bridge.BridgeDelegate
import dev.hotwire.core.turbo.errors.VisitError
import dev.hotwire.core.turbo.util.TURBO_REQUEST_CODE_FILES
import dev.hotwire.core.turbo.views.TurboWebChromeClient
import dev.hotwire.core.turbo.views.TurboWebView
import dev.hotwire.navigation.R
import dev.hotwire.navigation.config.HotwireNavigation
import dev.hotwire.navigation.destinations.HotwireDestination
import dev.hotwire.navigation.session.SessionModalResult
import dev.hotwire.navigation.views.TurboView

/**
 * The base class from which all web "standard" fragments (non-dialogs) in a
 * Hotwire app should extend from.
 *
 * For native fragments, refer to [HotwireFragment].
 */
@HotwireDestination(uri = "turbo://fragment/web")
open class HotwireWebFragment : HotwireFragment(), HotwireWebFragmentCallback {
    private lateinit var webDelegate: HotwireWebFragmentDelegate

    private val bridgeDelegate by lazy {
        BridgeDelegate(
            location = location,
            destination = this,
            componentFactories = HotwireNavigation.registeredBridgeComponentFactories
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webDelegate = HotwireWebFragmentDelegate(delegate, this, this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.turbo_fragment_web, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        webDelegate.onViewCreated()
        viewLifecycleOwner.lifecycle.addObserver(bridgeDelegate)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewLifecycleOwner.lifecycle.removeObserver(bridgeDelegate)
    }

    override fun onStart() {
        super.onStart()

        if (!delegate.sessionViewModel.modalResultExists) {
            webDelegate.onStart()
        }
    }

    /**
     * Called when the Fragment has been started again after receiving a
     * modal result. Will navigate if the result indicates it should.
     */
    override fun onStartAfterModalResult(result: SessionModalResult) {
        super.onStartAfterModalResult(result)
        webDelegate.onStartAfterModalResult(result)
    }

    /**
     * Called when the Fragment has been started again after a dialog has
     * been dismissed/canceled and no result is passed back.
     */
    override fun onStartAfterDialogCancel() {
        super.onStartAfterDialogCancel()

        if (!delegate.sessionViewModel.modalResultExists) {
            webDelegate.onStartAfterDialogCancel()
        }
    }

    /**
     * Refreshes the contents, performing a cold boot reload of the
     * WebView location.
     */
    override fun refresh(displayProgress: Boolean) {
        webDelegate.refresh(displayProgress)
    }

    override fun activityResultLauncher(requestCode: Int): ActivityResultLauncher<Intent>? {
        return when (requestCode) {
            TURBO_REQUEST_CODE_FILES -> webDelegate.fileChooserResultLauncher
            else -> null
        }
    }

    final override fun prepareNavigation(onReady: () -> Unit) {
        webDelegate.prepareNavigation(onReady)
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

    // ----------------------------------------------------------------------------
    // HotwireWebFragmentCallback interface
    // ----------------------------------------------------------------------------

    /**
     * Gets the TurboView instance in the Fragment's view
     * with resource ID R.id.turbo_view.
     */
    final override val turboView: TurboView?
        get() = view?.findViewById(R.id.turbo_view)

    @SuppressLint("InflateParams")
    override fun createProgressView(location: String): View {
        return layoutInflater.inflate(R.layout.turbo_progress, null)
    }

    @SuppressLint("InflateParams")
    override fun createErrorView(error: VisitError): View {
        return layoutInflater.inflate(R.layout.turbo_error, null)
    }

    override fun createWebChromeClient(): TurboWebChromeClient {
        return TurboWebChromeClient(navigator.session)
    }

    override fun onVisitErrorReceived(location: String, error: VisitError) {
        webDelegate.showErrorView(error)
    }
}
