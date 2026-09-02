package dev.hotwire.core.bridge

import android.content.Context
import android.os.Build
import android.webkit.WebView
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.whenever
import dev.hotwire.core.config.Hotwire
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class BridgeTest {
    private lateinit var bridge: Bridge
    private val webView: WebView = mock()
    private val context: Context = mock()
    private val repository: Repository = mock()
    private val delegate: BridgeDelegate<TestData.AppBridgeDestination> = mock()

    private val bridgeDidReceiveMessageEnvelope = buildJsonObject {
        put("name", "bridgeDidReceiveMessage")
        putJsonArray("args") {
            add("""{"id":"1","component":"page","event":"connect","data":{"metadata":{"url":"https://37signals.com"},"title":"Page title","subtitle":"Page subtitle"}}""")
        }
    }.toString()

    @Before
    fun setup() {
        Hotwire.config.clearTrustedLocations()
        Hotwire.config.registerTrustedLocation("https://37signals.com")

        bridge = Bridge(webView)
        bridge.delegate = delegate
        bridge.repository = repository
    }

    @After
    fun teardown() {
        Hotwire.config.clearTrustedLocations()
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
        verify(webView).evaluateJavascript(eq(""), any())
    }

    @Test
    fun bridgeDidInitialize() {
        bridge.onBridgeMessage(
            data = """{"name":"bridgeDidInitialize","args":[]}""",
            sourceOrigin = "https://37signals.com",
            isMainFrame = true
        )

        verify(delegate).bridgeDidInitialize()
    }

    @Test
    fun bridgeDidReceiveMessage() {
        val data = """{"metadata":{"url":"https://37signals.com"},"title":"Page title","subtitle":"Page subtitle"}"""
        val message = Message(
            id = "1",
            component = "page",
            event = "connect",
            metadata = Metadata("https://37signals.com"),
            jsonData = data
        )

        bridge.onBridgeMessage(
            data = bridgeDidReceiveMessageEnvelope,
            sourceOrigin = "https://37signals.com",
            isMainFrame = true
        )

        verify(delegate).bridgeDidReceiveMessage(message)
    }

    @Test
    fun bridgeMessagesFromAnUntrustedOriginAreDropped() {
        bridge.onBridgeMessage(
            data = bridgeDidReceiveMessageEnvelope,
            sourceOrigin = "https://evil.attacker.com",
            isMainFrame = true
        )
        bridge.onBridgeMessage(
            data = """{"name":"bridgeDidInitialize","args":[]}""",
            sourceOrigin = "https://evil.attacker.com",
            isMainFrame = true
        )

        verify(delegate, never()).bridgeDidReceiveMessage(any())
        verify(delegate, never()).bridgeDidInitialize()
    }

    @Test
    fun bridgeMessagesFromASubFrameAreDropped() {
        bridge.onBridgeMessage(
            data = bridgeDidReceiveMessageEnvelope,
            sourceOrigin = "https://37signals.com",
            isMainFrame = false
        )

        verify(delegate, never()).bridgeDidReceiveMessage(any())
    }

    @Test
    fun malformedBridgeMessagesAreDropped() {
        listOf(
            "not json",
            """{"args":[]}""",
            """{"name":"noSuchMethod","args":[]}""",
            """{"name":"bridgeDidReceiveMessage","args":[]}"""
        ).forEach {
            bridge.onBridgeMessage(it, sourceOrigin = "https://37signals.com", isMainFrame = true)
        }

        verify(delegate, never()).bridgeDidReceiveMessage(any())
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
