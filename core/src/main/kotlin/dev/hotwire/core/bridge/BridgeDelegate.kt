package dev.hotwire.core.bridge

import android.webkit.WebView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import dev.hotwire.core.config.Hotwire
import dev.hotwire.core.logging.logDebug
import dev.hotwire.core.logging.logWarning

@Suppress("unused")
class BridgeDelegate<D : BridgeDestination>(
    val location: String,
    val startLocation: String,
    val destination: D,
    private val componentFactories: List<BridgeComponentFactory<D, BridgeComponent<D>>>
) : DefaultLifecycleObserver {
    internal var bridge: Bridge? = null
    private var destinationIsActive: Boolean = false
    private val resolvedLocation: String
        get() = bridge?.webView?.url ?: location

    val initializedComponents = hashMapOf<String, BridgeComponent<D>>()
    val activeComponents: List<BridgeComponent<D>>
        get() = initializedComponents.map { it.value }.takeIf { destinationIsActive }.orEmpty()

    fun onColdBootPageCompleted() {
        loadBridge()
    }

    fun onColdBootPageStarted() {
        bridge?.reset()
    }

    fun onWebViewAttached(webView: WebView) {
        bridge = Bridge.getBridgeFor(webView)?.apply {
            delegate = this@BridgeDelegate
        }

        if (bridge != null) {
            if (shouldReloadBridge()) {
                loadBridge()
            }
        } else {
            logWarning("bridgeNotInitializedForWebView", resolvedLocation)
        }
    }

    fun onWebViewDetached() {
        bridge?.delegate = null
        bridge = null
    }

    fun replyWith(message: Message): Boolean {
        if (!originIsTrustedForBridge()) {
            logBlockedForUntrustedOrigin("bridgeReplyBlockedForUntrustedOrigin")
            return false
        }

        bridge?.replyWith(message) ?: run {
            logWarning("bridgeMessageFailedToReply", "bridge is not available")
            return false
        }

        return true
    }

    internal fun bridgeDidInitialize() {
        if (!originIsTrustedForBridge()) {
            logBlockedForUntrustedOrigin("bridgeComponentRegistrationBlockedForUntrustedOrigin")
            return
        }

        bridge?.register(componentFactories.map { it.name })
    }

    internal fun bridgeDidReceiveMessage(message: Message): Boolean {
        return if (destinationIsActive &&
            resolvedLocation == message.metadata?.url &&
            originIsTrustedForBridge()
        ) {
            logDebug("bridgeDidReceiveMessage", message.toString())
            getOrCreateComponent(message.component)?.didReceive(message)
            true
        } else {
            logWarning("bridgeDidIgnoreMessage", message.toString())
            false
        }
    }

    private fun loadBridge() {
        if (!originIsTrustedForBridge()) {
            logBlockedForUntrustedOrigin("bridgeLoadBlockedForUntrustedOrigin")
            return
        }

        bridge?.load()
    }

    private fun shouldReloadBridge(): Boolean {
        return destination.bridgeWebViewIsReady() && bridge?.isReady() == false
    }

    private fun originIsTrustedForBridge(): Boolean {
        return Hotwire.config.hostVerifier.isTrustedForBridge(resolvedLocation, startLocation)
    }

    private fun logBlockedForUntrustedOrigin(event: String) {
        logWarning(event, listOf("location" to resolvedLocation, "startLocation" to startLocation))
    }

    // Lifecycle events

    override fun onStart(owner: LifecycleOwner) {
        logDebug("bridgeDestinationDidStart", resolvedLocation)
        destinationIsActive = true
        activeComponents.forEach { it.didStart() }
    }

    override fun onStop(owner: LifecycleOwner) {
        activeComponents.forEach { it.didStop() }
        destinationIsActive = false
        logDebug("bridgeDestinationDidStop", resolvedLocation)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        destinationIsActive = false
        logDebug("bridgeDestinationDidDestroy", resolvedLocation)
    }

    // Retrieve component(s) by type

    inline fun <reified C> component(): C? {
        return activeComponents.filterIsInstance<C>().firstOrNull()
    }

    inline fun <reified C> forEachInitializedComponent(action: (C) -> Unit) {
        initializedComponents.forEach { (_, component) ->
            if (component is C) {
                action(component)
            }
        }
    }

    inline fun <reified C> forEachActiveComponent(action: (C) -> Unit) {
        activeComponents.forEach { component ->
            if (component is C) {
                action(component)
            }
        }
    }

    private fun getOrCreateComponent(name: String): BridgeComponent<D>? {
        val factory = componentFactories.firstOrNull { it.name == name } ?: return null
        return initializedComponents.getOrPut(name) {
            factory.create(this).also {
                destination.onBridgeComponentInitialized(it)
            }
        }
    }
}
