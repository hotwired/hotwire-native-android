package dev.hotwire.core.files.delegates

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient.FileChooserParams
import androidx.activity.result.ActivityResult
import dev.hotwire.core.R
import dev.hotwire.core.files.util.HOTWIRE_REQUEST_CODE_FILES
import dev.hotwire.core.files.util.HotwireFileProvider
import dev.hotwire.core.logging.logError
import dev.hotwire.core.turbo.session.Session
import dev.hotwire.core.turbo.util.dispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class FileChooserDelegate(val session: Session) : CoroutineScope {
    private val context: Context = session.context
    private var uploadCallback: ValueCallback<Array<Uri>>? = null
    private val browseFilesDelegate = BrowseFilesDelegate(context)
    private val cameraCaptureDelegate = CameraCaptureDelegate(context)

    override val coroutineContext: CoroutineContext
        get() = dispatcherProvider.io + Job()

    fun onShowFileChooser(
        filePathCallback: ValueCallback<Array<Uri>>,
        params: FileChooserParams
    ): Boolean {
        uploadCallback = filePathCallback

        return openChooser(params).also { success ->
            if (!success) handleCancellation()
        }
    }

    fun onActivityResult(result: ActivityResult) {
        when (result.resultCode) {
            Activity.RESULT_OK -> handleResult(result.data)
            Activity.RESULT_CANCELED -> handleCancellation()
        }
    }

    fun deleteCachedFiles() {
        launch {
            HotwireFileProvider.deleteAllFiles(context)
        }
    }

    private fun openChooser(params: FileChooserParams): Boolean {
        val cameraIntent =  cameraCaptureDelegate.buildIntent(params)
        val chooserIntent = browseFilesDelegate.buildIntent(params)
        val extraIntents = listOfNotNull(cameraIntent).toTypedArray()

        val intent = Intent(Intent.ACTION_CHOOSER).apply {
            putExtra(Intent.EXTRA_INTENT, chooserIntent)
            putExtra(Intent.EXTRA_TITLE, params.title())
            putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents)
        }

        return startIntent(intent)
    }

    private fun startIntent(intent: Intent): Boolean {
        val destination = session.currentVisit?.callback?.visitDestination() ?: return false

        return try {
            destination.activityResultLauncher(HOTWIRE_REQUEST_CODE_FILES)?.launch(intent)
            true
        } catch (e: Exception) {
            logError("startIntentError", e)
            false
        }
    }

    private fun handleResult(intent: Intent?) {
        when (intent.containsFileResult()) {
            true -> browseFilesDelegate.handleResult(intent) { sendResult(it) }
            else -> cameraCaptureDelegate.handleResult { sendResult(it) }
        }
    }

    private fun sendResult(results: Array<Uri>?) {
        uploadCallback?.onReceiveValue(results)
        uploadCallback = null
    }

    private fun handleCancellation() {
        // Important to send a null value to the upload callback, otherwise the webview
        // gets into a state where it doesn't allow the file chooser to open again.
        uploadCallback?.onReceiveValue(null)
        uploadCallback = null
    }

    private fun FileChooserParams.title(): String {
        return title?.toString() ?: when (allowsMultiple()) {
            true -> session.context.getString(R.string.hotwire_file_chooser_select_multiple)
            else -> session.context.getString(R.string.hotwire_file_chooser_select)
        }
    }

    private fun Intent?.containsFileResult(): Boolean {
        return this?.dataString != null || this?.clipData != null
    }
}

internal fun FileChooserParams.allowsMultiple(): Boolean {
    return mode == FileChooserParams.MODE_OPEN_MULTIPLE
}

internal fun FileChooserParams.acceptsAny(): Boolean {
    return defaultAcceptType() == "*/*"
}

internal fun FileChooserParams.defaultAcceptType(): String {
    return when {
        acceptTypes.isEmpty() -> "*/*"
        acceptTypes.first().isBlank() -> "*/*"
        else -> acceptTypes.first()
    }
}
