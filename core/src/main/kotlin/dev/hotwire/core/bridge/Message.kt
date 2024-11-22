package dev.hotwire.core.bridge

data class Message constructor(
    /**
     * A unique identifier for this message. When you reply to the web with
     * a message, this identifier is used to find its previously sent message.
     */
    val id: String,

    /**
     * The component the message is sent from (e.g. - "form", "page", etc)
     */
    val component: String,

    /**
     * The event that this message is about: "submit", "display", "send"
     */
    val event: String,

    /**
     * The metadata associated with the message, which includes its url
     */
    val metadata: Metadata?,

    /**
     * Data, represented in a json object string, to send along with the message.
     * For a "page" component, this might be `{"title": "Page Title"}`
     */
    val jsonData: String
) {
    /**
     * Convenience method for creating a new message from an existing message,
     * replacing its `event` and/or `jsonData`. The new message can be sent
     * back to the over the bridge to notify the web of a change/action.
     */
    fun replacing(
        event: String = this.event,
        jsonData: String = this.jsonData
    ) = Message(
        id = this.id,
        component = this.component,
        event = event,
        metadata = this.metadata,
        jsonData = jsonData
    )

    inline fun <reified T> replacing(
        event: String = this.event,
        data: T
    ): Message {
        return replacing(event, BridgeComponentJsonConverter.toJson(data))
    }

    inline fun <reified T> data(): T? {
        return BridgeComponentJsonConverter.toObject(jsonData)
    }
}

data class Metadata(
    val url: String
)
