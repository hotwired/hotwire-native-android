package dev.hotwire.core.turbo.config

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockito_kotlin.*
import dev.hotwire.core.turbo.BaseRepositoryTest
import dev.hotwire.core.turbo.config.PathConfiguration.Location
import dev.hotwire.core.turbo.nav.PresentationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class PathConfigurationTest : BaseRepositoryTest() {
    private lateinit var context: Context
    private lateinit var pathConfiguration: PathConfiguration
    private val mockRepository = mock<PathConfigurationRepository>()
    private val url = "https://turbo.hotwired.dev"

    @Before
    override fun setup() {
        super.setup()

        context = ApplicationProvider.getApplicationContext()
        pathConfiguration = PathConfiguration().apply {
            load(context, Location(assetFilePath = "json/test-configuration.json"))
        }
    }

    @Test
    fun assetConfigurationIsLoaded() {
        assertThat(pathConfiguration.rules.size).isEqualTo(10)
    }

    @Test
    fun presentationContext() {
        assertThat(pathConfiguration.properties("$url/home").context).isEqualTo(
            PresentationContext.DEFAULT)
        assertThat(pathConfiguration.properties("$url/new").context).isEqualTo(
            PresentationContext.MODAL)
        assertThat(pathConfiguration.properties("$url/edit").context).isEqualTo(
            PresentationContext.MODAL)
    }

    @Test
    fun remoteConfigurationIsFetched() {
        pathConfiguration.loader = PathConfigurationLoader(context).apply {
            repository = mockRepository
        }

        runBlocking {
            val remoteUrl = "$url/demo/configurations/android-v1.json"
            val location = Location(remoteFileUrl = remoteUrl)

            pathConfiguration.load(context, location)
            verify(mockRepository).getCachedConfigurationForUrl(context, remoteUrl)
            verify(mockRepository).getRemoteConfiguration(remoteUrl)
        }
    }

    @Test
    fun validConfigurationIsCached() {
        pathConfiguration.loader = PathConfigurationLoader(context).apply {
            repository = mockRepository
        }

        runBlocking {
            val remoteUrl = "$url/demo/configurations/android-v1.json"
            val location = Location(remoteFileUrl = remoteUrl)
            val json = """{ "settings": {}, "rules": [] }"""

            whenever(mockRepository.getRemoteConfiguration(remoteUrl)).thenReturn(json)

            pathConfiguration.load(context, location)
            verify(mockRepository).cacheConfigurationForUrl(eq(context), eq(remoteUrl), any())
        }
    }

    @Test
    fun malformedConfigurationIsNotCached() {
        pathConfiguration.loader = PathConfigurationLoader(context).apply {
            repository = mockRepository
        }

        runBlocking {
            val remoteUrl = "$url/demo/configurations/android-v1.json"
            val location = Location(remoteFileUrl = remoteUrl)
            val json = "malformed-json"

            whenever(mockRepository.getRemoteConfiguration(remoteUrl)).thenReturn(json)

            pathConfiguration.load(context, location)
            verify(mockRepository, never()).cacheConfigurationForUrl(any(), any(), any())
        }
    }

    @Test
    fun globalSetting() {
        assertThat(pathConfiguration.settings.size).isEqualTo(1)
        assertThat(pathConfiguration.settings["custom_app_feature_enabled"]).isEqualTo("true")
        assertThat(pathConfiguration.settings["no_such_key"]).isNull()
    }

    @Test
    fun title() {
        assertThat(pathConfiguration.properties("$url/image.jpg").title).isEqualTo("Image Viewer")
    }

    @Test
    fun pullToRefresh() {
        assertThat(pathConfiguration.properties("$url/home").pullToRefreshEnabled).isTrue
        assertThat(pathConfiguration.properties("$url/new").pullToRefreshEnabled).isFalse
    }
}
