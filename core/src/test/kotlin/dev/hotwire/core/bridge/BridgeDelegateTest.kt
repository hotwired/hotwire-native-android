package dev.hotwire.core.bridge

import android.webkit.WebView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.verify

class BridgeDelegateTest {
    private lateinit var delegate: BridgeDelegate<TestData.AppBridgeDestination>
    private lateinit var lifecycleOwner: TestLifecycleOwner
    private val destination: TestData.AppBridgeDestination = mock()
    private val bridge: Bridge = mock()
    private val webView: WebView = mock()

    private val factories = listOf(
        BridgeComponentFactory("one", TestData::OneBridgeComponent),
        BridgeComponentFactory("two", TestData::TwoBridgeComponent)
    )

    @Rule
    @JvmField
    var coroutinesTestRule = CoroutinesTestRule()

    @Before
    fun setup() {
        whenever(destination.bridgeWebViewIsReady()).thenReturn(true)
        whenever(bridge.webView).thenReturn(webView)
        Bridge.initialize(bridge)

        delegate = BridgeDelegate(
            location = "https://37signals.com",
            destination = destination,
            componentFactories = factories
        )
        delegate.bridge = bridge

        lifecycleOwner = TestLifecycleOwner(Lifecycle.State.STARTED)
        lifecycleOwner.lifecycle.addObserver(delegate)
    }

    @Test
    fun onColdBootPageCompleted() {
        delegate.onColdBootPageCompleted()
        verify(bridge).load()
    }

    @Test
    fun onColdBootPageStarted() {
        delegate.onColdBootPageStarted()
        verify(bridge).reset()
    }

    @Test
    fun bridgeDidInitialize() {
        delegate.bridgeDidInitialize()
        verify(bridge).register(eq(listOf("one", "two")))
    }

    @Test
    fun bridgeDidReceiveMessage() {
        val message = Message(
            id = "1",
            component = "one",
            event = "connect",
            metadata = Metadata("https://37signals.com"),
            jsonData = """{"title":"Page-title","subtitle":"Page-subtitle"}"""
        )

        assertNull(delegate.component<TestData.OneBridgeComponent>())
        assertEquals(true, delegate.bridgeDidReceiveMessage(message))
        assertNotNull(delegate.component<TestData.OneBridgeComponent>())
    }

    @Test
    fun bridgeNotifiesWhenComponentIsInitialized() {
        val message = Message(
            id = "1",
            component = "one",
            event = "connect",
            metadata = Metadata("https://37signals.com"),
            jsonData = """{"title":"Page-title","subtitle":"Page-subtitle"}"""
        )

        delegate.bridgeDidReceiveMessage(message)
        delegate.bridgeDidReceiveMessage(message)
        verify(destination, times(1)).onBridgeComponentInitialized(any())
    }

    @Test
    fun bridgeDidReceiveMessageForLocationWithTrailingSlash() {
        whenever(webView.url).thenReturn("https://37signals.com/")

        val message = Message(
            id = "1",
            component = "one",
            event = "connect",
            metadata = Metadata("https://37signals.com/"),
            jsonData = """{"title":"Page-title","subtitle":"Page-subtitle"}"""
        )

        assertNull(delegate.component<TestData.OneBridgeComponent>())
        assertEquals(true, delegate.bridgeDidReceiveMessage(message))
        assertNotNull(delegate.component<TestData.OneBridgeComponent>())
    }

    @Test
    fun bridgeDidReceiveMessageForResolvedLocation() {
        whenever(webView.url).thenReturn("https://37signals.com/new_url")

        val message = Message(
            id = "1",
            component = "one",
            event = "connect",
            metadata = Metadata("https://37signals.com/new_url"),
            jsonData = """{"title":"Page-title","subtitle":"Page-subtitle"}"""
        )

        assertNull(delegate.component<TestData.OneBridgeComponent>())
        assertEquals(true, delegate.bridgeDidReceiveMessage(message))
        assertNotNull(delegate.component<TestData.OneBridgeComponent>())
    }

    @Test
    fun bridgeDidReceiveMessageIgnored() {
        val message = Message(
            id = "1",
            component = "page",
            event = "connect",
            metadata = Metadata("https://37signals.com/another_url"),
            jsonData = """{"title":"Page-title","subtitle":"Page-subtitle"}"""
        )

        assertEquals(false, delegate.bridgeDidReceiveMessage(message))
    }

    @Test
    fun replyWith() {
        val message = Message(
            id = "1",
            component = "page",
            event = "connect",
            metadata = Metadata("https://37signals.com/another_url"),
            jsonData = """{"title":"Page-title","subtitle":"Page-subtitle"}"""
        )

        assertEquals(true, delegate.replyWith(message))
    }

    @Test
    fun replyWithFailsWithoutBridge() {
        val message = Message(
            id = "1",
            component = "page",
            event = "connect",
            metadata = Metadata("https://37signals.com/another_url"),
            jsonData = """{"title":"Page-title","subtitle":"Page-subtitle"}"""
        )

        delegate.bridge = null
        assertEquals(false, delegate.replyWith(message))
    }

    @Test
    fun onWebViewAttached() {
        whenever(bridge.isReady()).thenReturn(false)
        delegate.onWebViewAttached(webView)

        assertEquals(delegate.bridge, bridge)
    }

    @Test
    fun onWebViewAttachedShouldLoad() {
        whenever(bridge.isReady()).thenReturn(false)
        delegate.onWebViewAttached(webView)

        verify(bridge).load()
    }

    @Test
    fun onWebViewAttachedShouldNotLoad() {
        whenever(bridge.isReady()).thenReturn(true)
        delegate.onWebViewAttached(webView)

        verify(bridge, never()).load()
    }

    @Test
    fun onWebViewDetached() {
        delegate.onWebViewDetached()

        assertNull(delegate.bridge?.delegate)
        assertNull(delegate.bridge)
    }

    @Test
    fun destinationIsInactive() {
        val message = Message(
            id = "1",
            component = "one",
            event = "connect",
            metadata = Metadata("https://37signals.com"),
            jsonData = """{"title":"Page-title","subtitle":"Page-subtitle"}"""
        )

        assertEquals(true, delegate.bridgeDidReceiveMessage(message))
        assertNotNull(delegate.component<TestData.OneBridgeComponent>())

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        assertEquals(false, delegate.bridgeDidReceiveMessage(message))
        assertNull(delegate.component<TestData.OneBridgeComponent>())
    }
}
