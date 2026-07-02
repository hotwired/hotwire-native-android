package dev.hotwire.core.turbo.visit

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher

interface VisitDestination {
    fun isActive(): Boolean
    fun activityResultLauncher(requestCode: Int): ActivityResultLauncher<Intent>?
    fun activityPermissionResultLauncher(requestCode: Int): ActivityResultLauncher<String>?

    /**
     * Returns a launcher capable of requesting multiple runtime permissions
     * at once. Used by [dev.hotwire.core.files.delegates.WebViewPermissionDelegate]
     * for media-capture requests that may include both audio and video. Default
     * implementation returns `null`; concrete destinations should override.
     */
    fun activityMultiplePermissionsResultLauncher(
        requestCode: Int
    ): ActivityResultLauncher<Array<String>>? = null
}
