package dev.hotwire.navigation.fragments

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.hotwire.core.turbo.config.title
import dev.hotwire.navigation.R
import dev.hotwire.navigation.destinations.HotwireDestination
import dev.hotwire.navigation.destinations.HotwireDialogDestination
import dev.hotwire.navigation.navigator.Navigator
import dev.hotwire.navigation.navigator.NavigatorHost

/**
 * The base class from which all bottom sheet native fragments in a
 * Hotwire app should extend from.
 *
 * For web bottom sheet fragments, refer to [HotwireWebBottomSheetFragment].
 */
abstract class HotwireBottomSheetFragment : BottomSheetDialogFragment(),
    HotwireDestination, HotwireDialogDestination {
    internal lateinit var delegate: HotwireFragmentDelegate

    override val navigator: Navigator
        get() = (parentFragment as NavigatorHost).navigator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        delegate = HotwireFragmentDelegate(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navigator.currentDialogDestination = this
        delegate.onViewCreated()

        if (shouldObserveTitleChanges()) {
            observeTitleChanges()
            pathProperties.title?.let {
                fragmentViewModel.setTitle(it)
            }
        }
    }

    override fun onDestroyView() {
        navigator.currentDialogDestination = null
        super.onDestroyView()
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
        delegate.onStart()
    }

    override fun onStop() {
        super.onStop()
        delegate.onStop()
    }

    override fun onCancel(dialog: DialogInterface) {
        delegate.onDialogCancel()
        super.onCancel(dialog)
    }

    override fun onDismiss(dialog: DialogInterface) {
        delegate.onDialogDismiss()
        super.onDismiss(dialog)
    }

    override fun closeDialog() {
        requireDialog().cancel()
        navigator.currentDialogDestination = null
    }

    override fun onBeforeNavigation() {}

    override fun refresh(displayProgress: Boolean) {}

    override fun prepareNavigation(onReady: () -> Unit) {
        delegate.prepareNavigation(onReady)
    }

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

    private fun observeTitleChanges() {
        fragmentViewModel.title.observe(viewLifecycleOwner) {
            toolbarForNavigation()?.title = it
        }
    }
}
