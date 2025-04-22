package dev.hotwire.demo.main

import dev.hotwire.demo.Demo
import dev.hotwire.demo.R
import dev.hotwire.navigation.navigator.NavigatorConfiguration
import dev.hotwire.navigation.tabs.HotwireBottomTab

private val navigation = HotwireBottomTab(
    title = "Navigation",
    iconResId = R.drawable.ic_tab_navigation,
    configuration = NavigatorConfiguration(
        name = "navigation",
        navigatorHostId = R.id.navigation_navigator_host,
        startLocation = Demo.current.url
    )
)

private val bridgeComponents = HotwireBottomTab(
    title = "Bridge Components",
    iconResId = R.drawable.ic_tab_bridge_components,
    configuration = NavigatorConfiguration(
        name = "bridge-components",
        navigatorHostId = R.id.bridge_components_navigator_host,
        startLocation = "${Demo.current.url}/components"
    )
)

private val resources = HotwireBottomTab(
    title = "Resources",
    iconResId = R.drawable.ic_tab_resources,
    configuration = NavigatorConfiguration(
        name = "resources",
        navigatorHostId = R.id.resources_navigator_host,
        startLocation = "${Demo.current.url}/resources"
    )
)

private val bugsAndFixes = HotwireBottomTab(
    title = "Bugs & Fixes",
    iconResId = R.drawable.ic_tab_bugs_fixes,
    isVisible = Demo.current == Demo.Environment.Local,
    configuration = NavigatorConfiguration(
        name = "bugs-fixes",
        navigatorHostId = R.id.bugs_fixes_navigator_host,
        startLocation = "${Demo.current.url}/bugs"
    )
)

val mainTabs = listOf(
    navigation,
    bridgeComponents,
    resources,
    bugsAndFixes
)
