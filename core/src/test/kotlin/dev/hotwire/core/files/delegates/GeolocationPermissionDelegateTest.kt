package dev.hotwire.core.files.delegates

import android.os.Build
import android.webkit.GeolocationPermissions
import androidx.appcompat.app.AppCompatActivity
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import dev.hotwire.core.config.Hotwire
import dev.hotwire.core.turbo.BaseRepositoryTest
import dev.hotwire.core.turbo.session.Session
import dev.hotwire.core.turbo.webview.HotwireWebView
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric.buildActivity
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class GeolocationPermissionDelegateTest : BaseRepositoryTest() {
    @Mock
    private lateinit var webView: HotwireWebView
    private lateinit var activity: AppCompatActivity
    private lateinit var session: Session

    @Before
    override fun setup() {
        super.setup()
        MockitoAnnotations.openMocks(this)

        activity = buildActivity(TurboTestActivity::class.java).get()
        session = Session("test", activity, webView)

        Hotwire.config.clearTrustedLocations()
        Hotwire.config.registerTrustedLocation("https://37signals.com")
    }

    @org.junit.After
    fun teardownTrustedLocations() {
        Hotwire.config.clearTrustedLocations()
    }

    @Test
    fun `denies a request from an untrusted origin`() {
        val callback = mock<GeolocationPermissions.Callback>()
        val origin = "https://evil.attacker.com"

        session.geolocationPermissionDelegate.onRequestPermission(origin, callback)

        verify(callback).invoke(origin, false, false)
    }

    @Test
    fun `denies a request with no origin`() {
        val callback = mock<GeolocationPermissions.Callback>()

        session.geolocationPermissionDelegate.onRequestPermission(null, callback)

        verify(callback).invoke(null, false, false)
    }
}
