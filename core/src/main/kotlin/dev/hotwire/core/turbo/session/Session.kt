package dev.hotwire.core.turbo.session

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.http.SslError
import android.util.SparseArray
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.webkit.WebResourceErrorCompat
import androidx.webkit.WebViewClientCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature.VISUAL_STATE_CALLBACK
import androidx.webkit.WebViewFeature.isFeatureSupported
import dev.hotwire.core.config.Hotwire
import dev.hotwire.core.files.delegates.FileChooserDelegate
import dev.hotwire.core.files.delegates.GeolocationPermissionDelegate
import dev.hotwire.core.logging.logEvent
import dev.hotwire.core.turbo.errors.HttpError
import dev.hotwire.core.turbo.errors.LoadError
import dev.hotwire.core.turbo.errors.WebError
import dev.hotwire.core.turbo.errors.WebSslError
import dev.hotwire.core.turbo.http.HotwireHttpClient
import dev.hotwire.core.turbo.http.HttpRepository
import dev.hotwire.core.turbo.offline.*
import dev.hotwire.core.turbo.util.isHttpGetRequest
import dev.hotwire.core.turbo.util.runOnUiThread
import dev.hotwire.core.turbo.util.toJson
import dev.hotwire.core.turbo.visit.Visit
import dev.hotwire.core.turbo.visit.VisitAction
import dev.hotwire.core.turbo.visit.VisitOptions
import dev.hotwire.core.turbo.webview.HotwireWebView
import kotlinx.coroutines.launch
import java.util.Date

/**
 * This class is primarily responsible for managing an instance of an Android WebView that will
 * be shared between destinations.
 *
 * @property sessionName An arbitrary name to be used as an identifier for a given session.
 * @property activity The activity to which the session will be bound to.
 * @property webView An instance of a [HotwireWebView] to be shared/managed.
 */
