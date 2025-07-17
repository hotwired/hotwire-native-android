package dev.hotwire.navigation.navigator

import android.os.Bundle
import android.view.View
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentOnAttachListener
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import dev.hotwire.core.config.Hotwire
import dev.hotwire.navigation.activities.HotwireActivity
import dev.hotwire.navigation.config.HotwireNavigation

open class NavigatorHost : NavHostFragment(), FragmentOnAttachListener {
    internal lateinit var activity: HotwireActivity
    lateinit var navigator: Navigator
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity = requireActivity() as HotwireActivity
        navigator = Navigator(this, configuration, activity)
        childFragmentManager.addFragmentOnAttachListener(this)

        initControllerGraph()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity.delegate.registerNavigatorHost(this)
    }

    override fun onAttachFragment(fragmentManager: FragmentManager, fragment: Fragment) {
        activity.delegate.onNavigatorHostReady(this)
        childFragmentManager.removeFragmentOnAttachListener(this)
    }

    override fun onDestroy() {
        activity.delegate.unregisterNavigatorHost(this)
        super.onDestroy()
    }

    /**
     * Returns whether the navigation host is ready for navigation. It is not
     * ready for navigation if the view is not attached or the start destination
     * has not been created yet.
     */
    fun isReady(): Boolean {
        return isAdded && !isDetached && childFragmentManager.primaryNavigationFragment != null
    }

    internal fun initControllerGraph() {
        ensureDeeplinkStartLocationValid()

        navController.apply {
            graph = NavigatorGraphBuilder(
                navigatorName = configuration.name,
                startLocation = configuration.startLocation,
                pathConfiguration = Hotwire.config.pathConfiguration,
                navController = findNavController()
            ).build(
                registeredFragments = HotwireNavigation.registeredFragmentDestinations
            )
        }
    }

    private fun ensureDeeplinkStartLocationValid() {
        val deepLinkExtrasKey = "android-support-nav:controller:deepLinkExtras"
        val locationKey = "location"

        val extrasBundle = activity.intent.extras?.getBundle(deepLinkExtrasKey) ?: return
        val startLocation = extrasBundle.getString(locationKey) ?: return

        val deepLinkStartUri = startLocation.toUriOrNull()
        val configStartUri = configuration.startLocation.toUriOrNull()

        if (deepLinkStartUri?.host != configStartUri?.host) {
            extrasBundle.putString(locationKey, configuration.startLocation)
            activity.intent.putExtra(deepLinkExtrasKey, extrasBundle)
        }
    }

    private fun String.toUriOrNull() = runCatching { toUri() }.getOrNull()

    private val configuration get() = activity.navigatorConfigurations().firstOrNull {
        id == it.navigatorHostId
    } ?: throw IllegalStateException("No configuration found for NavigatorHost")
}
