package dev.hotwire.demo.features.numbers

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.textview.MaterialTextView
import dev.hotwire.core.turbo.config.PathConfigurationProperties
import dev.hotwire.demo.R
import dev.hotwire.navigation.destinations.HotwireDestinationDeepLink
import dev.hotwire.navigation.fragments.HotwireBottomSheetFragment

@HotwireDestinationDeepLink(uri = "hotwire://fragment/numbers/sheet")
class NumberBottomSheetFragment : HotwireBottomSheetFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_number_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
    }

    private fun initView(view: View) {
        view.findViewById<MaterialTextView>(R.id.number).apply {
            text = Uri.parse(location).lastPathSegment
        }

        view.findViewById<MaterialTextView>(R.id.number_description).apply {
            text = pathProperties.description
        }
    }

    private val PathConfigurationProperties.description: String?
        get() = get("description")?.toString()
}
