package dev.hotwire.core.bridge

import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.annotation.VisibleForTesting
import dev.hotwire.core.logging.logEvent
import dev.hotwire.core.logging.logVerbose
import kotlinx.serialization.json.JsonElement
import java.lang.ref.WeakReference

// These need to match whatever is set in bridge_components.js
private const val bridgeGlobal = "window.nativeBridge"
private const val bridgeJavascriptInterface = "BridgeComponentsNative"

@Suppress("unused")
class Bridge internal constructor(webView: WebView) {
    private var componentsAreRegistered: Boolean = false
    private val webViewRef: WeakReference<WebView>

    internal val webView: WebView? get() = webViewRef.get()
    internal var repository = Repository()
    internal var delegate: BridgeDelegate<*>? = null

    init {
        // Use a weak reference in case the WebView is no longer being
        // used by the app, such as when the render process is gone.
        webViewRef = WeakReference(webView)

        // The JavascriptInterface must be added before the page is loaded
        webView.addJavascriptInterface(this, bridgeJavascriptInterface)
    }

    internal fun register(component: String) {
        logEvent("bridgeWillRegisterComponent", component)
        val javascript = generateJavaScript("register", component.toJsonElement())
        evaluate(javascript)
    }

    internal fun register(components: List<String>) {
        logEvent("bridgeWillRegisterComponents", components.joinToString())
        val javascript = generateJavaScript("register", components.toJsonElement())
        evaluate(javascript)
    }

    internal fun unregister(component: String) {
        logEvent("bridgeWillUnregisterComponent", component)
        val javascript = generateJavaScript("unregister", component.toJsonElement())
        evaluate(javascript)
    }

    internal fun replyWith(message: Message) {
        logEvent("bridgeWillReplyWithMessage", message.toString())
        val internalMessage = InternalMessage.fromMessage(message)
        val javascript = generateJavaScript("replyWith", internalMessage.toJson().toJsonElement())
        evaluate(javascript)
    }

    internal fun load() {
        logEvent("bridgeWillLoad")
        evaluate(userScript())
    }

    internal fun reset() {
        logEvent("bridgeDidReset")
        componentsAreRegistered = false
    }

    internal fun isReady(): Boolean {
        return componentsAreRegistered
    }

    @JavascriptInterface
    fun bridgeDidInitialize() {
        logEvent("bridgeDidInitialize", "success")
        runOnUiThread {
            delegate?.bridgeDidInitialize()
        }
    }

    @JavascriptInterface
    fun bridgeDidUpdateSupportedComponents() {
        logEvent("bridgeDidUpdateSupportedComponents", "success")
        componentsAreRegistered = true
    }

    @JavascriptInterface
    fun bridgeDidReceiveMessage(message: String?) {
        runOnUiThread {
            InternalMessage.fromJson(message)?.let {
                delegate?.bridgeDidReceiveMessage(it.toMessage())
            }
        }
    }

    // Internal

    internal fun userScript(): String {
        val context = requireNotNull(webView?.context)
        return repository.getUserScript(context)
    }

    internal fun evaluate(javascript: String) {
        logVerbose("evaluatingJavascript", javascript)
        webView?.evaluateJavascript(javascript) {}
    }

    internal fun generateJavaScript(bridgeFunction: String, vararg arguments: JsonElement): String {
        val functionName = sanitizeFunctionName(bridgeFunction)
        val encodedArguments = encode(arguments.toList())
        return "$bridgeGlobal.$functionName($encodedArguments)"
    }

    internal fun encode(arguments: List<JsonElement>): String {
        return arguments.joinToString(",") { it.toJson() }
    }

    internal fun sanitizeFunctionName(name: String): String {
        return name.removeSuffix("()")
    }

    companion object {
        private val instances = mutableListOf<Bridge>()

        fun initialize(webView: WebView) {
            if (getBridgeFor(webView) == null) {
                initialize(Bridge(webView))
            }
        }

        @VisibleForTesting
        internal fun initialize(bridge: Bridge) {
            instances.add(bridge)
            instances.removeIf { it.webView == null }
        }

        internal fun getBridgeFor(webView: WebView): Bridge? {
            return instances.firstOrNull { it.webView == webView }
        }
    }
}
