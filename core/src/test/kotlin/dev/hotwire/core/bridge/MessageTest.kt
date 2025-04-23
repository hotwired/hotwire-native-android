package dev.hotwire.core.bridge

import dev.hotwire.core.config.Hotwire
import kotlinx.serialization.Serializable
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class MessageTest {

    @Before
    fun setup() {
        Hotwire.config.jsonConverter = KotlinXJsonConverter()
    }

    @Test
    fun dataDecodesToObject() {
        val metadata = Metadata("https://37signals.com")
        val message = Message(
            id = "1",
            component = "page",
            event = "connect",
            metadata = metadata,
            jsonData = """{"title":"Page-title","subtitle":"Page-subtitle"}"""
        )

        val data = message.data<MessageData>()

        assertEquals("Page-title", data?.title)
        assertEquals("Page-subtitle", data?.subtitle)
    }

    @Test
    fun dataDoesNotDecodeToInvalidObject() {
        val metadata = Metadata("https://37signals.com")
        val message = Message(
            id = "1",
            component = "page",
            event = "connect",
            metadata = metadata,
            jsonData = """{"title":"Page-title","subtitle":"Page-subtitle"}"""
        )

        val data = message.data<InvalidMessageData>()

        assertNull(data)
    }

    @Test
    fun dataWithNullableFieldsDecodesToObject() {
        val metadata = Metadata("https://37signals.com")
        val message = Message(
            id = "1",
            component = "page",
            event = "connect",
            metadata = metadata,
            jsonData = """{"title":null}""" // subtitle missing
        )

        val data = message.data<NullableMessageData>()

        assertNotNull(data)
        assertEquals(null, data?.title)
        assertEquals(null, data?.subtitle)
    }

    @Test
    fun dataWithDefaultValuesDecodesToObject() {
        val metadata = Metadata("https://37signals.com")
        val message = Message(
            id = "1",
            component = "page",
            event = "connect",
            metadata = metadata,
            jsonData = """{}""" // title and subtitle missing
        )

        val data = message.data<DefaultValuesMessageData>()

        assertNotNull(data)
        assertEquals("Page-title", data?.title)
        assertEquals("Page-subtitle", data?.subtitle)
    }

    @Test
    fun replacingJsonData() {
        val metadata = Metadata("https://37signals.com")
        val message = Message(
            id = "1",
            component = "page",
            event = "connect",
            metadata = metadata,
            jsonData = """{"title":"Page-title","subtitle":"Page-subtitle"}"""
        )

        val newMessage = message.replacing(
            event = "disconnect",
            jsonData = "{}"
        )

        assertEquals("1", newMessage.id)
        assertEquals("page", newMessage.component)
        assertEquals("disconnect", newMessage.event)
        assertEquals(metadata, newMessage.metadata)
        assertEquals("{}", newMessage.jsonData)
    }

    @Test
    fun replacingData() {
        val metadata = Metadata("https://37signals.com")
        val message = Message(
            id = "1",
            component = "page",
            event = "connect",
            metadata = metadata,
            jsonData = "{}"
        )

        val data = MessageData(title = "New-title", subtitle = "New-subtitle")

        val newMessage = message.replacing(
            event = "disconnect",
            data = data
        )

        assertEquals("1", newMessage.id)
        assertEquals("page", newMessage.component)
        assertEquals("disconnect", newMessage.event)
        assertEquals(metadata, newMessage.metadata)
        assertEquals("""{"title":"New-title","subtitle":"New-subtitle"}""", newMessage.jsonData)
    }

    @Test
    fun replacingDataWithNoConverter() {
        Hotwire.config.jsonConverter = null

        val message = Message(
            id = "1",
            component = "page",
            event = "connect",
            metadata = Metadata("https://37signals.com"),
            jsonData = "{}"
        )

        val data = MessageData(title = "New-title", subtitle = "New-subtitle")

        assertThatThrownBy { message.replacing(data = data) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage(BridgeComponentJsonConverter.NO_CONVERTER)
    }

    @Test
    fun replacingDataWithInvalidConverter() {
        Hotwire.config.jsonConverter = InvalidJsonConverter()

        val message = Message(
            id = "1",
            component = "page",
            event = "connect",
            metadata = Metadata("https://37signals.com"),
            jsonData = "{}"
        )

        val data = MessageData(title = "New-title", subtitle = "New-subtitle")

        assertThatThrownBy { message.replacing(data = data) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage(BridgeComponentJsonConverter.INVALID_CONVERTER)
    }

    @Serializable
    private class MessageData(val title: String, val subtitle: String)

    @Serializable
    private class NullableMessageData(val title: String?, val subtitle: String? = null)

    @Serializable
    private class DefaultValuesMessageData(
        val title: String? = "Page-title",
        val subtitle: String? = "Page-subtitle"
    )

    private class InvalidMessageData()

    private class InvalidJsonConverter : BridgeComponentJsonConverter()
}
