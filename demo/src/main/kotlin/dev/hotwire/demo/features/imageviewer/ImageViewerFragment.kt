package dev.hotwire.demo.features.imageviewer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import coil.load
import dev.hotwire.demo.R
import dev.hotwire.navigation.destinations.HotwireDestinationDeepLink
import dev.hotwire.navigation.fragments.HotwireFragment
import dev.hotwire.navigation.util.displayBackButtonAsCloseIcon

@HotwireDestinationDeepLink(uri = "hotwire://fragment/image_viewer")
class ImageViewerFragment : HotwireFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_image_viewer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initToolbar()
        loadImage(view)
    }

    private fun initToolbar() {
        toolbarForNavigation()?.displayBackButtonAsCloseIcon()
    }

    private fun loadImage(view: View) {
        view.findViewById<ImageView>(R.id.image_view)?.load(location)
    }
}
