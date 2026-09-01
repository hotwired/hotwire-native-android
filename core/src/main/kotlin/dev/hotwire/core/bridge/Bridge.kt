package dev.hotwire.core.bridge

import android.webkit.WebView
import androidx.annotation.VisibleForTesting
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import androidx.webkit.WebViewFeature.WEB_MESSAGE_LISTENER
import dev.hotwire.core.config.Hotwire
import dev.hotwire.core.logging.logDebug
import dev.hotwire.core.logging.logError
import dev.hotwire.core.logging.logVerbose
import dev.hotwire.core.logging.logWarning
import dev.hotwire.core.turbo.util.JavascriptMessage
import dev.hotwire.core.turbo.util.string
import dev.hotwire.core.turbo.util.toJavascriptMessageOrNull
import kotlinx.serialization.json.JsonElement
import java.lang.ref.WeakReference

// These need to match whatever is set in bridge_components.js
private const val bridgeGlobal = "window.nativeBridge"
private const val bridgeChannelName = "BridgeComponentsChannel"

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

        // The channel must be added before the page is loaded
        initBridgeChannel(webView)
    }

    internal fun register(component: String) {
        logDebug("bridgeWillRegisterComponent", component)
        val javascript = generateJavaScript("register", component.toJsonElement())
        evaluate(javascript)
    }

    internal fun register(components: List<String>) {
        logDebug("bridgeWillRegisterComponents", components.joinToString())
        val javascript = generateJavaScript("register", components.toJsonElement())
        evaluate(javascript)
    }

    internal fun unregister(component: String) {
        logDebug("bridgeWillUnregisterComponent", component)
        val javascript = generateJavaScript("unregister", component.toJsonElement())
        evaluate(javascript)
    }

    internal fun replyWith(message: Message) {
        logDebug("bridgeWillReplyWithMessage", message.toString())
        val internalMessage = InternalMessage.fromMessage(message)
        val javascript = generateJavaScript("replyWith", internalMessage.toJson().toJsonElement())
        evaluate(javascript)
    }

    internal fun load() {
        logDebug("bridgeWillLoad")
        evaluate(userScript())
    }

    internal fun reset() {
        logDebug("bridgeDidReset")
        componentsAreRegistered = false
    }

    internal fun isReady(): Boolean {
        return componentsAreRegistered
    }

    private fun initBridgeChannel(webView: WebView) {
        if (!WebViewFeature.isFeatureSupported(WEB_MESSAGE_LISTENER)) {
            logError(
                "webMessageListenerNotSupported",
                "The WebView version on this device is not supported"
            )
            return
        }

        // "*" injects the channel into every frame. Each message is gated on
        // its browser-reported source origin instead, which a page can't forge.
        WebViewCompat.addWebMessageListener(webView, bridgeChannelName, setOf("*")) {
            _, message, sourceOrigin, isMainFrame, _ ->
            onBridgeMessage(message.data.orEmpty(), sourceOrigin.toString(), isMainFrame)
        }
    }

    /**
     * Handles a message posted through the BridgeComponentsChannel. Messages
     * can originate from any frame of any page loaded in the WebView, so each
     * one is gated on its source origin before it is even decoded. Runs on
     * the main thread — the message listener delivers there.
     */
    internal fun onBridgeMessage(data: String, sourceOrigin: String, isMainFrame: Boolean) {
        if (!isMainFrame || !Hotwire.config.hostVerifier.isTrustedForBridge(sourceOrigin)) {
            logWarning("bridgeMessageBlockedForUntrustedOrigin", listOf("origin" to sourceOrigin))
            return
        }

        val message = data.toJavascriptMessageOrNull() ?: run {
            logWarning("bridgeMessageMalformed", "")
            return
        }

        try {
            dispatchBridgeMessage(message)
        } catch (e: RuntimeException) {
            logError("bridgeMessageFailed", e)
        }
    }

    private fun dispatchBridgeMessage(message: JavascriptMessage) {
        when (message.name) {
            "bridgeDidInitialize" -> bridgeDidInitialize()
            "bridgeDidUpdateSupportedComponents" -> bridgeDidUpdateSupportedComponents()
            "bridgeDidReceiveMessage" -> bridgeDidReceiveMessage(message.args.string(0))
            else -> logWarning("bridgeMessageUnknown", listOf("name" to message.name))
        }
    }

    private fun bridgeDidInitialize() {
        logDebug("bridgeDidInitialize", "success")
        delegate?.bridgeDidInitialize()
    }

    private fun bridgeDidUpdateSupportedComponents() {
        logDebug("bridgeDidUpdateSupportedComponents", "success")
        componentsAreRegistered = true
    }

    private fun bridgeDidReceiveMessage(message: String) {
        InternalMessage.fromJson(message)?.let {
            delegate?.bridgeDidReceiveMessage(it.toMessage())
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
