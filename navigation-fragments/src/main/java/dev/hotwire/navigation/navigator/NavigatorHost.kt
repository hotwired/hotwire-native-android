package dev.hotwire.navigation.navigator

import android.os.Bundle
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.Companion.PROTECTED
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentOnAttachListener
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import dev.hotwire.core.config.Hotwire
import dev.hotwire.navigation.activities.HotwireActivity
import dev.hotwire.navigation.config.HotwireNavigation

internal const val DEEPLINK_EXTRAS_KEY = "android-support-nav:controller:deepLinkExtras"
internal const val LOCATION_KEY = "location"

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

    /**
     * Google's Navigation library automatically navigates to deep links provided in the
     * Activity's Intent. This exposes a vulnerability for malicious Intents to open an arbitrary
     * webpage outside of the app's domain, allowing javascript injection on the page. Ensure
     * that deep link intents always match the app's domain.
     */
    @VisibleForTesting(otherwise = PROTECTED)
    fun ensureDeeplinkStartLocationValid() {
        val extrasBundle = activity.intent.extras?.getBundle(DEEPLINK_EXTRAS_KEY) ?: return
        val startLocation = extrasBundle.getString(LOCATION_KEY) ?: return

        val deepLinkStartUri = startLocation.toUri()
        val configStartUri = configuration.startLocation.toUri()

        if (deepLinkStartUri.host != configStartUri.host) {
            extrasBundle.putString(LOCATION_KEY, configuration.startLocation)
            activity.intent.putExtra(DEEPLINK_EXTRAS_KEY, extrasBundle)
        }
    }

    private val configuration get() = activity.navigatorConfigurations().firstOrNull {
        id == it.navigatorHostId
    } ?: throw IllegalStateException("No configuration found for NavigatorHost")
}
