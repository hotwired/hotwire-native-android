package dev.hotwire.core.turbo.webview

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.text.Html
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.hotwire.core.R
import dev.hotwire.core.config.Hotwire
import dev.hotwire.core.turbo.webview.WebViewInfo.WebViewType

class WebViewVersionCompatibility {
    companion object {
        /**
         * Display an alert dialog if the WebView component installed on the device is
         * less than the `requiredVersion`. The user can tap "Update" to update the
         * corresponding "Google Chrome" or "Android System WebView" app in the Play Store.
         */
        fun displayUpdateDialogIfOutdated(activity: Activity, requiredVersion: Int) {
            val versionInfo = Hotwire.webViewInfo(activity)
            val majorVersion = versionInfo.majorVersion
            val type = versionInfo.webViewType

            if (type == WebViewType.UNKNOWN || majorVersion == null) {
                return
            }

            if (majorVersion < requiredVersion) {
                val descriptionResId = when (versionInfo.webViewType) {
                    WebViewType.CHROME -> R.string.webview_error_chrome_description
                    else -> R.string.webview_error_system_description
                }

                val formattedDescription = activity.getString(descriptionResId)
                    .format(versionInfo.majorVersion, requiredVersion)

                MaterialAlertDialogBuilder(activity)
                    .setTitle(R.string.webview_error_title)
                    .setMessage(Html.fromHtml(formattedDescription, 0))
                    .setNegativeButton(R.string.hotwire_dialog_cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setPositiveButton(R.string.webview_error_update) { dialog, _ ->
                        try {
                            activity.startActivity(Intent(Intent.ACTION_VIEW, versionInfo.playStoreWebViewAppUri))
                        } catch (_: ActivityNotFoundException) {
                            Toast.makeText(activity, R.string.webview_error_store_unavailable, Toast.LENGTH_LONG).show()
                        }
                        dialog.dismiss()
                    }
                    .create()
                    .show()
            }
        }
    }
}
