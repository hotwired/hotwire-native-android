package dev.hotwire.core.files.delegates

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.webkit.ValueCallback
import android.webkit.WebChromeClient.FileChooserParams
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockito_kotlin.anyOrNull
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import dev.hotwire.core.config.Hotwire
import dev.hotwire.core.files.util.HotwireFileProvider
import dev.hotwire.core.turbo.BaseRepositoryTest
import dev.hotwire.core.turbo.session.Session
import dev.hotwire.core.turbo.session.SessionCallback
import dev.hotwire.core.turbo.visit.Visit
import dev.hotwire.core.turbo.visit.VisitDestination
import dev.hotwire.core.turbo.visit.VisitOptions
import dev.hotwire.core.turbo.webview.HotwireWebView
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric.buildActivity
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class FileChooserDelegateTest : BaseRepositoryTest() {
    @Mock
    private lateinit var webView: HotwireWebView
    private lateinit var activity: AppCompatActivity
    private lateinit var context: Context
    private lateinit var session: Session

    @Before
    override fun setup() {
        super.setup()
        MockitoAnnotations.openMocks(this)

        activity = buildActivity(TurboTestActivity::class.java).get()
        context = ApplicationProvider.getApplicationContext()
        session = Session("test", activity, webView)

        Hotwire.config.clearTrustedLocations()
        Hotwire.config.registerTrustedLocation("https://37signals.com")
    }

    @After
    fun teardownTrustedLocations() {
        Hotwire.config.clearTrustedLocations()
    }

    @Test
    fun `file chooser is blocked on an untrusted page`() {
        whenever(webView.url).thenReturn("https://evil.attacker.com/page")
        val callback = mock<ValueCallback<Array<Uri>>>()

        val handled = session.fileChooserDelegate.onShowFileChooser(callback, mock())

        assertThat(handled).isTrue()
        verify(callback).onReceiveValue(null)
    }

    @Test
    fun `file chooser is blocked with no page loaded`() {
        whenever(webView.url).thenReturn(null)
        val callback = mock<ValueCallback<Array<Uri>>>()

        val handled = session.fileChooserDelegate.onShowFileChooser(callback, mock())

        assertThat(handled).isTrue()
        verify(callback).onReceiveValue(null)
    }

    @Test
    fun `file chooser passes the gate on a trusted page`() {
        whenever(webView.url).thenReturn("https://37signals.com/page")
        val callback = mock<ValueCallback<Array<Uri>>>()

        // The gate passes; the chooser then fails to open because no visit
        // destination exists, which reports "not handled".
        val handled = session.fileChooserDelegate.onShowFileChooser(callback, params())

        assertThat(handled).isFalse()
    }

    @Test
    fun `a second request answers the first before replacing it`() {
        whenever(webView.url).thenReturn("https://37signals.com/page")
        wireDestinationWithLauncher()
        val first = mock<ValueCallback<Array<Uri>>>()
        val second = mock<ValueCallback<Array<Uri>>>()

        session.fileChooserDelegate.onShowFileChooser(first, params())
        session.fileChooserDelegate.onShowFileChooser(second, params())

        verify(first).onReceiveValue(null)
        verify(second, never()).onReceiveValue(anyOrNull())
    }

    @Test
    fun `picker results are dropped when the page navigated to an untrusted origin`() {
        whenever(webView.url).thenReturn("https://37signals.com/page")
        wireDestinationWithLauncher()
        val callback = mock<ValueCallback<Array<Uri>>>()
        session.fileChooserDelegate.onShowFileChooser(callback, params())

        whenever(webView.url).thenReturn("https://evil.attacker.com/page")
        session.fileChooserDelegate.sendResult(arrayOf(Uri.parse("content://files/1")))

        verify(callback).onReceiveValue(null)
    }

    @Test
    fun `picker results are delivered when the page stays trusted`() {
        whenever(webView.url).thenReturn("https://37signals.com/page")
        wireDestinationWithLauncher()
        val callback = mock<ValueCallback<Array<Uri>>>()
        session.fileChooserDelegate.onShowFileChooser(callback, params())

        val results = arrayOf(Uri.parse("content://files/1"))
        session.fileChooserDelegate.sendResult(results)

        verify(callback).onReceiveValue(results)
    }

    private fun params(): FileChooserParams = mock {
        whenever(it.acceptTypes).thenReturn(arrayOf("*/*"))
        whenever(it.isCaptureEnabled).thenReturn(false)
    }

    private fun wireDestinationWithLauncher() {
        val launcher = mock<ActivityResultLauncher<Intent>>()
        val visitDestination = object : VisitDestination {
            override fun isActive() = true
            override fun activityResultLauncher(requestCode: Int) = launcher
            override fun activityPermissionResultLauncher(requestCode: Int) = null
        }
        val callback = mock<SessionCallback>()
        whenever(callback.visitDestination()).thenReturn(visitDestination)
        session.currentVisit = Visit(
            location = "https://37signals.com",
            destinationIdentifier = 1,
            restoreWithCachedSnapshot = false,
            reload = false,
            callback = callback,
            identifier = "",
            options = VisitOptions(),
        )
    }

    @Test
    fun fileProviderDirectoryIsCleared() {
        val dir = HotwireFileProvider.directory(context)

        File(dir, "testFile.txt").apply {
            writeText("text")
        }

        assertThat(dir.listFiles()?.size).isEqualTo(1)
        assertThat(dir.listFiles()?.get(0)?.name).isEqualTo("testFile.txt")

        runBlocking {
            session.fileChooserDelegate.deleteCachedFiles()
            assertThat(dir.listFiles()?.size).isEqualTo(0)
        }
    }
}

internal class TurboTestActivity : AppCompatActivity()
