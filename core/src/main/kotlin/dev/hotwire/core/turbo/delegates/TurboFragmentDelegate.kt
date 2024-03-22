package dev.hotwire.core.turbo.delegates

import dev.hotwire.core.lib.logging.logEvent
import dev.hotwire.core.turbo.fragments.TurboFragmentViewModel
import dev.hotwire.core.turbo.nav.HotwireNavDestination
import dev.hotwire.core.turbo.nav.TurboNavigator
import dev.hotwire.core.turbo.session.SessionModalResult
import dev.hotwire.core.turbo.session.SessionViewModel
import dev.hotwire.core.turbo.util.displayBackButton
import dev.hotwire.core.turbo.util.displayBackButtonAsCloseIcon

/**
 * Provides all the hooks for a Fragment to delegate its lifecycle events
 * to this class. Note: This class should not need to be used directly
 * from within your app.
 */
class TurboFragmentDelegate(private val navDestination: HotwireNavDestination) {
    private val fragment = navDestination.fragment
    private val location = navDestination.location
    private val sessionName = navDestination.session.sessionName

    internal val sessionViewModel = SessionViewModel.get(sessionName, fragment.requireActivity())
    internal val fragmentViewModel = TurboFragmentViewModel.get(location, fragment)

    internal lateinit var navigator: TurboNavigator

    /**
     * Should be called by the implementing Fragment during
     * [androidx.fragment.app.Fragment.onViewCreated].
     */
    fun onViewCreated() {
        navigator = TurboNavigator(navDestination)

        initToolbar()
        logEvent("fragment.onViewCreated", "location" to location)
    }

    /**
     * Should be called by the implementing Fragment during
     * [androidx.fragment.app.Fragment.onStart].
     */
    fun onStart() {
        logEvent("fragment.onStart", "location" to location)
    }

    /**
     * Should be called by the implementing Fragment during
     * [androidx.fragment.app.Fragment.onStop].
     */
    fun onStop() {
        logEvent("fragment.onStop", "location" to location)
    }

    /**
     * Provides a hook to Turbo when the Fragment has been started again after a dialog has
     * been dismissed/canceled and no result is passed back.
     */
    fun onStartAfterDialogCancel() {
        logEvent("fragment.onStartAfterDialogCancel", "location" to location)
    }

    /**
     * Provides a hook to Turbo when a Fragment has been started again after receiving a
     * modal result. Will navigate if the result indicates it should.
     */
    fun onStartAfterModalResult(result: SessionModalResult) {
        logEvent("fragment.onStartAfterModalResult", "location" to result.location, "options" to result.options)
        if (result.shouldNavigate) {
            navigator.navigate(result.location, result.options, result.bundle)
        }
    }

    /**
     * Provides a hook to Turbo when the dialog has been canceled. If there is a modal
     * result, an event will be created in [SessionViewModel] that can be observed.
     */
    fun onDialogCancel() {
        logEvent("fragment.onDialogCancel", "location" to location)
        if (!sessionViewModel.modalResultExists) {
            sessionViewModel.sendDialogResult()
        }
    }

    /**
     * Provides a hook to Turbo when the dialog has been dismissed.
     */
    fun onDialogDismiss() {
        logEvent("fragment.onDialogDismiss", "location" to location)
    }

    // ----------------------------------------------------------------------------
    // Private
    // ----------------------------------------------------------------------------

    private fun initToolbar() {
        navDestination.toolbarForNavigation()?.let {
            if (!navigator.isAtStartDestination()) {
                if (navDestination.isModal) {
                    it.displayBackButtonAsCloseIcon()
                } else {
                    it.displayBackButton()
                }
            }

            it.setNavigationOnClickListener {
                navDestination.navigateUp()
            }
        }
    }

    private fun logEvent(event: String, vararg params: Pair<String, Any>) {
        val attributes = params.toMutableList().apply {
            add(0, "session" to sessionName)
            add("fragment" to fragment.javaClass.simpleName)
        }
        logEvent(event, attributes)
    }
}