@Suppress("unused")
class Session(
    internal val sessionName: String,
    private val activity: AppCompatActivity,
    val webView: HotwireWebView
) {
    internal var coldBootVisitIdentifier = ""
    internal var previousOverrideUrlTime = 0L
    internal var isColdBooting = false
    internal var visitPending = false
    internal var restorationIdentifiers = SparseArray<String>()
    internal val context: Context = activity.applicationContext
    internal val httpRepository = HttpRepository()
    internal val offlineHttpRepository = OfflineHttpRepository(activity.lifecycleScope)
    internal val offlineRequestInterceptor = OfflineWebViewRequestInterceptor(this)

    // User accessible

    /**
     * The current visit for the current destination.
     */
    var currentVisit: Visit? = null
        internal set

    /**
     * Provides the status of whether Turbo is initialized and ready for use.
     */
    var isReady = false
        internal set

    /**
     * Specifies whether the render process is gone for the WebView instance. If the
     * render process is gone, this Session and its WebView cannot be reused and must
     * be recreated.
     */
    var isRenderProcessGone = false
        internal set

    /**
     * The delegate that handles WebView-requested file chooser requests.
     */
    val fileChooserDelegate = FileChooserDelegate(this)

    /**
     * The delegate the handles WebView-requested geolocation permission requests.
     */
    val geolocationPermissionDelegate = GeolocationPermissionDelegate(this)

    init {
        initializeWebView()
        HotwireHttpClient.enableCachingWith(context)
        fileChooserDelegate.deleteCachedFiles()
    }

    // Public

    /**
     * Fetches a given location and returns the response to the [OfflineRequestHandler]
     * Allows an offline cache to contain specific items instead of solely relying on visited items.
     *
     * @param location Location to cache.
     */
    fun preCacheLocation(location: String) {
        val requestHandler = checkNotNull(Hotwire.config.offlineRequestHandler) {
            "An offline request handler must be provided to pre-cache $location"
        }

        offlineHttpRepository.preCache(
            requestHandler, OfflinePreCacheRequest(
                url = location, userAgent = Hotwire.config.userAgentWithWebViewDefault(context)
            )
        )
    }

    /**
     * Resets this session to a cold boot state. The first subsequent visit after resetting will
     * execute a full cold boot (reloading of all resources).
     */
    fun reset() {
        logEvent("reset")
        currentVisit?.identifier = ""
        coldBootVisitIdentifier = ""
        restorationIdentifiers.clear()
        visitPending = false
        isReady = false
        isColdBooting = false
    }

    fun visit(visit: Visit) {
        this.currentVisit = visit
        callback { it.visitLocationStarted(visit.location) }

        if (visit.reload) {
            reset()
        }

        when {
            isColdBooting -> visitPending = true
            isReady -> visitLocation(visit)
            else -> visitLocationAsColdBoot(visit)
        }
    }

    /**
     * Synthetically restore the WebView's current visit without using a cached snapshot or a
     * visit request. This is used when restoring a destination from the backstack,
     * but the WebView's current location hasn't changed from the destination's location.
     */
    fun restoreCurrentVisit(callback: SessionCallback): Boolean {
        val visit = currentVisit ?: return false
        val restorationIdentifier = restorationIdentifiers[visit.destinationIdentifier]

        if (!isReady || restorationIdentifier == null) {
            return false
        }

        logEvent("restoreCurrentVisit",
            "location" to visit.location,
            "visitIdentifier" to visit.identifier,
            "restorationIdentifier" to restorationIdentifier
        )

        visit.callback = callback
        visitRendered(visit.identifier)
        visitCompleted(visit.identifier, restorationIdentifier)

        return true
    }

    fun removeCallback(callback: SessionCallback) {
        currentVisit?.let { visit ->
            if (visit.callback == callback) {
                visit.callback = null
            }
        }
    }

    // Callbacks from Turbo JS Core

    /**
     * Called by Turbo bridge when a new visit is proposed.
     *
     * Warning: This method is public so it can be used as a Javascript Interface.
     * You should never call this directly as it could lead to unintended behavior.
     *
     * @param location The location to visit.
     * @param optionsJson A JSON block to be serialized into [VisitOptions].
     */
    @JavascriptInterface
    fun visitProposedToLocation(location: String, optionsJson: String) {
        val options = VisitOptions.fromJSON(optionsJson) ?: return

        logEvent("visitProposedToLocation", "location" to location, "options" to options)
        callback { it.visitProposedToLocation(location, options) }
    }

    private fun visitProposedToCrossOriginRedirect(
        location: String,
        redirectLocation: String,
        visitIdentifier: String
    ) {
        logEvent("visitProposedToCrossOriginRedirect",
            "location" to location,
            "redirectLocation" to redirectLocation,
            "visitIdentifier" to visitIdentifier
        )

        if (visitIdentifier == currentVisit?.identifier) {
            callback { it.visitProposedToCrossOriginRedirect(redirectLocation) }
        }
    }

    /**
     * Called by Turbo bridge when a new visit proposal will refresh the
     * current page.
     *
     * Warning: This method is public so it can be used as a Javascript Interface.
     * You should never call this directly as it could lead to unintended behavior.
     *
     * @param location The location to visit.
     * @param optionsJson A JSON block to be serialized into [VisitOptions].
     */
    @JavascriptInterface
    fun visitProposalRefreshingPage(location: String, optionsJson: String) {
        val options = VisitOptions.fromJSON(optionsJson) ?: return
        logEvent("visitProposalRefreshingPage", "location" to location, "options" to options)
    }

    /**
     * Called by Turbo bridge when a new visit proposal will scroll to an anchor
     * on the same page.
     *
     * Warning: This method is public so it can be used as a Javascript Interface.
     * You should never call this directly as it could lead to unintended behavior.
     *
     * @param location The location to visit.
     * @param optionsJson A JSON block to be serialized into [VisitOptions].
     */
    @JavascriptInterface
    fun visitProposalScrollingToAnchor(location: String, optionsJson: String) {
        val options = VisitOptions.fromJSON(optionsJson) ?: return
        logEvent("visitProposalScrollingToAnchor", "location" to location, "options" to options)
    }

    /**
     * Called by Turbo bridge when a new visit has just started.
     *
     * Warning: This method is public so it can be used as a Javascript Interface.
     * You should never call this directly as it could lead to unintended behavior.
     *
     * @param visitIdentifier A unique identifier for the visit.
     * @param visitHasCachedSnapshot Whether the visit has a cached snapshot available.
     * @param location The location being visited.
     */
    @JavascriptInterface
    fun visitStarted(visitIdentifier: String, visitHasCachedSnapshot: Boolean,
                     visitIsPageRefresh: Boolean, location: String
    ) {
        logEvent(
            "visitStarted", "location" to location,
            "visitIdentifier" to visitIdentifier,
            "visitHasCachedSnapshot" to visitHasCachedSnapshot,
            "visitIsPageRefresh" to visitIsPageRefresh
        )

        currentVisit?.identifier = visitIdentifier
    }

    /**
     * Called by Turbo bridge when the HTTP request has started.
     *
     * @param visitIdentifier A unique identifier for the visit.
     */
    @JavascriptInterface
    fun visitRequestStarted(visitIdentifier: String) {
        logEvent("visitRequestStarted", "visitIdentifier" to visitIdentifier)
    }

    /**
     * Called by Turbo bridge when the HTTP request has been completed.
     *
     * @param visitIdentifier A unique identifier for the visit.
     */
    @JavascriptInterface
    fun visitRequestCompleted(visitIdentifier: String) {
        logEvent("visitRequestCompleted", "visitIdentifier" to visitIdentifier)
    }

    /**
     * Called by Turbo bridge when the HTTP request has failed.
     *
     * Warning: This method is public so it can be used as a Javascript Interface.
     * You should never call this directly as it could lead to unintended behavior.
     *
     * @param location The location of the failed visit.
     * @param visitIdentifier A unique identifier for the visit.
     * @param visitHasCachedSnapshot Whether the visit has a cached snapshot available.
     * @param statusCode The HTTP status code that caused the failure.
     */
    @JavascriptInterface
    fun visitRequestFailedWithStatusCode(
        location: String,
        visitIdentifier: String,
        visitHasCachedSnapshot: Boolean,
        statusCode: Int
    ) {
        val visitError = HttpError.from(statusCode)

        logEvent(
            "visitRequestFailedWithStatusCode",
            "location" to location,
            "visitIdentifier" to visitIdentifier,
            "visitHasCachedSnapshot" to visitHasCachedSnapshot,
            "error" to visitError
        )

        if (visitIdentifier == currentVisit?.identifier) {
            callback { it.requestFailedWithError(visitHasCachedSnapshot, visitError) }
        }
    }

    /**
     * Called by Turbo bridge when a visit request fails with a non-HTTP status code, suggesting
     * it may be the result of a cross-origin redirect visit. Determining a cross-origin redirect
     * is not possible in javascript with the Fetch API due to CORS restrictions, so verify on
     * the native side. Propose a cross-origin redirect visit if a redirect is found, otherwise
     * fail the visit.
     *
     * Warning: This method is public so it can be used as a Javascript Interface.
     * You should never call this directly as it could lead to unintended behavior.
     *
     * @param location The original visit location requested.
     * @param visitIdentifier A unique identifier for the visit.
     * @param visitHasCachedSnapshot Whether the visit has a cached snapshot available.
     */
    @JavascriptInterface
    fun visitRequestFailedWithNonHttpStatusCode(
        location: String,
        visitIdentifier: String,
        visitHasCachedSnapshot: Boolean
    ) {
        logEvent("visitRequestFailedWithNonHttpStatusCode",
            "location" to location,
            "visitIdentifier" to visitIdentifier,
            "visitHasCachedSnapshot" to visitHasCachedSnapshot
        )

        activity.lifecycleScope.launch {
            val result = httpRepository.fetch(location)

            if (result != null && result.response.isSuccessful &&
                result.redirect?.isCrossOrigin == true) {
                visitProposedToCrossOriginRedirect(
                    location = location,
                    redirectLocation = result.redirect.location,
                    visitIdentifier = visitIdentifier
                )
            } else {
                visitRequestFailedWithStatusCode(
                    location = location,
                    visitIdentifier = visitIdentifier,
                    visitHasCachedSnapshot = visitHasCachedSnapshot,
                    statusCode = WebError.Unknown.errorCode
                )
            }
        }
    }

    /**
     * Called by Turbo bridge when the HTTP request has been completed.
     *
     * Warning: This method is public so it can be used as a Javascript Interface.
     * You should never call this directly as it could lead to unintended behavior.
     *
     * @param visitIdentifier A unique identifier for the visit.
     */
    @JavascriptInterface
    fun visitRequestFinished(visitIdentifier: String) {
        logEvent("visitRequestFinished", "visitIdentifier" to visitIdentifier)
    }

    /**
     * Called by Turbo bridge once the page has been fully loaded by the WebView.
     *
     * Warning: This method is public so it can be used as a Javascript Interface.
     * You should never call this directly as it could lead to unintended behavior.
     *
     * @param restorationIdentifier A unique identifier for restoring the page and scroll position
     * from cache.
     */
    @JavascriptInterface
    fun pageLoaded(restorationIdentifier: String) {
        logEvent("pageLoaded", "restorationIdentifier" to restorationIdentifier)

        currentVisit?.let { visit ->
            restorationIdentifiers.put(visit.destinationIdentifier, restorationIdentifier)
        }
    }

    /**
     * Called by Turbo bridge once the page has been fully rendered in the webView.
     *
     * Warning: This method is public so it can be used as a Javascript Interface.
     * You should never call this directly as it could lead to unintended behavior.
     *
     * @param visitIdentifier A unique identifier for the visit.
     */
    @JavascriptInterface
    fun visitRendered(visitIdentifier: String) {
        logEvent("visitRendered", "visitIdentifier" to visitIdentifier)

        currentVisit?.let { visit ->
            if (visitIdentifier == coldBootVisitIdentifier || visitIdentifier == visit.identifier) {
                if (isFeatureSupported(VISUAL_STATE_CALLBACK)) {
                    postVisitVisualStateCallback(visitIdentifier)
                } else {
                    callback { it.visitRendered() }
                }
            }
        }
    }

    /**
     * Called by Turbo bridge when the visit is fully completed (request successful and
     * page rendered).
     *
     * Warning: This method is public so it can be used as a Javascript Interface.
     * You should never call this directly as it could lead to unintended behavior.
     *
     * @param visitIdentifier  A unique identifier for the visit.
     * @param restorationIdentifier A unique identifier for restoring the page and scroll position
     * from cache.
     */
    @JavascriptInterface
    fun visitCompleted(visitIdentifier: String, restorationIdentifier: String) {
        logEvent(
            "visitCompleted",
            "visitIdentifier" to visitIdentifier,
            "restorationIdentifier" to restorationIdentifier
        )

        currentVisit?.let { visit ->
            if (visitIdentifier == visit.identifier) {
                restorationIdentifiers.put(visit.destinationIdentifier, restorationIdentifier)
                callback { it.visitCompleted(visit.completedOffline) }
            }
        }
    }

    /**
     * Called by Turbo bridge when a form submission has started.
     *
     * Warning: This method is public so it can be used as a Javascript Interface.
     * You should never call this directly as it could lead to unintended behavior.
     *
     * @param location The location of the form submission.
     */
    @JavascriptInterface
    fun formSubmissionStarted(location: String) {
        logEvent(
            "formSubmissionStarted",
            "location" to location
        )

        currentVisit?.let {
            callback { it.formSubmissionStarted(location) }
        }
    }

    /**
     * Called by Turbo bridge when a form submission has finished.
     *
     * Warning: This method is public so it can be used as a Javascript Interface.
     * You should never call this directly as it could lead to unintended behavior.
     *
     * @param location The location of the form submission.
     */
    @JavascriptInterface
    fun formSubmissionFinished(location: String) {
        logEvent(
            "formSubmissionFinished",
            "location" to location
        )

        currentVisit?.let {
            callback { it.formSubmissionFinished(location) }
        }
    }

    /**
     * Called when Turbo bridge detects that the page being visited has been invalidated,
     * typically by new resources in the the page HEAD.
     *
     * Warning: This method is public so it can be used as a Javascript Interface.
     * You should never call this directly as it could lead to unintended behavior.
     *
     */
    @JavascriptInterface
    fun pageInvalidated() {
        logEvent("pageInvalidated")

        currentVisit?.let { visit ->
            callback {
                it.pageInvalidated()
                visit(visit.copy(reload = true))
            }
        }
    }

    /**
     * Sets internal flags that indicate whether Turbo in the WebView is ready for use.
     *
     * Warning: This method is public so it can be used as a Javascript Interface.
     * You should never call this directly as it could lead to unintended behavior.
     *
     * @param isReady
     */
    @JavascriptInterface
    fun turboIsReady(isReady: Boolean) {
        logEvent("turboIsReady", "isReady" to isReady)

        currentVisit?.let { visit ->
            this.isReady = isReady
            this.isColdBooting = false

            if (!isReady) {
                reset()

                val visitError = LoadError.NotReady
                logEvent("turboIsNotReady", "error" to visitError)

                callback { it.requestFailedWithError(false, visitError) }
                return
            }

            // Check if a visit was requested while cold
            // booting. If so, visit the pending location.
            when (visitPending) {
                true -> visitPendingLocation(visit)
                else -> renderVisitForColdBoot()
            }
        }
    }

    /**
     * Sets internal flags indicating that Turbo did not properly initialize.
     *
     * Warning: This method is public so it can be used as a Javascript Interface.
     * You should never call this directly as it could lead to unintended behavior.
     */
    @JavascriptInterface
    fun turboFailedToLoad() {
        val visitError = LoadError.NotPresent

        logEvent("turboFailedToLoad", "error" to visitError)
        reset()
        callback { it.onReceivedError(visitError) }
    }

    /**
     * Called when a touched element event has started.
     *
     * Warning: This method is public so it can be used as a Javascript Interface.
     * You should never call this directly as it could lead to unintended behavior.
     */
    @JavascriptInterface
    fun elementTouchStarted(scrollable: Boolean) {
        webView.elementTouchIsScrollable = scrollable
    }

    /**
     * Called when a touched element event has ended.
     *
     * Warning: This method is public so it can be used as a Javascript Interface.
     * You should never call this directly as it could lead to unintended behavior.
     */
    @JavascriptInterface
    fun elementTouchEnded() {
        webView.elementTouchIsScrollable = false
    }

    // Private

    private fun visitLocation(visit: Visit) {
        val restorationIdentifier = when (visit.options.action) {
            VisitAction.RESTORE -> restorationIdentifiers[visit.destinationIdentifier] ?: ""
            VisitAction.ADVANCE -> ""
            else -> ""
        }

        val options = when (restorationIdentifier) {
            "" -> visit.options.copy(action = VisitAction.ADVANCE)
            else -> visit.options
        }

        logEvent(
            "visitLocation",
            "location" to visit.location,
            "options" to options,
            "restorationIdentifier" to restorationIdentifier
        )

        webView.visitLocation(visit.location, options, restorationIdentifier)
    }

    private fun visitLocationAsColdBoot(visit: Visit) {
        logEvent("visitLocationAsColdBoot", "location" to visit.location)
        isColdBooting = true

        // When a page is invalidated by Turbo, we need to reload the
        // same URL in the WebView. For a URL with an anchor, the WebView
        // sees a WebView.loadUrl() request as a same-page visit instead of
        // requesting a full page reload. To work around this, we call
        // WebView.reload(), which fully reloads the page for all URLs.
        when (visit.reload) {
            true -> webView.reload()
            else -> webView.loadUrl(visit.location)
        }
    }

    private fun visitPendingLocation(visit: Visit) {
        logEvent("visitPendingLocation", "location" to visit.location)
        visitLocation(visit)
        visitPending = false
    }

    private fun renderVisitForColdBoot() {
        logEvent("renderVisitForColdBoot", "coldBootVisitIdentifier" to coldBootVisitIdentifier)
        webView.visitRenderedForColdBoot(coldBootVisitIdentifier)

        currentVisit?.let { visit ->
            callback { it.visitCompleted(visit.completedOffline) }
        }
    }

    /**
     * Updates to the DOM are processed asynchronously, so the changes may not immediately
     * be reflected visually by subsequent WebView.onDraw invocations. Use a VisualStateCallback
     * to be notified when the contents of the DOM are ready to be drawn.
     */
    private fun postVisitVisualStateCallback(visitIdentifier: String) {
        if (!isFeatureSupported(VISUAL_STATE_CALLBACK)) return

        context.runOnUiThread {
            WebViewCompat.postVisualStateCallback(webView, visitIdentifier.toRequestId()) { requestId ->
                logEvent("visitVisualStateComplete", "visitIdentifier" to visitIdentifier)

                if (visitIdentifier.toRequestId() == requestId) {
                    webView.requestLayout()
                    callback { it.visitRendered() }
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initializeWebView() {
        logEvent(
            "WebView info",
            "package" to (webView.packageName ?: ""),
            "version" to (webView.versionName ?: ""),
            "major version" to (webView.majorVersion ?: "")
        )

        webView.apply {
            addJavascriptInterface(this@Session, "TurboSession")
            webChromeClient = WebChromeClient()
            webViewClient = TurboWebViewClient()
            initDownloadListener()
        }
    }

    private fun WebView.initDownloadListener() {
        setDownloadListener { url, _, _, _, _ ->
            logEvent("downloadListener", "location" to url)
            visitProposedToLocation(url, VisitOptions().toJson())
        }
    }

    private fun installBridge(location: String) {
        logEvent("installBridge", "location" to location)

        webView.installBridge {
            callback { it.onPageFinished(location) }
        }
    }

    private fun String.toRequestId(): Long {
        return hashCode().toLong()
    }

    private fun callback(action: (SessionCallback) -> Unit) {
        context.runOnUiThread {
            currentVisit?.callback?.let { callback ->
                if (callback.visitDestination().isActive()) {
                    action(callback)
                }
            }
        }
    }

    private fun logEvent(event: String, vararg params: Pair<String, Any>) {
        val attributes = params.toMutableList().apply { add(0, "session" to sessionName) }
        logEvent(event, attributes)
    }


    // Classes and objects

    private inner class TurboWebViewClient : WebViewClientCompat() {
        private var initialScaleChanged = false
        private var initialScale = 0f

        override fun onPageStarted(view: WebView, location: String, favicon: Bitmap?) {
            logEvent("onPageStarted", "location" to location)
            callback { it.onPageStarted(location) }
            coldBootVisitIdentifier = ""
            currentVisit?.identifier = ""
        }

        override fun onPageFinished(view: WebView, location: String) {
            if (coldBootVisitIdentifier == location.identifier()) {
                // If we got here, onPageFinished() has already been called for
                // this location so bail. It's common for onPageFinished()
                // to be called multiple times when the document has initially
                // loaded and then when resources like images finish loading.
                return
            }

            if (!isColdBooting) {
                // We can get here even when the page failed to load. If
                // onReceivedError() or onReceivedHttpError() are called,
                // they reset the session, so we're no longer cold booting.
                return
            }

            logEvent("onPageFinished", "location" to location, "progress" to view.progress)
            coldBootVisitIdentifier = location.identifier()
            currentVisit?.identifier = coldBootVisitIdentifier
            installBridge(location)
        }

        override fun onPageCommitVisible(view: WebView, location: String) {
            super.onPageCommitVisible(view, location)
            logEvent("onPageCommitVisible", "location" to location, "progress" to view.progress)
        }

        override fun onScaleChanged(view: WebView, oldScale: Float, newScale: Float) {
            super.onScaleChanged(view, oldScale, newScale)
            logEvent("onScaleChanged", "oldScale" to oldScale, "newScale" to newScale)

            trackInitialScale(oldScale)

            if (isAtInitialScale(newScale)) {
                callback { it.onZoomReset(newScale) }
            } else {
                callback { it.onZoomed(newScale) }
            }
        }

        private fun trackInitialScale(scale: Float) {
            if (!initialScaleChanged) {
                initialScaleChanged = true
                initialScale = scale
            }
        }

        private fun isAtInitialScale(scale: Float): Boolean {
            return initialScale == scale
        }

        /**
         * Turbo will not call adapter.visitProposedToLocation in some cases,
         * like target=_blank or when the domain doesn't match. We still route those here.
         * This is only called when links within a webView are clicked and during a
         * redirect while cold booting.
         * http://stackoverflow.com/a/6739042/3280911
         */
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val location = request.url.toString()
            val isHttpRequest = request.isHttpGetRequest()
            val isColdBootRedirect = isHttpRequest && isColdBooting && currentVisit?.location != location
            val shouldOverride = isReady || isColdBootRedirect

            // Don't allow onPageFinished to process its
            // callbacks if a cold boot was blocked.
            if (isColdBootRedirect) {
                logEvent("coldBootRedirect", "location" to location)
                reset()
            }

            val willProposeVisit = isColdBootRedirect || shouldProposeThrottledVisit()
            if (shouldOverride && willProposeVisit) {
                // Replace the cold boot destination on a redirect
                // since the original url isn't visitable.
                val options = when (isColdBootRedirect) {
                    true -> VisitOptions(action = VisitAction.REPLACE)
                    else -> VisitOptions(action = VisitAction.ADVANCE)
                }
                visitProposedToLocation(location, options.toJson())
            }

            logEvent(
                "shouldOverrideUrlLoading",
                "location" to location,
                "shouldOverride" to shouldOverride,
                "isColdBootRedirect" to isColdBootRedirect,
                "willProposeVisit" to willProposeVisit
            )

            return shouldOverride
        }

        override fun onReceivedHttpAuthRequest(view: WebView, handler: HttpAuthHandler, host: String, realm: String) {
            callback { it.onReceivedHttpAuthRequest(handler, host, realm) }
        }

        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            return offlineRequestInterceptor.interceptRequest(request)
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceErrorCompat) {
            super.onReceivedError(view, request, error)

            if (request.isForMainFrame) {
                val visitError = WebError.from(error)

                logEvent("onReceivedError", "error" to visitError)
                reset()
                callback { it.onReceivedError(visitError) }
            }
        }

        override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
            super.onReceivedHttpError(view, request, errorResponse)

            if (request.isForMainFrame) {
                val visitError = HttpError.from(errorResponse)

                logEvent("onReceivedHttpError", "error" to visitError)
                reset()
                callback { it.onReceivedError(visitError) }
            }
        }

        override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
            super.onReceivedSslError(view, handler, error)
            handler.cancel()

            val visitError = WebSslError.from(error)
            logEvent("onReceivedSslError", "error" to visitError, "url" to error.url)

            if (currentVisit?.location == error.url) {
                reset()
                callback { it.onReceivedError(visitError) }
            }
        }

        override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
            logEvent("onRenderProcessGone", "didCrash" to detail.didCrash())

            if (view == webView) {
                // Set a flag if the WebView render process is gone so we
                // can avoid using this session any further in the app.
                isRenderProcessGone = true

                // Note: The render process can be gone even if a visit
                // hasn't been performed yet in this WebView instance.
                callback { it.onRenderProcessGone() }
            }

            return true
        }

        /**
         * Prevents firing twice in a row within a few milliseconds of each other, which
         * happens sometimes. So we check for a slight delay between requests, which is
         * plenty of time to allow for a user to click the same link again.
         */
        private fun shouldProposeThrottledVisit(): Boolean {
            val limit = 500
            val currentTime = Date().time

            return (currentTime - previousOverrideUrlTime > limit).also {
                previousOverrideUrlTime = currentTime
            }
        }

        private fun String.identifier(): String {
            return hashCode().toString()
        }
    }
}
