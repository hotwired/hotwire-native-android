package dev.hotwire.core.turbo.webview

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
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
         * Determines whether the WebView component version installed on the device is less
         * than the `requiredVersion`.
         *
         * @return True if the WebView is outdated, otherwise false.
         */
        fun isOutdated(context: Context, requiredVersion: Int): Boolean {
            val versionInfo = Hotwire.webViewInfo(context)
            val majorVersion = versionInfo.majorVersion
            val type = versionInfo.webViewType

            return type != WebViewType.UNKNOWN &&
                    majorVersion != null &&
                    majorVersion < requiredVersion
        }

        /**
         * Display an alert dialog if the WebView component version installed on the device is
         * less than the `requiredVersion`. The user can tap "Update" to update the
         * corresponding "Google Chrome" or "Android System WebView" app in the Play Store.
         *
         * @return True if the WebView is outdated and the dialog is displayed, otherwise false.
         */
        fun displayUpdateDialogIfOutdated(activity: Activity, requiredVersion: Int): Boolean {
            val versionInfo = Hotwire.webViewInfo(activity)

            return if (isOutdated(activity, requiredVersion)) {
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

                true
            } else {
                false
            }
        }
    }
}
