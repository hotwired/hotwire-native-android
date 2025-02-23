package dev.hotwire.core.config

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dev.hotwire.core.turbo.BaseRepositoryTest
import dev.hotwire.core.turbo.config.PathConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
class HotwireTest : BaseRepositoryTest(){
    private lateinit var context: Context

    override fun setup() {
        super.setup()
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `loadPathConfiguration should pass headers through to HTTP request`() {
        enqueueResponse("test-configuration.json")
        val customHeaders = hashMapOf(
            "Custom-Header" to "test-value",
            "Accept" to "application/json"
        )
        val clientConfig = PathConfiguration.ClientConfig(headers = customHeaders)

        Hotwire.loadPathConfiguration(
            context = context,
            location = PathConfiguration.Location(
                remoteFileUrl = baseUrl()
            ),
            clientConfig = clientConfig
        )

        runBlocking {
            launch(Dispatchers.Main) {
                val request = server.takeRequest(5, TimeUnit.SECONDS)

                assertThat(request?.headers?.get("Custom-Header")).isEqualTo("test-value")
                assertThat(request?.headers?.get("Accept")).isEqualTo("application/json")
            }
        }
    }

    @Test
    fun `loadPathConfiguration should work without custom headers`() {
        enqueueResponse("test-configuration.json")

        Hotwire.loadPathConfiguration(
            context = context,
            location = PathConfiguration.Location(
                remoteFileUrl = baseUrl()
            )
        )

        runBlocking {
            launch(Dispatchers.Main) {
                val request = server.takeRequest(5, TimeUnit.SECONDS)

                assertThat(request).isNotNull
                assertThat(request?.headers?.get("Custom-Header")).isNull()
            }
        }
    }
}