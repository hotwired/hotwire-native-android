package dev.hotwire.core.files.util

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import dev.hotwire.core.turbo.BaseUnitTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class HotwireFileProviderTest : BaseUnitTest() {
    private lateinit var context: Context

    @Before
    override fun setup() {
        super.setup()
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun authority() {
        val authority = HotwireFileProvider.authority(context)
        assertThat(authority).isEqualTo("dev.hotwire.core.test.hotwire.fileprovider")
    }

    @Test
    fun directory() {
        val directory = HotwireFileProvider.directory(context)
        assertThat(directory.path).endsWith("dev.hotwire.core.test-dataDir/files/shared")
    }
}
