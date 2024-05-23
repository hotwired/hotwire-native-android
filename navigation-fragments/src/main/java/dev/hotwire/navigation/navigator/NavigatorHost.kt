package dev.hotwire.navigation.navigator

import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import dev.hotwire.navigation.activities.HotwireActivity
import dev.hotwire.navigation.config.Hotwire

open class NavigatorHost : NavHostFragment() {
    internal lateinit var activity: HotwireActivity
    lateinit var navigator: Navigator
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity = requireActivity() as HotwireActivity
        activity.delegate.registerNavigatorHost(this)
        navigator = Navigator(this, configuration)

        initControllerGraph()
    }

    override fun onDestroy() {
        super.onDestroy()
        activity.delegate.unregisterNavigatorHost(this)
    }

    internal fun initControllerGraph() {
        navController.apply {
            graph = NavigatorGraphBuilder(
                startLocation = configuration.startLocation,
                pathConfiguration = Hotwire.config.pathConfiguration,
                navController = findNavController()
            ).build(
                registeredFragments = Hotwire.registeredFragmentDestinations
            )
        }
    }

    private val configuration get() = activity.navigatorConfigurations().firstOrNull {
        id == it.navigatorHostId
    } ?: throw IllegalStateException("No configuration found for NavigatorHost")
}
