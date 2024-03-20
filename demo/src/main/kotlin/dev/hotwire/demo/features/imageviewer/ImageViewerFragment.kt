package dev.hotwire.demo.features.imageviewer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import dev.hotwire.core.navigation.fragments.HotwireFragment
import dev.hotwire.core.turbo.nav.TurboNavGraphDestination
import dev.hotwire.core.turbo.util.displayBackButtonAsCloseIcon
import dev.hotwire.demo.R

@TurboNavGraphDestination(uri = "turbo://fragment/image_viewer")
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
        view.findViewById<ImageView>(R.id.image_view)?.let {
            Glide.with(this).load(location).into(it)
        }
    }
}
