package dev.hotwire.core.bridge

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Test

class InternalMessageTest {
    @Serializable
    private data class Page(
        @SerialName("metadata") val metadata: InternalMetadata,
        @SerialName("title") val title: String,
        @SerialName("subtitle") val subtitle: String,
        @SerialName("actions") val actions: List<String>
    )

    private val json = """{
        "id":"1",
        "component":"page",
        "event":"connect",
        "data":{
            "metadata":{
                "url":"https://37signals.com"
            },
            "title":"Page-title",
            "subtitle":"Page-subtitle",
            "actions": [
                "one",
                "two",
                "three"
            ]
        }
    }""".replace("\\s".toRegex(), "")

    @Test
    fun toMessage() {
        val messageJsonData = """{"metadata":{"url":"https://37signals.com"},"title":"Page-title","subtitle":"Page-subtitle","actions":["one","two","three"]}"""
        val message = InternalMessage(
            id = "1",
            component = "page",
            event = "connect",
            data = createPage().toJsonElement()
        ).toMessage()

        assertEquals("1", message.id)
        assertEquals("page", message.component)
        assertEquals("connect", message.event)
        assertEquals("https://37signals.com", message.metadata?.url)
        assertEquals(messageJsonData, message.jsonData)
    }

    @Test
    fun toJson() {
        val message = InternalMessage(
            id = "1",
            component = "page",
            event = "connect",
            data = createPage().toJsonElement()
        )

        assertEquals(json, message.toJson())
    }

    @Test
    fun fromJson() {
        val message = InternalMessage.fromJson(json)
        val page = message?.data?.decode<Page>()

        assertEquals("1", message?.id)
        assertEquals("page", message?.component)
        assertEquals("connect", message?.event)
        assertEquals("Page-title", page?.title)
        assertEquals("Page-subtitle", page?.subtitle)
        assertEquals("one", page?.actions?.get(0))
        assertEquals("two", page?.actions?.get(1))
        assertEquals("three", page?.actions?.get(2))
    }

    @Test
    fun fromJsonNoData() {
        val noDataJson = """{"id":"1","component":"page","event":"connect","metadata":{"url":"https://37signals.com"}}"""
        val message = InternalMessage.fromJson(noDataJson)

        assertEquals("1", message?.id)
        assertEquals(0, message?.data?.jsonObject?.size)
    }

    private fun createPage(): Page {
        return Page(
            metadata = InternalMetadata(url = "https://37signals.com"),
            title = "Page-title",
            subtitle = "Page-subtitle",
            actions = listOf("one", "two", "three")
        )
    }
}
