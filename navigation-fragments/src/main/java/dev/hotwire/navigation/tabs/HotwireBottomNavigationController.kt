package dev.hotwire.navigation.tabs

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.os.Bundle
import android.view.View
import androidx.core.animation.doOnEnd
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.FragmentNavigator
import com.google.android.material.bottomnavigation.BottomNavigationView
import dev.hotwire.core.turbo.nav.PresentationContext
import dev.hotwire.core.turbo.visit.VisitOptions
import dev.hotwire.navigation.activities.HotwireActivity
import dev.hotwire.navigation.navigator.NavigatorHost
import dev.hotwire.navigation.navigator.presentationContext

/**
 * A [BottomNavigationView] controller that manages multiple [HotwireBottomTab]s, each associated
 * with its own [NavigatorHost] instance in the Activity layout.
 */
class HotwireBottomNavigationController(
    val activity: HotwireActivity,
    val view: BottomNavigationView,
    val initialVisibility: Visibility = Visibility.DEFAULT,
    val clearNavigationOnTabReselection: Boolean = true,
    val animateVisibilityChanges: Boolean = true
) : NavController.OnDestinationChangedListener {

    /**
     * The visibility mode for the `BottomNavigationView`.
     */
    enum class Visibility {
        /**
         * Visible by default, but hidden in modal screens and when the
         * virtual keyboard is present on screen.
         */
        DEFAULT,

        /**
         * Always hidden.
         */
        HIDDEN
    }

    private var keyboardVisible = false
        set(value) {
            field = value
            updateVisibility()
        }

    private var destinationIsModal = false
        set(value) {
            field = value
            updateVisibility()
        }

    private var listener: ((Int, HotwireBottomTab) -> Unit)? = null

    /**
     * Set the visibility of the `BottomNavigationView`.
     */
    var visibility = initialVisibility
        set(value) {
            field = value
            updateVisibility()
        }

    /**
     * The currently selected tab in the [BottomNavigationView].
     */
    val currentTab: HotwireBottomTab
        get() {
            require(tabs.isNotEmpty()) { "No tabs have been loaded." }
            return tabs[view.selectedItemId]
        }

    /**
     * The tabs that have been loaded into the [BottomNavigationView].
     */
    var tabs = listOf<HotwireBottomTab>()
        private set

    /**
     * Load the tabs and their navigator configurations into the [BottomNavigationView].
     *
     * @param tabs The list of [HotwireBottomTab] instances that correspond to the
     *  [BottomNavigationView] tabs.
     * @param selectedTabIndex The index of the initially selected tab.
     */
    fun load(
        tabs: List<HotwireBottomTab>,
        selectedTabIndex: Int = 0
    ) {
        require(tabs.isNotEmpty()) { "Tabs cannot be empty." }
        removeDestinationChangedListener()

        this.tabs = tabs

        val initialIndex = selectedTabIndex.coerceIn(0, tabs.lastIndex)
        val initialTab = tabs[initialIndex]

        loadMenu()
        selectTab(initialIndex)
        initOnItemSelectedListener()
        initDestinationChangedListener()
        applyWindowInsets()
        switchTab(initialTab)
    }

    /**
     * Select the tab for the provided index.
     */
    fun selectTab(tabIndex: Int) {
        view.selectedItemId = tabIndex.coerceIn(0, tabs.lastIndex)
    }

    /**
     * Set a listener that will be notified when a navigation tab is selected.
     */
    fun setOnTabSelectedListener(listener: ((index: Int, tab: HotwireBottomTab) -> Unit)?) {
        this.listener = listener
    }

    /**
     * Routes to the specified location in the current tab. The resulting destination and
     * its presentation will be determined using the path configuration rules.
     *
     * @param location The location to navigate to.
     * @param options Visit options to apply to the visit. (optional)
     * @param bundle Bundled arguments to pass to the destination. (optional)
     * @param extras Extras that can be passed to enable Fragment specific behavior. (optional)
     */
    fun route(
        location: String,
        options: VisitOptions = VisitOptions(),
        bundle: Bundle? = null,
        extras: FragmentNavigator.Extras? = null
    ) {
        activity.delegate.currentNavigator?.route(location, options, bundle, extras)
    }

    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {
        if (activity.delegate.currentNavigator?.host?.navController == controller) {
            destinationIsModal = arguments?.presentationContext == PresentationContext.MODAL
        }
    }

    private fun loadMenu() {
        view.menu.clear()

        tabs.forEachIndexed { index, tab ->
            view.menu.add(0, index, index, tab.title).apply {
                setIcon(tab.iconResId)
                isCheckable = true
                isEnabled = tab.isVisible
                isVisible = tab.isVisible
            }
        }
    }

    private fun initOnItemSelectedListener() {
        view.setOnItemSelectedListener { item ->
            val tab = tabs[item.itemId]
            switchTab(tab)
            listener?.invoke(item.itemId, tab)
            true
        }

        if (clearNavigationOnTabReselection) {
            view.setOnItemReselectedListener { item ->
                val tab = tabs[item.itemId]
                tab.navigatorHost.navigator.clearAll()
            }
        }
    }

    private fun initDestinationChangedListener() {
        tabs.forEach {
            it.navigatorHost.navController.addOnDestinationChangedListener(this)
        }
    }

    private fun removeDestinationChangedListener() {
        if (tabs.isEmpty()) return

        tabs.forEach {
            it.navigatorHost.navController.removeOnDestinationChangedListener(this)
        }
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            insets.getInsets(WindowInsetsCompat.Type.systemBars()).apply {
                v.setPadding(left, 0, right, bottom)
            }

            keyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            insets
        }
    }

    private fun updateVisibility() {
        val visible = !keyboardVisible && !destinationIsModal && visibility != Visibility.HIDDEN

        if (visible != view.isVisible) {
            if (animateVisibilityChanges) {
                view.translationYAnimator(200, visible).start()
            } else {
                view.isVisible = !keyboardVisible && !destinationIsModal
            }
        }
    }

    private fun switchTab(tab: HotwireBottomTab) {
        activity.delegate.setCurrentNavigator(tab.configuration)

        tabs.forEach {
            val navigatorHostView = activity.findViewById<View>(it.configuration.navigatorHostId)
            navigatorHostView?.isVisible = it == tab
        }
    }

    private val HotwireBottomTab.navigatorHost: NavigatorHost
        get() {
            val fragment = activity.supportFragmentManager.findFragmentById(configuration.navigatorHostId)
            return fragment as NavigatorHost
        }

    private fun View.translationYAnimator(
        duration: Long,
        visibleOnEnd: Boolean
    ): ObjectAnimator {
        isVisible = true

        val startY = if (visibleOnEnd) height.toFloat() else 0f
        val endY = if (visibleOnEnd) 0f else height.toFloat()
        val values = PropertyValuesHolder.ofFloat("translationY", startY, endY)

        return ObjectAnimator.ofPropertyValuesHolder(this, values).apply {
            this.duration = duration
            this.doOnEnd {
                isVisible = visibleOnEnd
            }
        }
    }
}
