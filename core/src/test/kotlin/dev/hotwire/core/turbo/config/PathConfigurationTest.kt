package dev.hotwire.core.turbo.config

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockito_kotlin.*
import dev.hotwire.core.turbo.BaseRepositoryTest
import dev.hotwire.core.turbo.config.PathConfiguration.LoaderOptions
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
        assertThat(pathConfiguration.rules.size).isEqualTo(11)
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
    fun callbackIsInvokedWhenConfigurationIsLoaded() {
        val localPath = "/not-animated"
        val cachedPath = "/from-cache"
        val remotePath = "/from-remote"

        var onCompleteInvocations = 0
        val onCompletionCallback: (PathConfiguration) -> Unit = { pathConfiguration ->
            onCompleteInvocations++

            when (onCompleteInvocations) {
                // See PathConfiguration#load for implementation order of calls, this verifies the correct path is loaded in the correct order

                // First call = loading from asset path
                1 -> assertThat(pathConfiguration.rules.filter { it.matches(localPath)}.flatMap { it.patterns }.contains(localPath)).isTrue()
                // Second call = from cache
                2 -> assertThat(pathConfiguration.rules.filter { it.matches(cachedPath)}.flatMap { it.patterns }.contains(cachedPath)).isTrue()
                // Third call = from remote
                3 -> assertThat(pathConfiguration.rules.filter { it.matches(remotePath)}.flatMap { it.patterns }.contains(remotePath)).isTrue()
            }

        }

        val assetFilePath = "json/test-configuration.json"
        val remoteUrl = "$url/demo/configurations/android-v1.json"
        val location = Location(
            assetFilePath = assetFilePath,
            remoteFileUrl = remoteUrl
        )
        val options = LoaderOptions()

        val assetJson = context.assets.open(assetFilePath).use { String(it.readBytes()) }
        // Update a value in the JSON to simulate a different configuration
        val cachedFakeJson = assetJson.replace(localPath, cachedPath)
        val remoteFakeJson = assetJson.replace(localPath, remotePath)

        // Tell the mock repository to return values for the asset, cached, and remote configurations, changing a field in each to allow for assertion
        whenever(mockRepository.getBundledConfiguration(context, assetFilePath)).thenReturn(assetJson)
        whenever(mockRepository.getCachedConfigurationForUrl(context, remoteUrl)).thenReturn(cachedFakeJson)
        whenever(runBlocking { mockRepository.getRemoteConfiguration(remoteUrl, options)}).thenReturn(remoteFakeJson)

        pathConfiguration = PathConfiguration()
        pathConfiguration.loader = PathConfigurationLoader(context).apply {
            repository = mockRepository
        }

        runBlocking {
            pathConfiguration.load(context, location, options, onCompletionCallback)
            verify(mockRepository).getBundledConfiguration(context, assetFilePath)
            verify(mockRepository).getCachedConfigurationForUrl(context, remoteUrl)
            verify(mockRepository).getRemoteConfiguration(remoteUrl, options)

            // The onComplete callback should be invoked 3 times.
            // 1 - asset path
            // 2 - from cache
            // 3 - from remote
            assertThat(onCompleteInvocations).isEqualTo(3)
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
        assertThat(pathConfiguration.settings.size).isEqualTo(1)
        assertThat(pathConfiguration.settings["custom_app_feature_enabled"]).isEqualTo("true")
        assertThat(pathConfiguration.settings["no_such_key"]).isNull()
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
}
