package dev.hotwire.core.turbo.config

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.google.gson.reflect.TypeToken
import dev.hotwire.core.turbo.BaseRepositoryTest
import dev.hotwire.core.turbo.util.toObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class PathConfigurationRepositoryTest : BaseRepositoryTest() {
    private lateinit var context: Context
    private val repository = PathConfigurationRepository()

    override fun setup() {
        super.setup()
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun getRemoteConfiguration() {
        enqueueResponse("test-configuration.json")

        runBlocking {
            launch(Dispatchers.Main) {
                val json = repository.getRemoteConfiguration(baseUrl(), PathConfiguration.ClientConfig())
                assertThat(json).isNotNull()

                val config = load(json)
                assertThat(config?.rules?.size).isEqualTo(2)
            }
        }
    }

    @Test
    fun getBundledAssetConfiguration() {
        val json = repository.getBundledConfiguration(context, "json/test-configuration.json")
        assertThat(json).isNotNull()

        val config = load(json)
        assertThat(config?.rules?.size).isEqualTo(10)
    }

    @Test
    fun getCachedConfiguration() {
        val url = "https://turbo.hotwired.dev/demo/configurations/android-v1.json"
        val config = requireNotNull(load(json()))
        repository.cacheConfigurationForUrl(context, url, config)

        val json = repository.getCachedConfigurationForUrl(context, url)
        assertThat(json).isNotNull()

        val cachedConfig = load(json)
        assertThat(cachedConfig?.rules?.size).isEqualTo(1)
    }

    @Test
    fun `getRemoteConfiguration should not include custom headers by default`() {
        enqueueResponse("test-configuration.json")

        runBlocking {
            launch(Dispatchers.Main) {
                repository.getRemoteConfiguration(baseUrl(), PathConfiguration.ClientConfig())

                val request = server.takeRequest()
                print(request.headers)
                assertThat(request.headers["Custom-Header"]).isNull()
            }
        }
    }

    @Test
    fun `getRemoteConfiguration should not include custom headers by default when clientConfig is null`() {
        enqueueResponse("test-configuration.json")

        runBlocking {
            launch(Dispatchers.Main) {
                repository.getRemoteConfiguration(baseUrl(), null)

                val request = server.takeRequest()
                print(request.headers)
                assertThat(request.headers["Custom-Header"]).isNull()
            }
        }
    }

    @Test
    fun `getRemoteConfiguration should include custom headers when set`() {
        enqueueResponse("test-configuration.json")

        val customHeaders = mapOf(
            "Custom-Header" to "test-value",
            "Accept" to "application/json"
        )
        val clientConfiguration = PathConfiguration.ClientConfig(customHeaders)

        runBlocking {
            launch(Dispatchers.Main) {
                repository.getRemoteConfiguration(baseUrl(), clientConfiguration)

                val request = server.takeRequest()
                assertThat(request.headers["Custom-Header"]).isEqualTo("test-value")
                assertThat(request.headers["Accept"]).isEqualTo("application/json")
            }
        }
    }

    private fun load(json: String?): PathConfiguration? {
        return json?.toObject(object : TypeToken<PathConfiguration>() {})
    }

    private fun json(): String {
        return """
        {
          "rules": [
            {"patterns": [".+"], "properties": {"context": "default"} }
          ]
        }
        """.trimIndent()
    }
}
