package dev.hotwire.core.files.delegates

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.content.pm.PackageManager
import android.webkit.GeolocationPermissions
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import dev.hotwire.core.files.util.HOTWIRE_REQUEST_CODE_GEOLOCATION_PERMISSION
import dev.hotwire.core.logging.logError
import dev.hotwire.core.turbo.session.Session

class GeolocationPermissionDelegate(private val session: Session) {
    private val context: Context = session.context
    private val permissionToRequest = locationPermission()

    private var requestOrigin: String? = null
    private var requestCallback: GeolocationPermissions.Callback? = null

    fun onRequestPermission(
        origin: String?,
        callback: GeolocationPermissions.Callback?
    ) {
        requestOrigin = origin
        requestCallback = callback

        if (requestOrigin == null || requestCallback == null || permissionToRequest == null) {
            permissionDenied()
        } else if (hasLocationPermission(context)) {
            permissionGranted()
        } else {
            startPermissionRequest()
        }
    }

    fun onActivityResult(isGranted: Boolean) {
        if (isGranted) {
            permissionGranted()
        } else {
            permissionDenied()
        }
    }

    private fun startPermissionRequest() {
        val destination = session.currentVisit?.callback?.visitDestination() ?: return
        val resultLauncher = destination.activityPermissionResultLauncher(
            HOTWIRE_REQUEST_CODE_GEOLOCATION_PERMISSION
        )

        try {
            resultLauncher?.launch(permissionToRequest)
        } catch (e: Exception) {
            logError("startGeolocationPermissionError", e)
            permissionDenied()
        }
    }

    private fun hasLocationPermission(context: Context): Boolean {
        return permissionToRequest?.let {
            ContextCompat.checkSelfPermission(context, it) == PermissionChecker.PERMISSION_GRANTED
        } == true
    }

    private fun permissionGranted() {
        requestCallback?.invoke(requestOrigin, true, true)
        requestOrigin = null
        requestCallback = null
    }

    private fun permissionDenied() {
        requestCallback?.invoke(requestOrigin, false, false)
        requestOrigin = null
        requestCallback = null
    }

    private fun locationPermission(): String? {
        // Only request "fine" location if provided in the app manifest, since
        // the WebView requires this permission for location access. Granting
        // "coarse" location does not work. See: https://issues.chromium.org/issues/40205003
        return manifestPermissions().firstOrNull { it == ACCESS_FINE_LOCATION }
    }

    private fun manifestPermissions(): Array<String> {
        val context = session.context
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )

        return packageInfo.requestedPermissions
    }
}