package dev.hotwire.demo.features.numbers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.hotwire.demo.R
import dev.hotwire.demo.Urls
import dev.hotwire.navigation.destinations.HotwireDestinationDeepLink
import dev.hotwire.navigation.fragments.HotwireFragment

@HotwireDestinationDeepLink(uri = "turbo://fragment/numbers")
class NumbersFragment : HotwireFragment(), NumbersFragmentCallback {
    private val numbersAdapter = NumbersAdapter(this)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_numbers, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
    }

    private fun initView(view: View) {
        view.findViewById<RecyclerView>(R.id.recycler_view).apply {
            layoutManager = LinearLayoutManager(view.context)
            adapter = numbersAdapter.apply {
                setData((1..100).toList())
            }
        }
    }

    override fun onItemClicked(number: Int) {
        navigator.route("${Urls.numbersUrl}/$number")
    }
}
