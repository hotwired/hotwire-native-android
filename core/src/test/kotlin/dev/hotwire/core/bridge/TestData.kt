package dev.hotwire.core.bridge

import dev.hotwire.core.turbo.nav.HotwireNavDestination
import org.mockito.Mockito.mock

object TestData {
    private val mockNavDestination: HotwireNavDestination = mock()

    val componentFactories = listOf(
        BridgeComponentFactory("one", TestData::OneBridgeComponent),
        BridgeComponentFactory("two", TestData::TwoBridgeComponent)
    )

    val bridgeDelegate = BridgeDelegate(
        location = "https://37signals.com",
        destination = mockNavDestination
    )

    abstract class AppBridgeComponent(
        name: String,
        delegate: BridgeDelegate
    ) : BridgeComponent(name, delegate)

    class OneBridgeComponent(
        name: String,
        delegate: BridgeDelegate
    ) : AppBridgeComponent(name, delegate) {
        var onStartCalled = false
        var onStopCalled = false

        override fun onStart() {
            onStartCalled = true
        }

        override fun onStop() {
            onStopCalled = true
        }

        override fun onReceive(message: Message) {}

        fun receivedMessageForPublic(event: String): Message? {
            return receivedMessageFor(event)
        }
    }

    class TwoBridgeComponent(
        name: String,
        delegate: BridgeDelegate
    ) : AppBridgeComponent(name, delegate) {
        override fun onReceive(message: Message) {}
    }
}