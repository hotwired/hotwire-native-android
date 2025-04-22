package dev.hotwire.core.turbo.config

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import dev.hotwire.core.turbo.BaseRepositoryTest
import dev.hotwire.core.turbo.config.PathConfiguration.LoaderOptions
import dev.hotwire.core.turbo.config.PathConfiguration.Location
import dev.hotwire.core.turbo.nav.Presentation
import dev.hotwire.core.turbo.nav.PresentationContext
import dev.hotwire.core.turbo.util.toJson
import dev.hotwire.core.turbo.util.toObject
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
    private val options = LoaderOptions(
        httpHeaders = mapOf(
            "Accept" to "application/json",
            "Custom-Header" to "test-value"
        )
    )

    @Before
    override fun setup() {
        super.setup()

        context = ApplicationProvider.getApplicationContext()
        pathConfiguration = PathConfiguration().apply {
            load(
                context = context,
                location = Location(assetFilePath = "json/test-configuration.json"),
                options = options
            )
        }
    }

    @Test
    fun assetConfigurationIsLoaded() {
        assertThat(pathConfiguration.rules.size).isGreaterThan(0)
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
            val options = LoaderOptions()

            pathConfiguration.load(context, location, options)
            verify(mockRepository).getCachedConfigurationForUrl(context, remoteUrl)
            verify(mockRepository).getRemoteConfiguration(remoteUrl, options)
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
            val options = LoaderOptions()
            val json = """{ "settings": {}, "rules": [] }"""

            whenever(mockRepository.getRemoteConfiguration(remoteUrl, options))
                .thenReturn(json)

            pathConfiguration.load(context, location, options)
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
            val options = LoaderOptions()
            val json = "malformed-json"

            whenever(mockRepository.getRemoteConfiguration(remoteUrl, options))
                .thenReturn(json)

            pathConfiguration.load(context, location, options)
            verify(mockRepository, never()).cacheConfigurationForUrl(any(), any(), any())
        }
    }

    @Test
    fun globalSetting() {
        assertThat(pathConfiguration.settings.size).isEqualTo(3)
        assertThat(pathConfiguration.settings["no_such_key"]).isNull()
        assertThat(pathConfiguration.settings["custom_app_feature_enabled"]).isEqualTo(true)
        assertThat(pathConfiguration.settings["custom_app_number"] as Long).isEqualTo(1)
        assertThat(pathConfiguration.settings.getCustomAppData()).isEqualTo(
            CustomAppData(
                marketingSite = "https://native.hotwired.dev",
                demoSite = "https://hotwire-native-demo.dev"
            )
        )
    }

    @Test
    fun recedeHistoricalLocation() {
        val properties = pathConfiguration.properties("$url/recede_historical_location")

        assertThat(properties.presentation).isEqualTo(Presentation.POP)
        assertThat(properties.context).isEqualTo(PresentationContext.DEFAULT)
        assertThat(properties.isHistoricalLocation).isTrue()
    }

    @Test
    fun resumeHistoricalLocation() {
        val properties = pathConfiguration.properties("$url/resume_historical_location")

        assertThat(properties.presentation).isEqualTo(Presentation.NONE)
        assertThat(properties.context).isEqualTo(PresentationContext.DEFAULT)
        assertThat(properties.isHistoricalLocation).isTrue()
    }

    @Test
    fun refreshHistoricalLocation() {
        val properties = pathConfiguration.properties("$url/refresh_historical_location")

        assertThat(properties.presentation).isEqualTo(Presentation.REFRESH)
        assertThat(properties.context).isEqualTo(PresentationContext.DEFAULT)
        assertThat(properties.isHistoricalLocation).isTrue()
    }

    @Test
    fun title() {
        assertThat(pathConfiguration.properties("$url/image.jpg").title).isEqualTo("Image Viewer")
    }

    @Test
    fun animated() {
        assertThat(pathConfiguration.properties("$url/home").animated).isTrue()
        assertThat(pathConfiguration.properties("$url/new").animated).isTrue()
        assertThat(pathConfiguration.properties("$url/not-animated").animated).isFalse()
    }

    @Test
    fun pullToRefresh() {
        assertThat(pathConfiguration.properties("$url/home").pullToRefreshEnabled).isTrue
        assertThat(pathConfiguration.properties("$url/new").pullToRefreshEnabled).isFalse
    }

    @Test
    fun customProperties() {
        assertThat((pathConfiguration.properties("$url/custom/tabs").getTabs()?.size)).isEqualTo(1)
        assertThat((pathConfiguration.properties("$url/custom/tabs").getTabs()?.first()?.label)).isEqualTo("Tab 1")
    }

    // Extension functions to show support for deserializing custom properties/settings

    private fun PathConfigurationProperties.getTabs(): List<Tab>? {
        return get("tabs")?.toJson()?.toObject(object : TypeToken<List<Tab>>() {})
    }

    private fun PathConfigurationSettings.getCustomAppData(): CustomAppData? {
        return get("custom_app_data")?.toJson()?.toObject(object : TypeToken<CustomAppData>() {})
    }

    private data class Tab(
        @SerializedName("label") val label: String,
        @SerializedName("path") val path: String
    )

    private data class CustomAppData(
        @SerializedName("marketing_site") val marketingSite: String,
        @SerializedName("demo_site") val demoSite: String
    )
}
