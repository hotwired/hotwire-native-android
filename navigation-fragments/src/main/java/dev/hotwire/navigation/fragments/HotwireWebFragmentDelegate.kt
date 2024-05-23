package dev.hotwire.navigation.fragments

import android.content.Intent
import android.graphics.Bitmap
import android.webkit.HttpAuthHandler
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStateAtLeast
import dev.hotwire.core.config.Hotwire
import dev.hotwire.core.turbo.config.pullToRefreshEnabled
import dev.hotwire.core.turbo.errors.VisitError
import dev.hotwire.navigation.destinations.HotwireNavDestination
import dev.hotwire.core.turbo.session.SessionCallback
import dev.hotwire.core.turbo.views.TurboWebView
import dev.hotwire.core.turbo.visit.Visit
import dev.hotwire.core.turbo.visit.VisitAction
import dev.hotwire.core.turbo.visit.VisitDestination
import dev.hotwire.core.turbo.visit.VisitOptions
import dev.hotwire.navigation.session.SessionModalResult
import dev.hotwire.navigation.util.dispatcherProvider
import dev.hotwire.navigation.views.TurboView
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * Provides all the hooks for a web Fragment to delegate its lifecycle events
 * to this class.
 */
internal class HotwireWebFragmentDelegate(
    private val delegate: HotwireFragmentDelegate,
    private val navDestination: HotwireNavDestination,
    private val callback: HotwireWebFragmentCallback
) : SessionCallback, VisitDestination {

    private val location = navDestination.location
    private val visitOptions = currentVisitOptions()
    private val identifier = generateIdentifier()
    private var isInitialVisit = true
    private var isWebViewAttachedToNewDestination = false
    private var screenshot: Bitmap? = null
    private var screenshotOrientation = 0
    private var screenshotZoomed = false
    private var currentlyZoomed = false
    private val navigator = navDestination.navigator
    private val session get() = navigator.session
    private val turboView get() = callback.turboView
    private val viewTreeLifecycleOwner get() = turboView?.findViewTreeLifecycleOwner()

    /**
     * Get the session's WebView instance
     */
    val webView: TurboWebView
        get() = session.webView

    /**
     * The activity result launcher that handles file chooser results.
     */
    val fileChooserResultLauncher = registerFileChooserLauncher()

    fun prepareNavigation(onReady: () -> Unit) {
        session.removeCallback(this)
        detachWebView(onReady)
    }

    /**
     * Should be called by the implementing Fragment during
     * [androidx.fragment.app.Fragment.onViewCreated].
     */
    fun onViewCreated() {
        if (session.isRenderProcessGone) {
            navigator.createNewSession()
        }
    }

    /**
     * Should be called by the implementing Fragment during
     * [androidx.fragment.app.Fragment.onStart].
     */
    fun onStart() {
        initNavigationVisit()
        initWebChromeClient()
    }

    /**
     * Provides a hook when a fragment has been started again after receiving a
     * modal result. Will navigate if the result indicates it should.
     */
    fun onStartAfterModalResult(result: SessionModalResult) {
        if (!result.shouldNavigate) {
            initNavigationVisit()
            initWebChromeClient()
        }
    }

    /**
     * Provides a hook when the fragment has been started again after a dialog has
     * been dismissed/canceled and no result is passed back. Initializes all necessary views and
     * executes the visit.
     */
    fun onStartAfterDialogCancel() {
        initNavigationVisit()
        initWebChromeClient()
    }

    /**
     * Provides a hook when the dialog has been canceled. Detaches the WebView
     * before navigation.
     */
    fun onDialogCancel() {
        session.removeCallback(this)
        detachWebView()
    }

    /**
     * Provides a hook when the dialog has been dismissed. Detaches the WebView
     * before navigation.
     */
    fun onDialogDismiss() {
        // The WebView is already detached in most circumstances, but sometimes
        // fast user cancellation does not call onCancel() before onDismiss()
        if (webViewIsAttached()) {
            session.removeCallback(this)
            detachWebView()
        }
    }

    /**
     * Should be called by the implementing Fragment during
     * [dev.hotwire.core.turbo.nav.HotwireNavDestination.refresh]
     */
    fun refresh(displayProgress: Boolean) {
        if (webView.url == null) return

        turboView?.webViewRefresh?.apply {
            if (displayProgress && !isRefreshing) {
                isRefreshing = true
            }
        }

        isWebViewAttachedToNewDestination = false
        visit(location, restoreWithCachedSnapshot = false, reload = true)
    }

    /**
     * Displays the error view that's implemented via [HotwireWebFragmentCallback.createErrorView].
     */
    fun showErrorView(error: VisitError) {
        turboView?.addErrorView(callback.createErrorView(error))
    }

    // -----------------------------------------------------------------------
    // VisitDestination interface
    // -----------------------------------------------------------------------

    override fun isActive(): Boolean {
        return navDestination.isActive
    }

    override fun activityResultLauncher(requestCode: Int): ActivityResultLauncher<Intent>? {
        return navDestination.activityResultLauncher(requestCode)
    }

    // -----------------------------------------------------------------------
    // SessionCallback interface
    // -----------------------------------------------------------------------

    override fun onPageStarted(location: String) {
        callback.onColdBootPageStarted(location)
    }

    override fun onPageFinished(location: String) {
        callback.onColdBootPageCompleted(location)
    }

    override fun onZoomed(newScale: Float) {
        currentlyZoomed = true
        pullToRefreshEnabled(false)
    }

    override fun onZoomReset(newScale: Float) {
        currentlyZoomed = false
        pullToRefreshEnabled(navDestination.pathProperties.pullToRefreshEnabled)
    }

    override fun pageInvalidated() {}

    override fun visitLocationStarted(location: String) {
        callback.onVisitStarted(location)

        if (isWebViewAttachedToNewDestination) {
            showProgressView(location)
        }
    }

    override fun visitRendered() {
        callback.onVisitRendered(location)
        navDestination.fragmentViewModel.setTitle(title())
        removeTransitionalViews()
    }

    override fun visitCompleted(completedOffline: Boolean) {
        callback.onVisitCompleted(location, completedOffline)
        navDestination.fragmentViewModel.setTitle(title())
    }

    override fun onReceivedError(error: VisitError) {
        callback.onVisitErrorReceived(location, error)
    }

    override fun onRenderProcessGone() {
        navigator.route(location, VisitOptions(action = VisitAction.REPLACE))
    }

    override fun requestFailedWithError(visitHasCachedSnapshot: Boolean, error: VisitError) {
        if (visitHasCachedSnapshot) {
            callback.onVisitErrorReceivedWithCachedSnapshotAvailable(location, error)
        } else {
            callback.onVisitErrorReceived(location, error)
        }
    }

    override fun onReceivedHttpAuthRequest(handler: HttpAuthHandler, host: String, realm: String) {
        callback.onReceivedHttpAuthRequest(handler, host, realm)
    }

    override fun visitProposedToLocation(
        location: String,
        options: VisitOptions
    ) {
        navigator.route(location, options)
    }

    override fun visitDestination(): VisitDestination {
        return this
    }

    override fun formSubmissionStarted(location: String) {
        callback.onFormSubmissionStarted(location)
    }

    override fun formSubmissionFinished(location: String) {
        callback.onFormSubmissionFinished(location)
    }

    // -----------------------------------------------------------------------
    // Private
    // -----------------------------------------------------------------------

    private fun currentVisitOptions(): VisitOptions {
        val visitOptions = delegate.sessionViewModel.visitOptions
        return visitOptions?.getContentIfNotHandled() ?: VisitOptions()
    }

    private fun initNavigationVisit() {
        initView()
        attachWebViewAndVisit()
    }

    private fun initView() {
        currentlyZoomed = false
        turboView?.apply {
            initializePullToRefresh(this)
            initializeErrorPullToRefresh(this)
            showScreenshotIfAvailable(this)
            screenshot = null
            screenshotOrientation = 0
            screenshotZoomed = false
        }
    }

    private fun initWebChromeClient() {
        webView.webChromeClient = callback.createWebChromeClient()
    }

    private fun attachWebView(onReady: (Boolean) -> Unit = {}) {
        val view = turboView

        if (view == null) {
            onReady(false)
            return
        }

        view.attachWebView(webView) { attachedToNewDestination ->
            onReady(attachedToNewDestination)

            if (attachedToNewDestination) {
                callback.onWebViewAttached(webView)
            }
        }
    }

    /**
     * It's necessary to detach the shared WebView from a screen *before* it is hidden or exits and
     * the navigation animations run. The framework animator expects that the View hierarchy will
     * not change during the transition. Because the incoming screen will attach the WebView to the
     * new view hierarchy, it needs to already be detached from the previous screen.
     */
    private fun detachWebView(onReady: () -> Unit = {}) {
        val webView = webView
        screenshotView()

        turboView?.detachWebView(webView) {
            callback.onWebViewDetached(webView)
            onReady()
        }
    }

    private fun attachWebViewAndVisit() {
        // Attempt to attach the WebView. It may already be attached to the current instance.
        attachWebView {
            isWebViewAttachedToNewDestination = it

            // Visit every time the WebView is reattached to the current Fragment.
            if (isWebViewAttachedToNewDestination) {
                val currentSessionVisitRestored = !isInitialVisit &&
                    session.currentVisit?.destinationIdentifier == identifier &&
                    session.restoreCurrentVisit(this)

                if (!currentSessionVisitRestored) {
                    showProgressView(location)
                    visit(location, restoreWithCachedSnapshot = !isInitialVisit, reload = false)
                    isInitialVisit = false
                }
            }
        }
    }

    private fun webViewIsAttached(): Boolean {
        val webView = webView
        return turboView?.webViewIsAttached(webView) ?: false
    }

    private fun title(): String {
        return webView.title ?: ""
    }

    private fun registerFileChooserLauncher(): ActivityResultLauncher<Intent> {
        return navDestination.fragment.registerForActivityResult(StartActivityForResult()) { result ->
            session.fileChooserDelegate.onActivityResult(result)
        }
    }

    private fun visit(location: String, restoreWithCachedSnapshot: Boolean, reload: Boolean) {
        val restore = restoreWithCachedSnapshot && !reload
        val options = when {
            restore -> VisitOptions(action = VisitAction.RESTORE)
            reload -> VisitOptions()
            else -> visitOptions
        }

        viewTreeLifecycleOwner?.lifecycleScope?.launch {
            val snapshot = when (options.action) {
                VisitAction.ADVANCE -> fetchCachedSnapshot()
                else -> null
            }

            viewTreeLifecycleOwner?.lifecycle?.whenStateAtLeast(STARTED) {
                session.visit(
                    Visit(
                        location = location,
                        destinationIdentifier = identifier,
                        restoreWithCachedSnapshot = restoreWithCachedSnapshot,
                        reload = reload,
                        callback = this@HotwireWebFragmentDelegate,
                        options = options.copy(snapshotHTML = snapshot)
                    )
                )
            }
        }
    }

    private suspend fun fetchCachedSnapshot(): String? {
        return withContext(dispatcherProvider.io) {
            val response = Hotwire.config.offlineRequestHandler?.getCachedSnapshot(
                url = location
            )

            response?.data?.use {
                String(it.readBytes())
            }
        }
    }

    private fun screenshotView() {
        turboView?.let {
            screenshot = it.createScreenshot()
            screenshotOrientation = it.screenshotOrientation()
            screenshotZoomed = currentlyZoomed
            showScreenshotIfAvailable(it)
        }
    }

    private fun showProgressView(location: String) {
        turboView?.addProgressView(callback.createProgressView(location))
    }

    private fun initializePullToRefresh(turboView: TurboView) {
        turboView.webViewRefresh?.apply {
            isEnabled = navDestination.pathProperties.pullToRefreshEnabled
            setOnRefreshListener {
                refresh(displayProgress = true)
            }
        }
    }

    private fun initializeErrorPullToRefresh(turboView: TurboView) {
        turboView.errorRefresh?.apply {
            setOnRefreshListener {
                refresh(displayProgress = true)
            }
        }
    }

    private fun pullToRefreshEnabled(enabled: Boolean) {
        turboView?.webViewRefresh?.isEnabled = enabled
    }

    private fun showScreenshotIfAvailable(turboView: TurboView) {
        if (screenshotOrientation == turboView.screenshotOrientation() &&
            screenshotZoomed == currentlyZoomed
        ) {
            screenshot?.let { turboView.addScreenshot(it) }
        }
    }

    private fun removeTransitionalViews() {
        turboView?.webViewRefresh?.isRefreshing = false
        turboView?.errorRefresh?.isRefreshing = false
        turboView?.removeProgressView()
        turboView?.removeScreenshot()
        turboView?.removeErrorView()
    }

    private fun generateIdentifier(): Int {
        return Random.nextInt(0, 999999999)
    }
}
