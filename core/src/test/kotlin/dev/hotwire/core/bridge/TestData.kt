package dev.hotwire.core.bridge

object TestData {
    val componentFactories = listOf(
        BridgeComponentFactory("one", TestData::OneBridgeComponent),
        BridgeComponentFactory("two", TestData::TwoBridgeComponent)
    )

    val bridgeDelegate = BridgeDelegate(
        location = "https://37signals.com",
        destination = AppBridgeDestination(),
        componentFactories = componentFactories
    )

    class AppBridgeDestination : BridgeDestination {
        override fun bridgeWebViewIsReady() = true
    }

    abstract class AppBridgeComponent(
        name: String,
        delegate: BridgeDelegate<AppBridgeDestination>
    ) : BridgeComponent<AppBridgeDestination>(name, delegate)

    class OneBridgeComponent(
        name: String,
        delegate: BridgeDelegate<AppBridgeDestination>
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
        delegate: BridgeDelegate<AppBridgeDestination>
    ) : AppBridgeComponent(name, delegate) {
        override fun onReceive(message: Message) {}
    }
}