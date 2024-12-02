package dev.hotwire.navigation.fragments

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import dev.hotwire.core.bridge.BridgeDelegate
import dev.hotwire.core.turbo.errors.VisitError
import dev.hotwire.core.files.util.HOTWIRE_REQUEST_CODE_FILES
import dev.hotwire.core.files.util.HOTWIRE_REQUEST_CODE_GEOLOCATION_PERMISSION
import dev.hotwire.core.turbo.webview.HotwireWebChromeClient
import dev.hotwire.core.turbo.webview.HotwireWebView
import dev.hotwire.navigation.R
import dev.hotwire.navigation.config.HotwireNavigation
import dev.hotwire.navigation.destinations.HotwireDestinationDeepLink
import dev.hotwire.navigation.views.HotwireView

/**
 * The base class from which all bottom sheet web fragments in a
 * Hotwire app should extend from.
 *
 * For native bottom sheet fragments, refer to [HotwireBottomSheetFragment].
 */
@HotwireDestinationDeepLink(uri = "hotwire://fragment/web/modal/sheet")
open class HotwireWebBottomSheetFragment : HotwireBottomSheetFragment(), HotwireWebFragmentCallback {
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
        return inflater.inflate(R.layout.hotwire_fragment_web_bottom_sheet, container, false)
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

    override fun activityResultLauncher(requestCode: Int): ActivityResultLauncher<Intent>? {
        return when (requestCode) {
            HOTWIRE_REQUEST_CODE_FILES -> webDelegate.fileChooserResultLauncher
            else -> null
        }
    }

    override fun activityPermissionResultLauncher(requestCode: Int): ActivityResultLauncher<String>? {
        return when (requestCode) {
            HOTWIRE_REQUEST_CODE_GEOLOCATION_PERMISSION -> webDelegate.geoLocationPermissionResultLauncher
            else -> null
        }
    }

    override fun onStart() {
        super.onStart()
        webDelegate.onStart()
    }

    override fun onCancel(dialog: DialogInterface) {
        webDelegate.onDialogCancel()
        super.onCancel(dialog)
    }

    override fun onDismiss(dialog: DialogInterface) {
        webDelegate.onDialogDismiss()
        super.onDismiss(dialog)
    }

    override fun refresh(displayProgress: Boolean) {
        webDelegate.refresh(displayProgress)
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

    override fun onWebViewAttached(webView: HotwireWebView) {
        bridgeDelegate.onWebViewAttached(webView)
    }

    override fun onWebViewDetached(webView: HotwireWebView) {
        bridgeDelegate.onWebViewDetached()
    }

    // ----------------------------------------------------------------------------
    // HotwireWebFragmentCallback interface
    // ----------------------------------------------------------------------------

    /**
     * Gets the HotwireView instance in the Fragment's view
     * with resource ID R.id.hotwire_view.
     */
    final override val hotwireView: HotwireView?
        get() = view?.findViewById(R.id.hotwire_view)

    @SuppressLint("InflateParams")
    override fun createProgressView(location: String): View {
        return layoutInflater.inflate(R.layout.hotwire_progress_bottom_sheet, null)
    }

    @SuppressLint("InflateParams")
    override fun createErrorView(error: VisitError): View {
        return layoutInflater.inflate(R.layout.hotwire_error, null)
    }

    override fun createWebChromeClient(): HotwireWebChromeClient {
        return HotwireWebChromeClient(navigator.session)
    }

    override fun onVisitErrorReceived(location: String, error: VisitError) {
        webDelegate.showErrorView(error)
    }
}
