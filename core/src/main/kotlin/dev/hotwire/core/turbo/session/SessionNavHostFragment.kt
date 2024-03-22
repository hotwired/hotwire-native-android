package dev.hotwire.core.turbo.session

import android.content.Context
import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import dev.hotwire.core.bridge.Bridge
import dev.hotwire.core.config.Hotwire
import dev.hotwire.core.config.Hotwire.pathConfiguration
import dev.hotwire.core.navigation.activities.HotwireActivity
import dev.hotwire.core.turbo.nav.HotwireNavDestination
import dev.hotwire.core.turbo.nav.TurboNavGraphBuilder
import dev.hotwire.core.turbo.views.TurboWebView

open class SessionNavHostFragment : NavHostFragment() {
    private val activity get() = requireActivity() as HotwireActivity

    val sessionConfiguration get() = activity.sessionConfigurations().first {
        id == it.navHostFragmentId
    }

    /**
     * The [Session] instance that is shared with all destinations that are
     * hosted inside this [SessionNavHostFragment].
     */
    lateinit var session: Session
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity.delegate.registerNavHostFragment(this)

        createNewSession()
        initControllerGraph()
    }

    override fun onDestroy() {
        super.onDestroy()
        activity.delegate.unregisterNavHostFragment(this)
    }

    internal fun createNewSession() {
        session = Session(
            sessionName = sessionConfiguration.name,
            activity = activity.appCompatActivity,
            webView = onCreateWebView(activity.appCompatActivity)
        )
        onSessionCreated()
    }

    /**
     * Called whenever the [Session] instance has been (re)created. A new
     * session is created whenever the [SessionNavHostFragment] is created
     * and whenever the WebView render process has been terminated and a new
     * WebView instance is required.
     */
    open fun onSessionCreated() {
        // Initialize bridge with new WebView instance
        if (Hotwire.registeredBridgeComponentFactories.isNotEmpty()) {
            Bridge.initialize(session.webView)
        }
    }

    /**
     * Called whenever a new WebView instance needs to be (re)created. You can
     * override this to provide your own [TurboWebView] subclass if you need
     * custom behaviors.
     */
    open fun onCreateWebView(context: Context): TurboWebView {
        return TurboWebView(context, null)
    }

    /**
     * Resets the [SessionNavHostFragment] instance, it's [Session]
     * instance, and the entire navigation graph to its original starting point.
     */
    fun reset(onReset: () -> Unit = {}) {
        currentNavDestination.delegate().navigator.onNavigationVisit {
            currentNavDestination.clearBackStack {
                session.reset()
                initControllerGraph()

                if (view == null) {
                    onReset()
                } else {
                    requireView().post { onReset() }
                }
            }
        }
    }

    /**
     * Retrieves the currently active [HotwireNavDestination] on the backstack.
     */
    val currentNavDestination: HotwireNavDestination
        get() = childFragmentManager.primaryNavigationFragment as HotwireNavDestination?
            ?: throw IllegalStateException("No current destination found in NavHostFragment")

    private fun initControllerGraph() {
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
