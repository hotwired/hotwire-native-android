package dev.hotwire.core.files.delegates

import android.Manifest.permission.CAMERA
import android.Manifest.permission.MODIFY_AUDIO_SETTINGS
import android.Manifest.permission.RECORD_AUDIO
import android.app.Application
import android.content.Context
import android.os.Build
import android.webkit.PermissionRequest
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockito_kotlin.whenever
import dev.hotwire.core.turbo.BaseRepositoryTest
import dev.hotwire.core.turbo.session.Session
import dev.hotwire.core.turbo.session.SessionCallback
import dev.hotwire.core.turbo.visit.Visit
import dev.hotwire.core.turbo.visit.VisitDestination
import dev.hotwire.core.turbo.visit.VisitOptions
import dev.hotwire.core.turbo.webview.HotwireWebView
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric.buildActivity
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class WebViewPermissionDelegateTest : BaseRepositoryTest() {
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
    }

    @Test
    fun `denies request that asks for an unsupported resource`() {
        declareInManifest(RECORD_AUDIO)
        val request = mockRequest("android.webkit.resource.MIDI_SYSEX")

        session.webViewPermissionDelegate.onRequest(request)

        verify(request).deny()
    }

    @Test
    fun `denies audio request when RECORD_AUDIO is not declared in manifest`() {
        // Manifest declares neither audio nor video.
        val request = mockRequest(PermissionRequest.RESOURCE_AUDIO_CAPTURE)

        session.webViewPermissionDelegate.onRequest(request)

        verify(request).deny()
    }

    @Test
    fun `denies audio request when MODIFY_AUDIO_SETTINGS is not declared in manifest`() {
        // RECORD_AUDIO alone isn't enough — Chromium WebView needs
        // MODIFY_AUDIO_SETTINGS to select the audio device.
        declareInManifest(RECORD_AUDIO)
        val request = mockRequest(PermissionRequest.RESOURCE_AUDIO_CAPTURE)

        session.webViewPermissionDelegate.onRequest(request)

        verify(request).deny()
    }

    @Test
    fun `denies video request when CAMERA is not declared in manifest`() {
        val request = mockRequest(PermissionRequest.RESOURCE_VIDEO_CAPTURE)

        session.webViewPermissionDelegate.onRequest(request)

        verify(request).deny()
    }

    @Test
    fun `denies audio + video request when only audio permissions are declared`() {
        declareInManifest(RECORD_AUDIO, MODIFY_AUDIO_SETTINGS)
        // CAMERA is missing — the whole request is denied so the page sees a
        // NotAllowedError and can fall back to audio-only.
        val request = mockRequest(
            PermissionRequest.RESOURCE_AUDIO_CAPTURE,
            PermissionRequest.RESOURCE_VIDEO_CAPTURE,
        )

        session.webViewPermissionDelegate.onRequest(request)

        verify(request).deny()
    }

    @Test
    fun `grants audio when permission is already granted`() {
        declareInManifest(RECORD_AUDIO, MODIFY_AUDIO_SETTINGS)
        grantRuntimePermissions(RECORD_AUDIO, MODIFY_AUDIO_SETTINGS)
        val request = mockRequest(PermissionRequest.RESOURCE_AUDIO_CAPTURE)

        session.webViewPermissionDelegate.onRequest(request)

        verify(request).grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
    }

    @Test
    fun `grants video when permission is already granted`() {
        declareInManifest(CAMERA)
        grantRuntimePermissions(CAMERA)
        val request = mockRequest(PermissionRequest.RESOURCE_VIDEO_CAPTURE)

        session.webViewPermissionDelegate.onRequest(request)

        verify(request).grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
    }

    @Test
    fun `grants audio + video when all permissions are already granted`() {
        declareInManifest(RECORD_AUDIO, MODIFY_AUDIO_SETTINGS, CAMERA)
        grantRuntimePermissions(RECORD_AUDIO, MODIFY_AUDIO_SETTINGS, CAMERA)
        val resources = arrayOf(
            PermissionRequest.RESOURCE_AUDIO_CAPTURE,
            PermissionRequest.RESOURCE_VIDEO_CAPTURE,
        )
        val request = mockRequest(*resources)

        session.webViewPermissionDelegate.onRequest(request)

        verify(request).grant(resources)
    }

    @Test
    fun `onActivityResult is a no-op when no request is held`() {
        session.webViewPermissionDelegate.onActivityResult(emptyMap())
        session.webViewPermissionDelegate.onActivityResult(mapOf(RECORD_AUDIO to true))
    }

    @Test
    fun `a second pending request denies the first to avoid orphaning it`() {
        declareInManifest(RECORD_AUDIO, MODIFY_AUDIO_SETTINGS)
        wireDestinationWithLauncher()
        // Permission not yet granted at runtime, so each request is held as
        // pending after launching the system prompt. The second request should
        // explicitly deny the first so the WebView always gets a verdict.
        val first = mockRequest(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
        val second = mockRequest(PermissionRequest.RESOURCE_AUDIO_CAPTURE)

        session.webViewPermissionDelegate.onRequest(first)
        session.webViewPermissionDelegate.onRequest(second)

        verify(first).deny()
    }

    @Test
    fun `onCancel clears matching pending request so onActivityResult is a no-op`() {
        declareInManifest(RECORD_AUDIO, MODIFY_AUDIO_SETTINGS)
        wireDestinationWithLauncher()
        val request = mockRequest(PermissionRequest.RESOURCE_AUDIO_CAPTURE)

        session.webViewPermissionDelegate.onRequest(request)
        session.webViewPermissionDelegate.onCancel(request)
        // After the cancel, the system permission dialog might still resolve;
        // the result must be ignored rather than applied to the canceled
        // request.
        session.webViewPermissionDelegate.onActivityResult(mapOf(RECORD_AUDIO to true))

        verify(request, org.mockito.Mockito.never()).grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
        verify(request, org.mockito.Mockito.never()).deny()
    }

    @Test
    fun `onCancel ignores a request that is not currently pending`() {
        declareInManifest(RECORD_AUDIO, MODIFY_AUDIO_SETTINGS)
        // In production MODIFY_AUDIO_SETTINGS is auto-granted at install since
        // it is a normal-level permission; Robolectric requires the explicit
        // grant for the post-result isGranted() check to return true.
        grantRuntimePermissions(MODIFY_AUDIO_SETTINGS)
        wireDestinationWithLauncher()
        val pending = mockRequest(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
        val unrelated = mockRequest(PermissionRequest.RESOURCE_AUDIO_CAPTURE)

        session.webViewPermissionDelegate.onRequest(pending)
        session.webViewPermissionDelegate.onCancel(unrelated)
        // The pending request is still tracked; resolving via onActivityResult
        // with a granted permission should grant the original request.
        session.webViewPermissionDelegate.onActivityResult(mapOf(RECORD_AUDIO to true))

        verify(pending).grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
    }

    private fun declareInManifest(vararg permissions: String) {
        val packageInfo = shadowOf(context.packageManager)
            .getInternalMutablePackageInfo(context.packageName)
        val existing = packageInfo.requestedPermissions ?: emptyArray()
        packageInfo.requestedPermissions = (existing + permissions).distinct().toTypedArray()
    }

    private fun grantRuntimePermissions(vararg permissions: String) {
        shadowOf(context.applicationContext as Application).grantPermissions(*permissions)
    }

    @Suppress("UNCHECKED_CAST")
    private fun wireDestinationWithLauncher() {
        // Provide a no-op launcher so onRequest holds the request as pending
        // instead of denying it via the launcher-not-available branch.
        val launcher = mock(ActivityResultLauncher::class.java) as ActivityResultLauncher<Array<String>>
        val visitDestination = object : VisitDestination {
            override fun isActive() = true
            override fun activityResultLauncher(requestCode: Int) = null
            override fun activityPermissionResultLauncher(requestCode: Int) = null
            override fun activityMultiplePermissionsResultLauncher(
                requestCode: Int
            ): ActivityResultLauncher<Array<String>>? = launcher
        }
        val callback = mock(SessionCallback::class.java)
        whenever(callback.visitDestination()).thenReturn(visitDestination)
        session.currentVisit = Visit(
            location = baseUrl(),
            destinationIdentifier = 1,
            restoreWithCachedSnapshot = false,
            reload = false,
            callback = callback,
            identifier = "",
            options = VisitOptions(),
        )
    }

    private fun mockRequest(vararg resources: String): PermissionRequest {
        val request = mock(PermissionRequest::class.java)
        whenever(request.resources).thenReturn(resources)
        return request
    }
}
