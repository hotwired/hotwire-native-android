package dev.hotwire.core.bridge

import android.webkit.WebView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import dev.hotwire.core.config.Hotwire
import dev.hotwire.core.lib.logging.logEvent
import dev.hotwire.core.lib.logging.logWarning
import dev.hotwire.core.turbo.nav.HotwireNavDestination

@Suppress("unused")
class BridgeDelegate(
    val location: String,
    val destination: HotwireNavDestination
) : DefaultLifecycleObserver {
    internal var bridge: Bridge? = null
    private var destinationIsActive: Boolean = false
    private val componentFactories = Hotwire.registeredBridgeComponentFactories
    private val initializedComponents = hashMapOf<String, BridgeComponent>()
    private val resolvedLocation: String
        get() = bridge?.webView?.url ?: location

    val activeComponents: List<BridgeComponent>
        get() = initializedComponents.map { it.value }.takeIf { destinationIsActive }.orEmpty()

    fun onColdBootPageCompleted() {
        bridge?.load()
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
                bridge?.load()
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
        bridge?.replyWith(message) ?: run {
            logWarning("bridgeMessageFailedToReply", "bridge is not available")
            return false
        }

        return true
    }

    internal fun bridgeDidInitialize() {
        bridge?.register(componentFactories.map { it.name })
    }

    internal fun bridgeDidReceiveMessage(message: Message): Boolean {
        return if (destinationIsActive && resolvedLocation == message.metadata?.url) {
            logEvent("bridgeDidReceiveMessage", message.toString())
            getOrCreateComponent(message.component)?.didReceive(message)
            true
        } else {
            logWarning("bridgeDidIgnoreMessage", message.toString())
            false
        }
    }

    private fun shouldReloadBridge(): Boolean {
        return destination.session.isReady && bridge?.isReady() == false
    }

    // Lifecycle events

    override fun onStart(owner: LifecycleOwner) {
        logEvent("bridgeDestinationDidStart", resolvedLocation)
        destinationIsActive = true
        activeComponents.forEach { it.didStart() }
    }

    override fun onStop(owner: LifecycleOwner) {
        activeComponents.forEach { it.didStop() }
        destinationIsActive = false
        logEvent("bridgeDestinationDidStop", resolvedLocation)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        destinationIsActive = false
        logEvent("bridgeDestinationDidDestroy", resolvedLocation)
    }

    // Retrieve component(s) by type

    inline fun <reified C> component(): C? {
        return activeComponents.filterIsInstance<C>().firstOrNull()
    }

    inline fun <reified C> forEachComponent(action: (C) -> Unit) {
        activeComponents.filterIsInstance<C>().forEach { action(it) }
    }

    private fun getOrCreateComponent(name: String): BridgeComponent? {
        val factory = componentFactories.firstOrNull { it.name == name } ?: return null
        return initializedComponents.getOrPut(name) { factory.create(this) }
    }
}
