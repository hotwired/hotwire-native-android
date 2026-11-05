package dev.hotwire.navigation.navigator

import android.content.Intent
import android.os.Bundle
import android.view.View
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

    /**
     * Whether the navigation graph has been built and the start destination
     * loaded. See [initControllerGraphIfNeeded].
     */
    internal var isGraphInitialized = false
        private set

    // The configuration's start location can change before onDestroy.
    private var registeredStartLocation: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        activity = requireActivity() as HotwireActivity

        // NavHostFragment sets a graph supplied through its arguments or XML here. That graph can
        // match a deep link by the Intent's action alone, so it gets an empty Intent.
        withoutLaunchIntentNavigation(launchIntentCopy = { Intent() }) {
            super.onCreate(savedInstanceState)
        }

        registeredStartLocation = configuration.startLocation.also {
            Hotwire.config.registerStartLocation(it)
        }
        navigator = Navigator(this, configuration, activity)
        childFragmentManager.addFragmentOnAttachListener(this)
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
        registeredStartLocation?.let { Hotwire.config.unregisterStartLocation(it) }
        registeredStartLocation = null
        activity.delegate.unregisterNavigatorHost(this)

        // Each host owns its WebView, so nothing uses it after the host is destroyed, including
        // after an Activity recreation. Destroy it now so its page stops running.
        navigator.destroySession()

        super.onDestroy()
    }

    /**
     * Returns whether the navigation host is ready for navigation. It is not
     * ready for navigation if the view is not attached or the start destination
     * has not been created yet.
     */
    fun isReady(): Boolean {
        return isGraphInitialized
                && isAdded
                && !isDetached
                && childFragmentManager.primaryNavigationFragment != null
    }

    /**
     * Builds the navigation graph and loads the start destination if it hasn't
     * been built already. This is idempotent, so it's safe to call whenever the
     * host may need to become ready for navigation (e.g. when its tab is selected).
     */
    internal fun initControllerGraphIfNeeded() {
        if (!isGraphInitialized) {
            initControllerGraph()
        }
    }

    internal fun resetControllerGraph() {
        initControllerGraph()
    }

    private fun initControllerGraph() {
        isGraphInitialized = true

        val graph = NavigatorGraphBuilder(
            navigatorName = configuration.name,
            startLocation = configuration.startLocation,
            pathConfiguration = Hotwire.config.pathConfiguration,
            navController = findNavController()
        ).build(
            registeredFragments = HotwireNavigation.registeredFragmentDestinations
        )

        withoutLaunchIntentNavigation {
            navController.graph = graph
        }
    }

    /**
     * Google's Navigation library navigates to the launching Intent's deep link when a graph is
     * set: to the destination and arguments in its deep-link extras, or to the destination whose
     * deep link matches its data. Hotwire navigates from the configuration's start location and
     * its own visits instead, so a graph is only set while the Activity holds a copy of its
     * launching Intent without the deep link. Hotwire's own graph only has deep links by uri, so
     * the copy keeps the Intent's action and the app's extras for app code that runs meanwhile.
     * The Activity gets its launching Intent back right after, unchanged.
     */
    private inline fun withoutLaunchIntentNavigation(
        launchIntentCopy: (Intent) -> Intent = { it.withoutDeepLink() },
        setGraph: () -> Unit
    ) {
        val launchIntent: Intent? = activity.intent
        activity.intent = launchIntent?.let(launchIntentCopy) ?: Intent()

        try {
            setGraph()
        } finally {
            activity.intent = launchIntent
        }
    }

    private fun Intent.withoutDeepLink() = try {
        Intent(this).apply {
            setDataAndType(null, null)
            DEEP_LINK_EXTRAS.forEach { removeExtra(it) }
        }
    } catch (e: RuntimeException) {
        // Reading the extras unparcels them, which fails for a value whose class the app doesn't
        // have. Hide the whole Intent then.
        Intent()
    }

    private companion object {
        val DEEP_LINK_EXTRAS = listOf(
            "android-support-nav:controller:deepLinkIds",
            "android-support-nav:controller:deepLinkArgs",
            "android-support-nav:controller:deepLinkExtras"
        )
    }

    private val configuration get() = activity.navigatorConfigurations().firstOrNull {
        id == it.navigatorHostId
    } ?: throw IllegalStateException("No configuration found for NavigatorHost")
}
