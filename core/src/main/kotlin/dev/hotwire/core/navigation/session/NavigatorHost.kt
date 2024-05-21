package dev.hotwire.core.navigation.session

import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import dev.hotwire.core.config.Hotwire
import dev.hotwire.core.config.Hotwire.pathConfiguration
import dev.hotwire.core.navigation.activities.HotwireActivity
import dev.hotwire.core.turbo.nav.Navigator
import dev.hotwire.core.turbo.nav.TurboNavGraphBuilder

open class NavigatorHost : NavHostFragment() {
    internal lateinit var activity: HotwireActivity
    lateinit var navigator: Navigator
        private set

    val configuration get() = activity.navigatorConfigurations().first {
        id == it.navigatorHostId
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

    internal fun initControllerGraph() {
        navController.apply {
            graph = TurboNavGraphBuilder(
                startLocation = configuration.startLocation,
                pathConfiguration = pathConfiguration,
                navController = findNavController()
            ).build(
                registeredFragments = Hotwire.registeredFragmentDestinations
            )
        }
    }
}
