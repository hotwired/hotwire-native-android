package dev.hotwire.navigation.navigator

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.navigation.NavDestinationBuilder
import androidx.navigation.NavGraphNavigator
import androidx.navigation.createGraph
import androidx.navigation.navOptions
import androidx.navigation.testing.TestNavHostController
import androidx.navigation.ui.R
import androidx.test.core.app.ApplicationProvider
import dev.hotwire.core.turbo.config.PathConfiguration
import dev.hotwire.core.turbo.config.PathConfiguration.Location
import dev.hotwire.core.turbo.nav.Presentation
import dev.hotwire.core.turbo.nav.PresentationContext
import dev.hotwire.core.turbo.nav.QueryStringPresentation
import dev.hotwire.core.turbo.visit.VisitAction
import dev.hotwire.core.turbo.visit.VisitOptions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Suppress("UsePropertyAccessSyntax")
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class NavigatorRuleTest {
    private lateinit var context: Context
    private lateinit var controller: TestNavHostController
    private lateinit var pathConfiguration: PathConfiguration

    private val homeUrl = "https://hotwired.dev/home"
    private val newHomeUrl = "https://hotwired.dev/new-home"
    private val featureUrl = "https://hotwired.dev/feature"
    private val featureTwoUrl = "https://hotwired.dev/feature-two"
    private val newUrl = "https://hotwired.dev/feature/new"
    private val editUrl = "https://hotwired.dev/feature/edit"
    private val recedeUrl = "https://hotwired.dev/recede_historical_location"
    private val refreshUrl = "https://hotwired.dev/refresh_historical_location"
    private val resumeUrl = "https://hotwired.dev/resume_historical_location"
    private val modalRootUrl = "https://hotwired.dev/custom/modal"
    private val filterUrl = "https://hotwired.dev/feature?filter=true"
    private val customUrl = "https://hotwired.dev/custom"
    private val customQueryUrl = "https://hotwired.dev/custom?id=1"

    private val webDestinationId = 1
    private val webModalDestinationId = 2
    private val webHomeDestinationId = 3

    private val webUri = Uri.parse("hotwire://fragment/web")
    private val webModalUri = Uri.parse("hotwire://fragment/web/modal")
    private val webHomeUri = Uri.parse("hotwire://fragment/web/home")

    private val navigatorName = "test"
    private val extras = null
    private val navOptions = navOptions {
        anim {
            enter = R.anim.nav_default_enter_anim
            exit = R.anim.nav_default_exit_anim
            popEnter = R.anim.nav_default_pop_enter_anim
            popExit = R.anim.nav_default_pop_exit_anim
        }
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        controller = buildControllerWithGraph()
        pathConfiguration = PathConfiguration().apply {
            load(
                context = context,
                location = Location(assetFilePath = "json/test-configuration.json"),
                options = PathConfiguration.LoaderOptions()
            )
        }
    }

    @Test
    fun `navigate within context`() {
        val rule = getNavigatorRule(featureUrl)

        // Current destination
        assertThat(rule.previousLocation).isNull()
        assertThat(rule.currentLocation).isEqualTo(homeUrl)
        assertThat(rule.currentPresentationContext).isEqualTo(PresentationContext.DEFAULT)
        assertThat(rule.isAtStartDestination).isTrue()

        // New destination
        assertThat(rule.newLocation).isEqualTo(featureUrl)
        assertThat(rule.newPresentationContext).isEqualTo(PresentationContext.DEFAULT)
        assertThat(rule.newPresentation).isEqualTo(Presentation.PUSH)
        assertThat(rule.newQueryStringPresentation).isEqualTo(QueryStringPresentation.REPLACE)
        assertThat(rule.newNavigationMode).isEqualTo(NavigatorMode.IN_CONTEXT)
        assertThat(rule.newModalResult).isNull()
        assertThat(rule.newDestinationUri).isEqualTo(webUri)
        assertThat(rule.newDestination).isNotNull()
        assertThat(rule.newNavOptions).isEqualTo(navOptions)
    }

    @Test
    fun `navigate to modal context`() {
        val rule = getNavigatorRule(newUrl)

        // Current destination
        assertThat(rule.previousLocation).isNull()
        assertThat(rule.currentLocation).isEqualTo(homeUrl)
        assertThat(rule.currentPresentationContext).isEqualTo(PresentationContext.DEFAULT)
        assertThat(rule.isAtStartDestination).isTrue()

        // New destination
        assertThat(rule.newLocation).isEqualTo(newUrl)
        assertThat(rule.newPresentationContext).isEqualTo(PresentationContext.MODAL)
        assertThat(rule.newPresentation).isEqualTo(Presentation.PUSH)
        assertThat(rule.newQueryStringPresentation).isEqualTo(QueryStringPresentation.DEFAULT)
        assertThat(rule.newNavigationMode).isEqualTo(NavigatorMode.TO_MODAL)
        assertThat(rule.newModalResult).isNull()
        assertThat(rule.newDestinationUri).isEqualTo(webModalUri)
        assertThat(rule.newDestination).isNotNull()
        assertThat(rule.newNavOptions).isEqualTo(navOptions)
    }

    @Test
    fun `navigate to modal context replacing root`() {
        assertThatThrownBy { getNavigatorRule(modalRootUrl) }
            .isInstanceOf(NavigatorException::class.java)
            .hasMessage("A `modal` destination cannot use presentation `REPLACE_ROOT`")
    }

    @Test
    fun `navigate back to home from default context`() {
        controller.navigate(webDestinationId, locationArgs(featureUrl))
        val rule = getNavigatorRule(homeUrl)

        // Current destination
        assertThat(rule.previousLocation).isEqualTo(homeUrl)
        assertThat(rule.currentLocation).isEqualTo(featureUrl)
        assertThat(rule.currentPresentationContext).isEqualTo(PresentationContext.DEFAULT)
        assertThat(rule.isAtStartDestination).isFalse()

        // New destination
        assertThat(rule.newLocation).isEqualTo(homeUrl)
        assertThat(rule.newPresentationContext).isEqualTo(PresentationContext.DEFAULT)
        assertThat(rule.newPresentation).isEqualTo(Presentation.CLEAR_ALL)
        assertThat(rule.newQueryStringPresentation).isEqualTo(QueryStringPresentation.DEFAULT)
        assertThat(rule.newNavigationMode).isEqualTo(NavigatorMode.IN_CONTEXT)
        assertThat(rule.newModalResult).isNull()
        assertThat(rule.newDestinationUri).isEqualTo(webHomeUri)
        assertThat(rule.newDestination).isNotNull()
        assertThat(rule.newNavOptions).isEqualTo(navOptions)
    }

    @Test
    fun `replace root when navigating in default context`() {
        controller.navigate(webDestinationId, locationArgs(featureUrl))
        val rule = getNavigatorRule(newHomeUrl)

        // Current destination
        assertThat(rule.previousLocation).isEqualTo(homeUrl)
        assertThat(rule.currentLocation).isEqualTo(featureUrl)
        assertThat(rule.currentPresentationContext).isEqualTo(PresentationContext.DEFAULT)
        assertThat(rule.isAtStartDestination).isFalse()

        // New destination
        assertThat(rule.newLocation).isEqualTo(newHomeUrl)
        assertThat(rule.newPresentationContext).isEqualTo(PresentationContext.DEFAULT)
        assertThat(rule.newPresentation).isEqualTo(Presentation.REPLACE_ROOT)
        assertThat(rule.newQueryStringPresentation).isEqualTo(QueryStringPresentation.DEFAULT)
        assertThat(rule.newNavigationMode).isEqualTo(NavigatorMode.IN_CONTEXT)
        assertThat(rule.newModalResult).isNull()
        assertThat(rule.newDestinationUri).isEqualTo(webHomeUri)
        assertThat(rule.newDestination).isNotNull()
        assertThat(rule.newNavOptions.enterAnim).isEqualTo(navOptions.enterAnim)
        assertThat(rule.newNavOptions.exitAnim).isEqualTo(navOptions.exitAnim)
        assertThat(rule.newNavOptions.popEnterAnim).isEqualTo(navOptions.popEnterAnim)
        assertThat(rule.newNavOptions.popExitAnim).isEqualTo(navOptions.popExitAnim)
    }

    @Test
    fun `navigate back to feature from modal context`() {
        controller.navigate(webDestinationId, locationArgs(featureUrl))
        controller.navigate(webModalDestinationId, locationArgs(newUrl))
        val rule = getNavigatorRule(featureUrl)

        // Current destination
        assertThat(rule.previousLocation).isEqualTo(featureUrl)
        assertThat(rule.currentLocation).isEqualTo(newUrl)
        assertThat(rule.currentPresentationContext).isEqualTo(PresentationContext.MODAL)
        assertThat(rule.isAtStartDestination).isFalse()

        // New destination
        assertThat(rule.newLocation).isEqualTo(featureUrl)
        assertThat(rule.newPresentationContext).isEqualTo(PresentationContext.DEFAULT)
        assertThat(rule.newPresentation).isEqualTo(Presentation.POP)
        assertThat(rule.newQueryStringPresentation).isEqualTo(QueryStringPresentation.REPLACE)
        assertThat(rule.newNavigationMode).isEqualTo(NavigatorMode.DISMISS_MODAL)
        assertThat(rule.newModalResult?.location).isEqualTo(featureUrl)
        assertThat(rule.newDestinationUri).isEqualTo(webUri)
        assertThat(rule.newDestination).isNotNull()
        assertThat(rule.newNavOptions).isEqualTo(navOptions)
    }

    @Test
    fun `navigating back to feature from modal context with replace action maintains replace`() {
        controller.navigate(webDestinationId, locationArgs(featureUrl))
        controller.navigate(webModalDestinationId, locationArgs(newUrl))
        val rule = getNavigatorRule(featureUrl, VisitOptions(action = VisitAction.REPLACE))

        // Current destination
        assertThat(rule.previousLocation).isEqualTo(featureUrl)
        assertThat(rule.currentLocation).isEqualTo(newUrl)
        assertThat(rule.currentPresentationContext).isEqualTo(PresentationContext.MODAL)
        assertThat(rule.isAtStartDestination).isFalse()

        // New destination
        assertThat(rule.newLocation).isEqualTo(featureUrl)
        assertThat(rule.newPresentationContext).isEqualTo(PresentationContext.DEFAULT)
        assertThat(rule.newPresentation).isEqualTo(Presentation.POP)
        assertThat(rule.newQueryStringPresentation).isEqualTo(QueryStringPresentation.REPLACE)
        assertThat(rule.newNavigationMode).isEqualTo(NavigatorMode.DISMISS_MODAL)
        assertThat(rule.newModalResult?.location).isEqualTo(featureUrl)
        assertThat(rule.newModalResult?.options?.action).isEqualTo(VisitAction.REPLACE)
        assertThat(rule.newDestinationUri).isEqualTo(webUri)
        assertThat(rule.newDestination).isNotNull()
        assertThat(rule.newNavOptions).isEqualTo(navOptions)
    }

    @Test
    fun `navigating to new feature from modal context with replace action changes to advance`() {
        controller.navigate(webDestinationId, locationArgs(featureUrl))
        controller.navigate(webModalDestinationId, locationArgs(newUrl))
        val rule = getNavigatorRule(featureTwoUrl, VisitOptions(action = VisitAction.REPLACE))

        // Current destination
        assertThat(rule.previousLocation).isEqualTo(featureUrl)
        assertThat(rule.currentLocation).isEqualTo(newUrl)
        assertThat(rule.currentPresentationContext).isEqualTo(PresentationContext.MODAL)
        assertThat(rule.isAtStartDestination).isFalse()

        // New destination
        assertThat(rule.newLocation).isEqualTo(featureTwoUrl)
        assertThat(rule.newPresentationContext).isEqualTo(PresentationContext.DEFAULT)
        assertThat(rule.newPresentation).isEqualTo(Presentation.POP)
        assertThat(rule.newQueryStringPresentation).isEqualTo(QueryStringPresentation.REPLACE)
        assertThat(rule.newNavigationMode).isEqualTo(NavigatorMode.DISMISS_MODAL)
        assertThat(rule.newModalResult?.location).isEqualTo(featureTwoUrl)
        assertThat(rule.newModalResult?.options?.action).isEqualTo(VisitAction.ADVANCE)
        assertThat(rule.newDestinationUri).isEqualTo(webUri)
        assertThat(rule.newDestination).isNotNull()
        assertThat(rule.newNavOptions).isEqualTo(navOptions)
    }

    @Test
    fun `navigate from modal to same modal`() {
        controller.navigate(webModalDestinationId, locationArgs(newUrl))
        val rule = getNavigatorRule(newUrl)

        // Current destination
        assertThat(rule.previousLocation).isEqualTo(homeUrl)
        assertThat(rule.currentLocation).isEqualTo(newUrl)
        assertThat(rule.currentPresentationContext).isEqualTo(PresentationContext.MODAL)
        assertThat(rule.isAtStartDestination).isFalse()

        // New destination
        assertThat(rule.newLocation).isEqualTo(newUrl)
        assertThat(rule.newPresentationContext).isEqualTo(PresentationContext.MODAL)
        assertThat(rule.newPresentation).isEqualTo(Presentation.REPLACE)
        assertThat(rule.newQueryStringPresentation).isEqualTo(QueryStringPresentation.DEFAULT)
        assertThat(rule.newNavigationMode).isEqualTo(NavigatorMode.IN_CONTEXT)
        assertThat(rule.newModalResult).isNull()
        assertThat(rule.newDestinationUri).isEqualTo(webModalUri)
        assertThat(rule.newDestination).isNotNull()
        assertThat(rule.newNavOptions).isEqualTo(navOptions)
    }

    @Test
    fun `navigate from modal to new modal`() {
        controller.navigate(webModalDestinationId, locationArgs(newUrl))
        val rule = getNavigatorRule(editUrl)

        // Current destination
        assertThat(rule.previousLocation).isEqualTo(homeUrl)
        assertThat(rule.currentLocation).isEqualTo(newUrl)
        assertThat(rule.currentPresentationContext).isEqualTo(PresentationContext.MODAL)
        assertThat(rule.isAtStartDestination).isFalse()

        // New destination
        assertThat(rule.newLocation).isEqualTo(editUrl)
        assertThat(rule.newPresentationContext).isEqualTo(PresentationContext.MODAL)
        assertThat(rule.newPresentation).isEqualTo(Presentation.PUSH)
        assertThat(rule.newQueryStringPresentation).isEqualTo(QueryStringPresentation.DEFAULT)
        assertThat(rule.newNavigationMode).isEqualTo(NavigatorMode.IN_CONTEXT)
        assertThat(rule.newModalResult).isNull()
        assertThat(rule.newDestinationUri).isEqualTo(webModalUri)
        assertThat(rule.newDestination).isNotNull()
        assertThat(rule.newNavOptions).isEqualTo(navOptions)
    }

    @Test
    fun `recede historical location in default context pops the current destination`() {
        controller.navigate(webDestinationId, locationArgs(featureUrl))
        val rule = getNavigatorRule(recedeUrl)

        // Current destination
        assertThat(rule.previousLocation).isEqualTo(homeUrl)
        assertThat(rule.currentLocation).isEqualTo(featureUrl)
        assertThat(rule.currentPresentationContext).isEqualTo(PresentationContext.DEFAULT)
        assertThat(rule.isAtStartDestination).isFalse()

        // New destination
        assertThat(rule.newLocation).isEqualTo(recedeUrl)
        assertThat(rule.newPresentationContext).isEqualTo(PresentationContext.DEFAULT)
        assertThat(rule.newPresentation).isEqualTo(Presentation.POP)
        assertThat(rule.newQueryStringPresentation).isEqualTo(QueryStringPresentation.DEFAULT)
        assertThat(rule.newNavigationMode).isEqualTo(NavigatorMode.IN_CONTEXT)
        assertThat(rule.newModalResult).isNull()
        assertThat(rule.newDestination).isNotNull()
        assertThat(rule.newNavOptions).isEqualTo(navOptions)
    }

    @Test
    fun `refresh historical location in default context refreshes the current destination`() {
        controller.navigate(webDestinationId, locationArgs(featureUrl))
        val rule = getNavigatorRule(refreshUrl)

        // Current destination
        assertThat(rule.previousLocation).isEqualTo(homeUrl)
        assertThat(rule.currentLocation).isEqualTo(featureUrl)
        assertThat(rule.currentPresentationContext).isEqualTo(PresentationContext.DEFAULT)
        assertThat(rule.isAtStartDestination).isFalse()

        // New destination
        assertThat(rule.newLocation).isEqualTo(refreshUrl)
        assertThat(rule.newPresentationContext).isEqualTo(PresentationContext.DEFAULT)
        assertThat(rule.newPresentation).isEqualTo(Presentation.REFRESH)
        assertThat(rule.newQueryStringPresentation).isEqualTo(QueryStringPresentation.DEFAULT)
        assertThat(rule.newNavigationMode).isEqualTo(NavigatorMode.REFRESH)
        assertThat(rule.newModalResult).isNull()
        assertThat(rule.newDestinationUri).isEqualTo(webUri)
        assertThat(rule.newDestination).isNotNull()
        assertThat(rule.newNavOptions).isEqualTo(navOptions)
    }

    @Test
    fun `resume historical location in default context does nothing`() {
        controller.navigate(webDestinationId, locationArgs(featureUrl))
        val rule = getNavigatorRule(resumeUrl)

        // Current destination
        assertThat(rule.previousLocation).isEqualTo(homeUrl)
        assertThat(rule.currentLocation).isEqualTo(featureUrl)
        assertThat(rule.currentPresentationContext).isEqualTo(PresentationContext.DEFAULT)
        assertThat(rule.isAtStartDestination).isFalse()

        // New destination
        assertThat(rule.newLocation).isEqualTo(resumeUrl)
        assertThat(rule.newPresentationContext).isEqualTo(PresentationContext.DEFAULT)
        assertThat(rule.newPresentation).isEqualTo(Presentation.NONE)
        assertThat(rule.newQueryStringPresentation).isEqualTo(QueryStringPresentation.DEFAULT)
        assertThat(rule.newNavigationMode).isEqualTo(NavigatorMode.NONE)
        assertThat(rule.newModalResult).isNull()
        assertThat(rule.newDestinationUri).isEqualTo(webUri)
        assertThat(rule.newDestination).isNotNull()
        assertThat(rule.newNavOptions).isEqualTo(navOptions)
    }

    @Test
    fun `recede historical location from modal context dismisses with result`() {
        controller.navigate(webDestinationId, locationArgs(featureUrl))
        controller.navigate(webModalDestinationId, locationArgs(newUrl))
        val rule = getNavigatorRule(recedeUrl)

        // Current destination
        assertThat(rule.previousLocation).isEqualTo(featureUrl)
        assertThat(rule.currentLocation).isEqualTo(newUrl)
        assertThat(rule.currentPresentationContext).isEqualTo(PresentationContext.MODAL)
        assertThat(rule.isAtStartDestination).isFalse()

        // New destination
        assertThat(rule.newLocation).isEqualTo(recedeUrl)
        assertThat(rule.newPresentationContext).isEqualTo(PresentationContext.DEFAULT)
        assertThat(rule.newPresentation).isEqualTo(Presentation.POP)
        assertThat(rule.newQueryStringPresentation).isEqualTo(QueryStringPresentation.DEFAULT)
        assertThat(rule.newNavigationMode).isEqualTo(NavigatorMode.DISMISS_MODAL)
        assertThat(rule.newModalResult).isNotNull()
        assertThat(rule.newModalResult?.location).isEqualTo(recedeUrl)
        assertThat(rule.newDestinationUri).isEqualTo(webUri)
        assertThat(rule.newDestination).isNotNull()
        assertThat(rule.newNavOptions).isEqualTo(navOptions)
    }

    @Test
    fun `refresh historical location from modal context dismisses with result`() {
        controller.navigate(webDestinationId, locationArgs(featureUrl))
        controller.navigate(webModalDestinationId, locationArgs(newUrl))
        val rule = getNavigatorRule(refreshUrl)

        // Current destination
        assertThat(rule.previousLocation).isEqualTo(featureUrl)
        assertThat(rule.currentLocation).isEqualTo(newUrl)
        assertThat(rule.currentPresentationContext).isEqualTo(PresentationContext.MODAL)
        assertThat(rule.isAtStartDestination).isFalse()

        // New destination
        assertThat(rule.newLocation).isEqualTo(refreshUrl)
        assertThat(rule.newPresentationContext).isEqualTo(PresentationContext.DEFAULT)
        assertThat(rule.newPresentation).isEqualTo(Presentation.REFRESH)
        assertThat(rule.newQueryStringPresentation).isEqualTo(QueryStringPresentation.DEFAULT)
        assertThat(rule.newNavigationMode).isEqualTo(NavigatorMode.DISMISS_MODAL)
        assertThat(rule.newModalResult).isNotNull()
        assertThat(rule.newModalResult?.location).isEqualTo(refreshUrl)
        assertThat(rule.newDestinationUri).isEqualTo(webUri)
        assertThat(rule.newDestination).isNotNull()
        assertThat(rule.newNavOptions).isEqualTo(navOptions)
    }

    @Test
    fun `resume historical location from modal context dismisses with result`() {
        controller.navigate(webDestinationId, locationArgs(featureUrl))
        controller.navigate(webModalDestinationId, locationArgs(newUrl))
        val rule = getNavigatorRule(resumeUrl)

        // Current destination
        assertThat(rule.previousLocation).isEqualTo(featureUrl)
        assertThat(rule.currentLocation).isEqualTo(newUrl)
        assertThat(rule.currentPresentationContext).isEqualTo(PresentationContext.MODAL)
        assertThat(rule.isAtStartDestination).isFalse()

        // New destination
        assertThat(rule.newLocation).isEqualTo(resumeUrl)
        assertThat(rule.newPresentationContext).isEqualTo(PresentationContext.DEFAULT)
        assertThat(rule.newPresentation).isEqualTo(Presentation.NONE)
        assertThat(rule.newQueryStringPresentation).isEqualTo(QueryStringPresentation.DEFAULT)
        assertThat(rule.newNavigationMode).isEqualTo(NavigatorMode.DISMISS_MODAL)
        assertThat(rule.newModalResult).isNotNull()
        assertThat(rule.newModalResult?.location).isEqualTo(resumeUrl)
        assertThat(rule.newDestinationUri).isEqualTo(webUri)
        assertThat(rule.newDestination).isNotNull()
        assertThat(rule.newNavOptions).isEqualTo(navOptions)
    }

    @Test
    fun `navigate to the same path with new query string`() {
        controller.navigate(webDestinationId, locationArgs(customUrl))
        val rule = getNavigatorRule(customQueryUrl)

        // Current destination
        assertThat(rule.previousLocation).isEqualTo(homeUrl)
        assertThat(rule.currentLocation).isEqualTo(customUrl)
        assertThat(rule.currentPresentationContext).isEqualTo(PresentationContext.DEFAULT)
        assertThat(rule.isAtStartDestination).isFalse()

        // New destination
        assertThat(rule.newLocation).isEqualTo(customQueryUrl)
        assertThat(rule.newPresentationContext).isEqualTo(PresentationContext.DEFAULT)
        assertThat(rule.newPresentation).isEqualTo(Presentation.PUSH)
        assertThat(rule.newQueryStringPresentation).isEqualTo(QueryStringPresentation.DEFAULT)
        assertThat(rule.newNavigationMode).isEqualTo(NavigatorMode.IN_CONTEXT)
        assertThat(rule.newModalResult).isNull()
        assertThat(rule.newDestinationUri).isEqualTo(webUri)
        assertThat(rule.newDestination).isNotNull()
        assertThat(rule.newNavOptions).isEqualTo(navOptions)
    }

    @Test
    fun `navigate to the same path with same query string`() {
        controller.navigate(webDestinationId, locationArgs(customQueryUrl))
        val rule = getNavigatorRule(customQueryUrl)

        // Current destination
        assertThat(rule.previousLocation).isEqualTo(homeUrl)
        assertThat(rule.currentLocation).isEqualTo(customQueryUrl)
        assertThat(rule.currentPresentationContext).isEqualTo(PresentationContext.DEFAULT)
        assertThat(rule.isAtStartDestination).isFalse()

        // New destination
        assertThat(rule.newLocation).isEqualTo(customQueryUrl)
        assertThat(rule.newPresentationContext).isEqualTo(PresentationContext.DEFAULT)
        assertThat(rule.newPresentation).isEqualTo(Presentation.REPLACE)
        assertThat(rule.newQueryStringPresentation).isEqualTo(QueryStringPresentation.DEFAULT)
        assertThat(rule.newNavigationMode).isEqualTo(NavigatorMode.IN_CONTEXT)
        assertThat(rule.newModalResult).isNull()
        assertThat(rule.newDestinationUri).isEqualTo(webUri)
        assertThat(rule.newDestination).isNotNull()
        assertThat(rule.newNavOptions).isEqualTo(navOptions)
    }

    @Test
    fun `navigate to the same path with filterable query string`() {
        controller.navigate(webDestinationId, locationArgs(featureUrl))
        val rule = getNavigatorRule(filterUrl)

        // Current destination
        assertThat(rule.previousLocation).isEqualTo(homeUrl)
        assertThat(rule.currentLocation).isEqualTo(featureUrl)
        assertThat(rule.currentPresentationContext).isEqualTo(PresentationContext.DEFAULT)
        assertThat(rule.isAtStartDestination).isFalse()

        // New destination
        assertThat(rule.newLocation).isEqualTo(filterUrl)
        assertThat(rule.newPresentationContext).isEqualTo(PresentationContext.DEFAULT)
        assertThat(rule.newPresentation).isEqualTo(Presentation.REPLACE)
        assertThat(rule.newQueryStringPresentation).isEqualTo(QueryStringPresentation.REPLACE)
        assertThat(rule.newNavigationMode).isEqualTo(NavigatorMode.IN_CONTEXT)
        assertThat(rule.newModalResult).isNull()
        assertThat(rule.newDestinationUri).isEqualTo(webUri)
        assertThat(rule.newDestination).isNotNull()
        assertThat(rule.newNavOptions).isEqualTo(navOptions)
    }

    @Test
    fun `prevent pop presentation from start destination`() {
        val rule = getNavigatorRule(recedeUrl)

        // Current destination
        assertThat(rule.previousLocation).isNull()
        assertThat(rule.currentLocation).isEqualTo(homeUrl)
        assertThat(rule.currentPresentationContext).isEqualTo(PresentationContext.DEFAULT)
        assertThat(rule.isAtStartDestination).isTrue()

        // New destination
        assertThat(rule.newLocation).isEqualTo(recedeUrl)
        assertThat(rule.newPresentationContext).isEqualTo(PresentationContext.DEFAULT)
        assertThat(rule.newPresentation).isEqualTo(Presentation.NONE)
        assertThat(rule.newQueryStringPresentation).isEqualTo(QueryStringPresentation.DEFAULT)
        assertThat(rule.newNavigationMode).isEqualTo(NavigatorMode.NONE)
        assertThat(rule.newModalResult).isNull()
        assertThat(rule.newDestinationUri).isEqualTo(webUri)
        assertThat(rule.newDestination).isNotNull()
        assertThat(rule.newNavOptions).isEqualTo(navOptions)
    }

    private fun getNavigatorRule(
        location: String,
        visitOptions: VisitOptions = VisitOptions(),
        bundle: Bundle? = null
    ): NavigatorRule {
        return NavigatorRule(
            location = location,
            visitOptions = visitOptions,
            bundle = bundle,
            navOptions = navOptions,
            extras = extras,
            pathConfiguration = pathConfiguration,
            navigatorName = navigatorName,
            controller = controller
        )
    }

    private fun locationArgs(location: String): Bundle {
        return bundleOf(ARG_LOCATION to location)
    }

    private fun buildControllerWithGraph(): TestNavHostController {
        return TestNavHostController(context).apply {
            graph = createGraph(startDestination = webHomeDestinationId) {
                destination(
                    NavDestinationBuilder(
                        navigator = provider.getNavigator<NavGraphNavigator>("test"),
                        id = webDestinationId
                    ).apply {
                        deepLink(webUri.toString())
                    }
                )

                destination(
                    NavDestinationBuilder(
                        navigator = provider.getNavigator<NavGraphNavigator>("test"),
                        id = webModalDestinationId
                    ).apply {
                        deepLink(webModalUri.toString())
                    }
                )

                destination(
                    NavDestinationBuilder(
                        navigator = provider.getNavigator<NavGraphNavigator>("test"),
                        id = webHomeDestinationId
                    ).apply {
                        argument(ARG_LOCATION) { defaultValue = homeUrl }
                        argument(ARG_NAVIGATOR_NAME) { defaultValue = navigatorName }
                        deepLink(webHomeUri.toString())
                    }
                )
            }
        }
    }
}
