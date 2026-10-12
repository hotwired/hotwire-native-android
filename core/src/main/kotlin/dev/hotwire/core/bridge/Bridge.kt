package dev.hotwire.core.bridge

import android.webkit.WebView
import androidx.annotation.VisibleForTesting
import dev.hotwire.core.logging.logDebug
import dev.hotwire.core.logging.logVerbose
import dev.hotwire.core.logging.logWarning
import dev.hotwire.core.security.JavascriptChannel
import dev.hotwire.core.security.JavascriptMessage
import dev.hotwire.core.security.stringAt
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
    internal val channel = JavascriptChannel(bridgeChannelName, ::dispatchBridgeMessage)

    init {
        // Use a weak reference in case the WebView is no longer being
        // used by the app, such as when the render process is gone.
        webViewRef = WeakReference(webView)

        channel.install(webView)
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

    private fun dispatchBridgeMessage(message: JavascriptMessage) {
        when (message.name) {
            "bridgeDidInitialize" -> bridgeDidInitialize()
            "bridgeDidUpdateSupportedComponents" -> bridgeDidUpdateSupportedComponents()
            "bridgeDidReceiveMessage" -> bridgeDidReceiveMessage(message.args.stringAt(0))
            else -> logWarning(
                "javascriptMessageUnknown",
                listOf("channel" to bridgeChannelName, "name" to message.name)
            )
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
