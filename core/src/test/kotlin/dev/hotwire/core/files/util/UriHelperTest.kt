package dev.hotwire.core.files.util

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import dev.hotwire.core.turbo.BaseUnitTest
import dev.hotwire.core.turbo.util.dispatcherProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.File

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class UriHelperTest : BaseUnitTest() {

    private val testDispatcher = UnconfinedTestDispatcher(TestCoroutineScheduler())

    private lateinit var context: Context
    private lateinit var uriHelper: UriHelper

    @Before
    override fun setup() {
        super.setup()
        Dispatchers.setMain(testDispatcher)
        dispatcherProvider.io = Dispatchers.Main

        context = ApplicationProvider.getApplicationContext()
        uriHelper = UriHelper(context)
    }

    override fun teardown() {
        super.teardown()
        Dispatchers.resetMain()
    }

    @Test
    fun validUriIsWrittenToFileSuccessfully() = runTest {
        val inputFile = File("/tmp/file.txt")
        val inputFileUri = Uri.fromFile(inputFile)
        Shadows.shadowOf(context.contentResolver).registerInputStream(inputFileUri, "fileContent".byteInputStream())

        val destFile = uriHelper.writeFileTo(inputFileUri, HotwireFileProvider.directory(context))

        assertThat(destFile).isNotNull()
    }

    @Test
    fun pathTraversingUriWithRelativePathFailsToWriteToFile() = runTest {
        val inputFileUri = Uri.parse("../../tmp/file.txt")
        Shadows.shadowOf(context.contentResolver).registerInputStream(inputFileUri, "fileContent".byteInputStream())

        val destFile = uriHelper.writeFileTo(inputFileUri, HotwireFileProvider.directory(context))

        assertThat(destFile).isNull()
    }

    @Test
    fun pathTraversingUriWithNameArgFailsToWriteToFile() = runTest {
        val inputFileUri = Uri.parse("content://malicious.app?path=/data/data/malicious.app/files/file.txt&name=../../file.txt")
        Shadows.shadowOf(context.contentResolver).registerInputStream(inputFileUri, "fileContent".byteInputStream())

        val destFile = uriHelper.writeFileTo(inputFileUri, HotwireFileProvider.directory(context))

        assertThat(destFile).isNull()
    }
}