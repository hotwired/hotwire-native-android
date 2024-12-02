package dev.hotwire.core.files.delegates

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.webkit.GeolocationPermissions
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import dev.hotwire.core.files.util.HOTWIRE_REQUEST_CODE_GEOLOCATION_PERMISSION
import dev.hotwire.core.logging.logError
import dev.hotwire.core.turbo.session.Session

class GeolocationPermissionDelegate(private val session: Session) {
    private val context: Context = session.context
    private val permissionToRequest = preferredLocationPermission()

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

    private fun preferredLocationPermission(): String? {
        val declaredPermissions = manifestPermissions().filter {
            it == ACCESS_COARSE_LOCATION ||
            it == ACCESS_FINE_LOCATION
        }

        // Prefer fine location if provided in manifest, otherwise coarse location
        return if (declaredPermissions.contains(ACCESS_FINE_LOCATION)) {
            ACCESS_FINE_LOCATION
        } else if (declaredPermissions.contains(ACCESS_COARSE_LOCATION)) {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.S ||
                Build.VERSION.SDK_INT == Build.VERSION_CODES.S_V2) {
                // Android 12 requires the "fine" permission for location
                // access within the WebView. Granting "coarse" location does not
                // work. See: https://issues.chromium.org/issues/40205003
                null
            } else {
                ACCESS_COARSE_LOCATION
            }
        } else {
            null
        }
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