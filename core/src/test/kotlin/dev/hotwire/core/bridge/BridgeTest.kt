package dev.hotwire.core.bridge

import android.content.Context
import android.webkit.WebView
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.verify

class BridgeTest {
    private lateinit var bridge: Bridge
    private val webView: WebView = mock()
    private val context: Context = mock()
    private val repository: Repository = mock()
    private val delegate: BridgeDelegate = mock()

    @Before
    fun setup() {
        bridge = Bridge(webView)
        bridge.delegate = delegate
        bridge.repository = repository
    }

    @Test
    fun registerComponent() {
        val javascript = """window.nativeBridge.register("page")"""
        bridge.register("page")
        verify(webView).evaluateJavascript(eq(javascript), any())
    }

    @Test
    fun registerComponents() {
        val javascript = """window.nativeBridge.register(["page","alert"])"""
        bridge.register(listOf("page", "alert"))
        verify(webView).evaluateJavascript(eq(javascript), any())
    }

    @Test
    fun unregisterComponent() {
        val javascript = """window.nativeBridge.unregister("page")"""
        bridge.unregister("page")
        verify(webView).evaluateJavascript(eq(javascript), any())
    }

    @Test
    fun replyWith() {
        val json = """{\"id\":\"1\",\"component\":\"page\",\"event\":\"connect\",\"data\":{\"title\":\"Page title\",\"subtitle\":\"Page subtitle\",\"html\":\"<span class='android'>content</span>\"}}"""
        val data = """{"title":"Page title","subtitle":"Page subtitle","html":"<span class='android'>content</span>"}"""
        val message = Message(
            id = "1",
            component = "page",
            event = "connect",
            metadata = Metadata("https://37signals.com"),
            jsonData = data
        )

        val javascript = """window.nativeBridge.replyWith("$json")"""
        bridge.replyWith(message)
        verify(webView).evaluateJavascript(eq(javascript), any())
    }

    @Test
    fun load() {
        whenever(webView.context).thenReturn(context)
        whenever(repository.getUserScript(context)).thenReturn("")

        bridge.load()
        verify(webView).addJavascriptInterface(eq(bridge), any())
    }

    @Test
    fun bridgeDidInitialize() {
        bridge.bridgeDidInitialize()
        verify(delegate).bridgeDidInitialize()
    }

    @Test
    fun bridgeDidReceiveMessage() {
        val json = """{"id":"1","component":"page","event":"connect","data":{"metadata":{"url":"https://37signals.com"},"title":"Page title","subtitle":"Page subtitle"}}"""
        val data = """{"metadata":{"url":"https://37signals.com"},"title":"Page title","subtitle":"Page subtitle"}"""
        val message = Message(
            id = "1",
            component = "page",
            event = "connect",
            metadata = Metadata("https://37signals.com"),
            jsonData = data
        )

        bridge.bridgeDidReceiveMessage(json)
        verify(delegate).bridgeDidReceiveMessage(message)
    }

    @Test
    fun userScript() {
        whenever(webView.context).thenReturn(context)

        bridge.userScript()
        verify(repository).getUserScript(context)
    }

    @Test
    fun evaluate() {
        val javascript = """window.nativeBridge.register("page")"""
        bridge.evaluate(javascript)
        verify(webView).evaluateJavascript(eq(javascript), any())
    }

    @Test
    fun generateJavascript() {
        val javascript = bridge.generateJavaScript("register", "page".toJsonElement())
        assertEquals("""window.nativeBridge.register("page")""", javascript)
    }

    @Test
    fun generateJavascriptArguments() {
        val javascript = bridge.generateJavaScript("register", listOf("page", "alert").toJsonElement())
        assertEquals("""window.nativeBridge.register(["page","alert"])""", javascript)
    }

    @Test
    fun encode() {
        assertEquals("\"page\"", bridge.encode(listOf("page".toJsonElement())))

        val argument = listOf("page", "alert").toJsonElement()
        assertEquals("[\"page\",\"alert\"]", bridge.encode(listOf(argument)))
    }

    @Test
    fun sanitizeFunctionName() {
        assertEquals(bridge.sanitizeFunctionName("replyWith()"), "replyWith")
    }
}
