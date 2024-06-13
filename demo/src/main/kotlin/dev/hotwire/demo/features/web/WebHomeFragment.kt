package dev.hotwire.demo.features.web

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dev.hotwire.core.turbo.errors.VisitError
import dev.hotwire.navigation.destinations.HotwireDestinationDeepLink
import dev.hotwire.demo.R

@HotwireDestinationDeepLink(uri = "hotwire://fragment/web/home")
class WebHomeFragment : WebFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_web_home, container, false)
    }

    @SuppressLint("InflateParams")
    override fun createErrorView(error: VisitError): View {
        return layoutInflater.inflate(R.layout.error_web_home, null)
    }

    override fun shouldObserveTitleChanges(): Boolean {
        return false
    }
}
