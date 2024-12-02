package dev.hotwire.core.turbo.visit

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher

interface VisitDestination {
    fun isActive(): Boolean
    fun activityResultLauncher(requestCode: Int): ActivityResultLauncher<Intent>?
    fun activityPermissionResultLauncher(requestCode: Int): ActivityResultLauncher<String>?
}
