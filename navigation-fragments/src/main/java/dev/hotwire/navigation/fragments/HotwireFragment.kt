package dev.hotwire.navigation.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import dev.hotwire.core.turbo.config.context
import dev.hotwire.core.turbo.config.title
import dev.hotwire.core.turbo.nav.PresentationContext
import dev.hotwire.navigation.R
import dev.hotwire.navigation.destinations.HotwireDestination
import dev.hotwire.navigation.navigator.Navigator
import dev.hotwire.navigation.navigator.NavigatorHost
import dev.hotwire.navigation.session.SessionModalResult

/**
 * The base class from which all "standard" native Fragments (non-dialogs) in a
 * Turbo-driven app should extend from.
 *
 * For web fragments, refer to [HotwireWebFragment].
 */
abstract class HotwireFragment : Fragment(), HotwireDestination {
    internal lateinit var delegate: HotwireFragmentDelegate

    override val navigator: Navigator
        get() = (parentFragment as NavigatorHost).navigator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        delegate = HotwireFragmentDelegate(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        delegate.onViewCreated()

        observeModalResult()
        observeDialogResult()

        if (shouldObserveTitleChanges()) {
            observeTitleChanges()
            pathProperties.title?.let {
                fragmentViewModel.setTitle(it)
            }
        }
    }

    /**
     * This is marked `final` to prevent further use, as it's now deprecated in
     * AndroidX's Fragment implementation.
     *
     * Use [onViewCreated] for code touching
     * the Fragment's view and [onCreate] for other initialization.
     */
    @Suppress("DEPRECATION", "OverrideDeprecatedMigration")
    final override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    /**
     * This is marked `final` to prevent further use, as it's now deprecated in
     * AndroidX's Fragment implementation.
     *
     * Use [registerForActivityResult] with the appropriate
     * [androidx.activity.result.contract.ActivityResultContract] and its callback.
     *
     * Turbo provides the [HotwireDestination.activityResultLauncher] interface
     * to obtain registered result launchers from any destination.
     */
    @Suppress("DEPRECATION", "OverrideDeprecatedMigration")
    final override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
    }

    override fun onStart() {
        super.onStart()

        if (!delegate.sessionViewModel.modalResultExists) {
            delegate.onStart()
        }
    }

    override fun onStop() {
        super.onStop()
        delegate.onStop()
    }

    override fun prepareNavigation(onReady: () -> Unit) {
        delegate.prepareNavigation(onReady)
    }

    /**
     * Called when the Fragment has been started again after receiving a
     * modal result. Will navigate if the result indicates it should.
     */
    open fun onStartAfterModalResult(result: SessionModalResult) {
        delegate.onStartAfterModalResult(result)
    }

    /**
     * Called when the Fragment has been started again after a dialog has
     * been dismissed/canceled and no result is passed back.
     */
    open fun onStartAfterDialogCancel() {
        if (!delegate.sessionViewModel.modalResultExists) {
            delegate.onStartAfterDialogCancel()
        }
    }

    override fun onBeforeNavigation() {}

    override fun refresh(displayProgress: Boolean) {}

    /**
     * Gets the Toolbar instance in your Fragment's view for use with
     * navigation. The title in the Toolbar will automatically be
     * updated if a title is available. By default, Turbo will look
     * for a Toolbar with resource ID `R.id.toolbar`. Override to
     * provide a Toolbar instance with a different ID.
     */
    override fun toolbarForNavigation(): Toolbar? {
        return view?.findViewById(R.id.toolbar)
    }

    final override fun delegate(): HotwireFragmentDelegate {
        return delegate
    }

    private fun observeModalResult() {
        if (shouldHandleModalResults()) {
            delegate.sessionViewModel.modalResult.observe(viewLifecycleOwner) { event ->
                event.getContentIfNotHandled()?.let {
                    onStartAfterModalResult(it)
                }
            }
        }
    }

    private fun observeDialogResult() {
        delegate.sessionViewModel.dialogResult.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                onStartAfterDialogCancel()
            }
        }
    }

    private fun observeTitleChanges() {
        fragmentViewModel.title.observe(viewLifecycleOwner) {
            toolbarForNavigation()?.title = it
        }
    }

    private fun shouldHandleModalResults(): Boolean {
        // Only handle modal results in non-modal contexts
        return pathProperties.context != PresentationContext.MODAL
    }
}
