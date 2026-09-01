package dev.hotwire.core.files.delegates

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.os.Build
import android.webkit.GeolocationPermissions
import androidx.appcompat.app.AppCompatActivity
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import dev.hotwire.core.config.Hotwire
import dev.hotwire.core.turbo.BaseRepositoryTest
import dev.hotwire.core.turbo.session.Session
import dev.hotwire.core.turbo.webview.HotwireWebView
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric.buildActivity
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
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

        // The delegate picks its runtime permission from the manifest at
        // construction, so declare it before the session builds the delegate.
        declareInManifest(ACCESS_FINE_LOCATION)
        session = Session("test", activity, webView)

        Hotwire.config.clearTrustedLocations()
        Hotwire.config.registerTrustedLocation("https://37signals.com")
    }

    @After
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

    @Test
    fun `grants a pending trusted request when the permission dialog resolves`() {
        val callback = mock<GeolocationPermissions.Callback>()
        val origin = "https://37signals.com"

        session.geolocationPermissionDelegate.onRequestPermission(origin, callback)
        session.geolocationPermissionDelegate.onActivityResult(isGranted = true)

        verify(callback).invoke(origin, true, true)
    }

    @Test
    fun `re-verifies the origin when the permission dialog resolves`() {
        val callback = mock<GeolocationPermissions.Callback>()
        val origin = "https://37signals.com"

        session.geolocationPermissionDelegate.onRequestPermission(origin, callback)
        Hotwire.config.clearTrustedLocations()
        session.geolocationPermissionDelegate.onActivityResult(isGranted = true)

        verify(callback).invoke(origin, false, false)
    }

    @Test
    fun `a second request answers the first before replacing it`() {
        val first = mock<GeolocationPermissions.Callback>()
        val second = mock<GeolocationPermissions.Callback>()
        val origin = "https://37signals.com"

        session.geolocationPermissionDelegate.onRequestPermission(origin, first)
        session.geolocationPermissionDelegate.onRequestPermission(origin, second)

        verify(first).invoke(origin, false, false)
    }

    @Test
    fun `a hidden prompt drops the held request`() {
        val callback = mock<GeolocationPermissions.Callback>()
        val origin = "https://37signals.com"

        session.geolocationPermissionDelegate.onRequestPermission(origin, callback)
        session.geolocationPermissionDelegate.onHidePrompt()
        session.geolocationPermissionDelegate.onActivityResult(isGranted = true)

        verify(callback, never()).invoke(any(), any(), any())
    }

    private fun declareInManifest(vararg permissions: String) {
        val context: Context = ApplicationProvider.getApplicationContext()
        val packageInfo = shadowOf(context.packageManager)
            .getInternalMutablePackageInfo(context.packageName)
        val existing = packageInfo.requestedPermissions ?: emptyArray()
        packageInfo.requestedPermissions = (existing + permissions).distinct().toTypedArray()
    }
}
