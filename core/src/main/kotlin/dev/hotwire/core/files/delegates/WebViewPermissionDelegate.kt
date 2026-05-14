package dev.hotwire.core.files.delegates

import android.Manifest.permission.CAMERA
import android.Manifest.permission.MODIFY_AUDIO_SETTINGS
import android.Manifest.permission.RECORD_AUDIO
import android.content.Context
import android.content.pm.PackageManager
import android.webkit.PermissionRequest
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import dev.hotwire.core.files.util.HOTWIRE_REQUEST_CODE_WEBVIEW_PERMISSION
import dev.hotwire.core.logging.logError
import dev.hotwire.core.logging.logWarning
import dev.hotwire.core.turbo.session.Session

/**
 * Handles WebView-issued [PermissionRequest]s for media-capture resources
 * (`RESOURCE_AUDIO_CAPTURE` and `RESOURCE_VIDEO_CAPTURE`). Requests for any
 * other resource are denied by default.
 *
 * Manifest requirements for the host app:
 *
 * - `RESOURCE_AUDIO_CAPTURE` requires both `android.permission.RECORD_AUDIO`
 *   and `android.permission.MODIFY_AUDIO_SETTINGS`. Only `RECORD_AUDIO` is
 *   runtime-requested; `MODIFY_AUDIO_SETTINGS` is a normal (install-time)
 *   permission, but the Chromium WebView's audio pipeline requires it to
 *   select an audio device — without it, `getUserMedia({ audio: true })`
 *   fails with `Unable to select communication device!` even after
 *   `RECORD_AUDIO` is granted.
 * - `RESOURCE_VIDEO_CAPTURE` requires `android.permission.CAMERA`.
 *
 * If a requested resource's manifest permissions are not all declared, the
 * entire request is denied so the page sees a `NotAllowedError` and can react
 * appropriately (and a warning is logged to surface the missing declaration).
 */
class WebViewPermissionDelegate(private val session: Session) {
    private val context: Context = session.context

    private var pendingRequest: PermissionRequest? = null

    fun onRequest(request: PermissionRequest) {
        val requestedResources = request.resources?.toList().orEmpty()
        val supportedResources = requestedResources.filter { it in SUPPORTED_RESOURCES }

        if (supportedResources.isEmpty() || supportedResources.size != requestedResources.size) {
            // Either no recognized resource was requested or the request mixes recognized and unrecognized resources.
            request.deny()
            return
        }

        val manifestPermissions = supportedResources.flatMap { it.requiredManifestPermissions() }.distinct()
        val undeclared = manifestPermissions.filterNot { isDeclaredInManifest(it) }
        if (undeclared.isNotEmpty()) {
            logWarning(
                "webViewPermissionNotDeclared",
                "Permission(s) ${undeclared.joinToString()} are not declared in the host " +
                        "app's AndroidManifest.xml. Add them via <uses-permission> to enable " +
                        "the corresponding WebView media-capture resource(s)."
            )
            request.deny()
            return
        }

        // Only dangerous-level permissions need a runtime grant. Normal-level
        // permissions like MODIFY_AUDIO_SETTINGS are granted automatically at
        // install time once declared in the manifest.
        val runtimeNeeded = manifestPermissions.filter { it in RUNTIME_GRANT_PERMISSIONS && !isGranted(it) }
        if (runtimeNeeded.isEmpty()) {
            request.grant(supportedResources.toTypedArray())
            return
        }

        // Replace any previously-held request before storing the new one so
        // the WebView always sees a grant or deny — never an orphaned request
        // that's silently dropped because it was overwritten.
        pendingRequest?.deny()
        pendingRequest = request
        startPermissionRequest(runtimeNeeded)
    }

    /**
     * Forwarded from [android.webkit.WebChromeClient.onPermissionRequestCanceled].
     * Clears our pending state if it matches the canceled request so we don't
     * later call grant/deny on a request that the WebView has already given up
     * on (e.g. the user navigated away or the page was reloaded mid-prompt).
     */
    fun onCancel(request: PermissionRequest) {
        if (pendingRequest === request) {
            pendingRequest = null
        }
    }

    fun onActivityResult(grantResults: Map<String, Boolean>) {
        val request = pendingRequest ?: return
        pendingRequest = null

        val resources = request.resources?.toList().orEmpty()
        val manifestPermissions = resources.flatMap { it.requiredManifestPermissions() }.distinct()
        val allGranted = manifestPermissions.all { permission ->
            grantResults[permission] == true || isGranted(permission)
        }

        if (allGranted) {
            request.grant(resources.toTypedArray())
        } else {
            request.deny()
        }
    }

    private fun startPermissionRequest(permissions: List<String>) {
        val destination = session.currentVisit?.callback?.visitDestination()
        val resultLauncher = destination?.activityMultiplePermissionsResultLauncher(
            HOTWIRE_REQUEST_CODE_WEBVIEW_PERMISSION
        )

        if (resultLauncher == null) {
            pendingRequest?.deny()
            pendingRequest = null
            return
        }

        try {
            resultLauncher.launch(permissions.toTypedArray())
        } catch (e: Exception) {
            logError("startWebViewPermissionError", e)
            pendingRequest?.deny()
            pendingRequest = null
        }
    }

    private fun isGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) ==
                PermissionChecker.PERMISSION_GRANTED
    }

    private fun isDeclaredInManifest(permission: String): Boolean {
        return manifestPermissions().contains(permission)
    }

    private fun manifestPermissions(): Array<String> {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_PERMISSIONS
            )
            packageInfo.requestedPermissions ?: emptyArray()
        } catch (e: PackageManager.NameNotFoundException) {
            logError("manifestPermissionsNotAvailable", e)
            emptyArray()
        }
    }

    private fun String.requiredManifestPermissions(): List<String> = when (this) {
        PermissionRequest.RESOURCE_AUDIO_CAPTURE -> listOf(RECORD_AUDIO, MODIFY_AUDIO_SETTINGS)
        PermissionRequest.RESOURCE_VIDEO_CAPTURE -> listOf(CAMERA)
        else -> error("Unsupported WebView resource: $this")
    }

    private companion object {
        private val SUPPORTED_RESOURCES = setOf(
            PermissionRequest.RESOURCE_AUDIO_CAPTURE,
            PermissionRequest.RESOURCE_VIDEO_CAPTURE,
        )
        private val RUNTIME_GRANT_PERMISSIONS = setOf(RECORD_AUDIO, CAMERA)
    }
}
