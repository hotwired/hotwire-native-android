package dev.hotwire.core.security

import dev.hotwire.core.config.Hotwire
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class JavascriptChannelTest {
    private val received = mutableListOf<JavascriptMessage>()
    private val channel = JavascriptChannel("TestChannel") { received.add(it) }

    @Before
    fun setup() {
        Hotwire.config.startLocationRegistry.clear()
        Hotwire.config.registerStartLocation("https://my.app.com/start")
    }

    @After
    fun teardown() {
        Hotwire.config.startLocationRegistry.clear()
    }

    @Test
    fun `a message from a trusted main frame is decoded and delivered`() {
        channel.receive("""{"name":"ping","args":["a",1,true]}""", "https://my.app.com", isMainFrame = true)

        assertEquals(listOf("ping"), received.map { it.name })
        assertEquals("a", received.single().args.stringAt(0))
        assertEquals(1, received.single().args.intAt(1))
        assertEquals(true, received.single().args.booleanAt(2))
    }

    @Test
    fun `messages from untrusted, opaque, or sub-frame sources are not delivered`() {
        channel.receive("""{"name":"ping"}""", "https://evil.attacker.com", isMainFrame = true)
        channel.receive("""{"name":"ping"}""", "null", isMainFrame = true)
        channel.receive("""{"name":"ping"}""", "https://my.app.com", isMainFrame = false)

        assertEquals(emptyList<JavascriptMessage>(), received)
    }

    @Test
    fun `malformed messages are not delivered`() {
        channel.receive("not json", "https://my.app.com", isMainFrame = true)
        channel.receive("""{"args":[]}""", "https://my.app.com", isMainFrame = true)

        assertEquals(emptyList<JavascriptMessage>(), received)
    }

    @Test
    fun `a handler failure does not escape the channel`() {
        val failing = JavascriptChannel("TestChannel") { it.args.stringAt(0) }

        failing.receive("""{"name":"ping","args":[]}""", "https://my.app.com", isMainFrame = true)
    }
}
