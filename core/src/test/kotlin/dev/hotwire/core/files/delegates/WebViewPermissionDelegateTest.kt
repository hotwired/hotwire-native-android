package dev.hotwire.core.files.delegates

import android.Manifest.permission.CAMERA
import android.Manifest.permission.MODIFY_AUDIO_SETTINGS
import android.Manifest.permission.RECORD_AUDIO
import android.app.Application
import android.content.Context
import android.os.Build
import android.webkit.PermissionRequest
import androidx.appcompat.app.AppCompatActivity
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockito_kotlin.whenever
import dev.hotwire.core.turbo.BaseRepositoryTest
import dev.hotwire.core.turbo.session.Session
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

    private fun declareInManifest(vararg permissions: String) {
        val packageInfo = shadowOf(context.packageManager)
            .getInternalMutablePackageInfo(context.packageName)
        val existing = packageInfo.requestedPermissions ?: emptyArray()
        packageInfo.requestedPermissions = (existing + permissions).distinct().toTypedArray()
    }

    private fun grantRuntimePermissions(vararg permissions: String) {
        shadowOf(context.applicationContext as Application).grantPermissions(*permissions)
    }

    private fun mockRequest(vararg resources: String): PermissionRequest {
        val request = mock(PermissionRequest::class.java)
        whenever(request.resources).thenReturn(resources)
        return request
    }
}
