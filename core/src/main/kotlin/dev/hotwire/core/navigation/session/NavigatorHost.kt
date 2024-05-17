package dev.hotwire.core.navigation.session

import android.content.Context
import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import dev.hotwire.core.config.Hotwire
import dev.hotwire.core.config.Hotwire.pathConfiguration
import dev.hotwire.core.navigation.activities.HotwireActivity
import dev.hotwire.core.turbo.nav.Navigator
import dev.hotwire.core.turbo.nav.TurboNavGraphBuilder
import dev.hotwire.core.turbo.session.Session
import dev.hotwire.core.turbo.views.TurboWebView

open class NavigatorHost : NavHostFragment() {
    internal lateinit var activity: HotwireActivity
    lateinit var navigator: Navigator
        private set

    val sessionConfiguration get() = activity.sessionConfigurations().first {
        id == it.navHostFragmentId
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity = requireActivity() as HotwireActivity
        activity.delegate.registerNavigatorHost(this)
        navigator = Navigator(this)

        initControllerGraph()
    }

    override fun onDestroy() {
        super.onDestroy()
        activity.delegate.unregisterNavigatorHost(this)
    }

    /**
     * Called whenever a new WebView instance needs to be (re)created. You can
     * override this to provide your own [TurboWebView] subclass if you need
     * custom behaviors.
     */
    open fun onCreateWebView(context: Context): TurboWebView {
        return TurboWebView(context, null)
    }

    internal fun initControllerGraph() {
        navController.apply {
            graph = TurboNavGraphBuilder(
                startLocation = sessionConfiguration.startLocation,
                pathConfiguration = pathConfiguration,
                navController = findNavController()
            ).build(
                registeredFragments = Hotwire.registeredFragmentDestinations
            )
        }
    }
}
