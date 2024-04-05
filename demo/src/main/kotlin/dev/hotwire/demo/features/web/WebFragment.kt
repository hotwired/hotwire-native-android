package dev.hotwire.demo.features.web

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.webkit.GeolocationPermissions
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import dev.hotwire.core.navigation.fragments.HotwireWebFragment
import dev.hotwire.core.turbo.errors.HttpError
import dev.hotwire.core.turbo.errors.VisitError
import dev.hotwire.core.turbo.nav.HotwireDestination
import dev.hotwire.core.turbo.views.TurboWebChromeClient
import dev.hotwire.core.turbo.visit.VisitAction.REPLACE
import dev.hotwire.core.turbo.visit.VisitOptions
import dev.hotwire.demo.R
import dev.hotwire.demo.Urls

@HotwireDestination(uri = "turbo://fragment/web")
open class WebFragment : HotwireWebFragment() {
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) refresh()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenu()
    }

    override fun onFormSubmissionStarted(location: String) {
        menuProgress?.isVisible = true
    }

    override fun onFormSubmissionFinished(location: String) {
        menuProgress?.isVisible = false
    }

    override fun onVisitErrorReceived(location: String, error: VisitError) {
        if (error is HttpError.ClientError.Unauthorized) {
            navigate(Urls.signInUrl, VisitOptions(action = REPLACE))
        } else {
            super.onVisitErrorReceived(location, error)
        }
    }

    override fun createWebChromeClient(): TurboWebChromeClient {
        return object : TurboWebChromeClient(session) {
            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?
            ) {
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    permissionLauncher.launch(
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                } else {
                    callback?.invoke(origin, true, false)
                }
            }
        }
    }

    private fun setupMenu() {
        toolbarForNavigation()?.inflateMenu(R.menu.web)
    }

    private val menuProgress: MenuItem?
        get() = toolbarForNavigation()?.menu?.findItem(R.id.menu_progress)
}
