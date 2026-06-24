package dev.hotwire.navigation.navigator

import android.content.Intent
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
internal const val DEEPLINK_ARGS_KEY = "android-support-nav:controller:deepLinkArgs"
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
     * Google's Navigation library automatically navigates to deep links provided in the launching
     * Intent, which lets a malicious Intent open an arbitrary page in the WebView. Intents the app
     * produced itself are trusted; any other Intent has its attacker-controllable deep-link
     * arguments sanitized so the start location stays within the app's domain.
     */
    @VisibleForTesting(otherwise = PROTECTED)
    fun ensureDeeplinkStartLocationValid() {
        val intent = activity.intent
        if (shouldTrustIntent(intent)) return

        // NavController merges deepLinkArgs over the validated deepLinkExtras (last write wins), so
        // empty each per-destination bundle to stop it overriding the validated start location.
        intent.extras?.getParcelableArrayList<Bundle>(DEEPLINK_ARGS_KEY)?.let { args ->
            intent.putParcelableArrayListExtra(DEEPLINK_ARGS_KEY, ArrayList(args.map { Bundle() }))
        }

        val extrasBundle = intent.extras?.getBundle(DEEPLINK_EXTRAS_KEY) ?: return
        val startLocation = extrasBundle.getString(LOCATION_KEY) ?: return

        val deepLinkStartUri = startLocation.toUri()
        val configStartUri = configuration.startLocation.toUri()

        if (deepLinkStartUri.host != configStartUri.host) {
            extrasBundle.putString(LOCATION_KEY, configuration.startLocation)
            intent.putExtra(DEEPLINK_EXTRAS_KEY, extrasBundle)
        }
    }

    private fun shouldTrustIntent(intent: Intent): Boolean {
        // EXTRA_REFERRER / EXTRA_REFERRER_NAME back Activity.referrer and are attacker-settable, so a
        // self-origin claim that relies on them can't be trusted.
        if (intent.hasExtra(Intent.EXTRA_REFERRER) || intent.hasExtra(Intent.EXTRA_REFERRER_NAME)) {
            return false
        }
        val caller = activity.callingPackage ?: activity.referrer?.authority
        return caller == activity.packageName
    }

    private val configuration get() = activity.navigatorConfigurations().firstOrNull {
        id == it.navigatorHostId
    } ?: throw IllegalStateException("No configuration found for NavigatorHost")
}
