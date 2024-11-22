package dev.hotwire.navigation.fragments

import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Holds onto fragment-level state data.
 */
class HotwireFragmentViewModel : ViewModel() {
    val title: MutableLiveData<String> = MutableLiveData()

    /**
     * Set's the Fragment destination's title.
     */
    fun setTitle(newTitle: String) {
        title.value = newTitle
    }

    companion object {
        fun get(location: String, fragment: Fragment): HotwireFragmentViewModel {
            return ViewModelProvider(fragment).get(
                location, HotwireFragmentViewModel::class.java
            )
        }
    }
}
