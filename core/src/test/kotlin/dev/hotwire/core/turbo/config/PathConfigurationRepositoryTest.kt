package dev.hotwire.core.turbo.config

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.google.gson.reflect.TypeToken
import dev.hotwire.core.turbo.BaseRepositoryTest
import dev.hotwire.core.turbo.config.PathConfiguration.LoaderOptions
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
                val json = repository.getRemoteConfiguration(baseUrl(), LoaderOptions())
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
        assertThat(config?.rules?.size).isEqualTo(12)
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
    fun `getRemoteConfiguration should include custom headers`() {
        enqueueResponse("test-configuration.json")

        val options = LoaderOptions(
            httpHeaders = mapOf(
                "Accept" to "application/json",
                "Custom-Header" to "test-value"
            )
        )

        runBlocking {
            launch(Dispatchers.Main) {
                repository.getRemoteConfiguration(baseUrl(), options)

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
