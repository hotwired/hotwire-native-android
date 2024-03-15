package dev.hotwire.core.bridge

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class InternalMessage(
    @SerialName("id") val id: String,
    @SerialName("component") val component: String,
    @SerialName("event") val event: String,
    @SerialName("data") val data: JsonElement = "{}".parseToJsonElement()
) {
    fun toMessage() = Message(
        id = id,
        component = component,
        event = event,
        metadata = data.decode<InternalDataMetadata>()?.let { Metadata(url = it.metadata.url) },
        jsonData = data.toJson()
    )

    companion object {
        fun fromMessage(message: Message) = InternalMessage(
            id = message.id,
            component = message.component,
            event = message.event,
            data = message.jsonData.parseToJsonElement()
        )

        fun fromJson(json: String?) = json?.decode<InternalMessage>()
    }
}

@Serializable
internal data class InternalDataMetadata(
    @SerialName("metadata") val metadata: InternalMetadata
)

@Serializable
internal data class InternalMetadata(
    @SerialName("url") val url: String
)
