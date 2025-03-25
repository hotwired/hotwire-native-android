package dev.hotwire.core.bridge

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import dev.hotwire.core.config.Hotwire
import dev.hotwire.core.turbo.BaseUnitTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class UserAgentTest : BaseUnitTest() {
    private lateinit var context: Context

    @Before
    override fun setup() {
        super.setup()
        context = ApplicationProvider.getApplicationContext()
        Hotwire.config.applicationUserAgentPrefix = null
    }

    @Test
    fun `user agent with no prefix`() {
        Hotwire.config.registeredBridgeComponentFactories = TestData.componentFactories

        val userAgent = Hotwire.config.userAgent
        val expectedUserAgent =
                "Hotwire Native Android; Turbo Native Android; " +
                "bridge-components: [one two];"

        assertEquals(expectedUserAgent, userAgent)
    }

    @Test
    fun `user agent with prefix`() {
        Hotwire.config.applicationUserAgentPrefix = "My Application Prefix;"
        Hotwire.config.registeredBridgeComponentFactories = TestData.componentFactories

        val userAgent = Hotwire.config.userAgent
        val expectedUserAgent =
                "My Application Prefix; " +
                "Hotwire Native Android; Turbo Native Android; " +
                "bridge-components: [one two];"

        assertEquals(expectedUserAgent, userAgent)
    }

    @Test
    fun `user agent with prefix and webview default`() {
        Hotwire.config.applicationUserAgentPrefix = "My Application Prefix;"
        Hotwire.config.registeredBridgeComponentFactories = TestData.componentFactories

        val userAgent = Hotwire.config.userAgentWithWebViewDefault(context)
        val expectedUserAgent =
            "My Application Prefix; " +
                    "Hotwire Native Android; Turbo Native Android; " +
                    "bridge-components: [one two]; " +
                    TEST_USER_AGENT

        assertEquals(expectedUserAgent, userAgent)
    }

    companion object {
        private const val TEST_USER_AGENT = "user"
    }
}
