package dev.hotwire.demo.main

import dev.hotwire.demo.R
import dev.hotwire.demo.Urls
import dev.hotwire.navigation.navigator.NavigatorConfiguration
import dev.hotwire.navigation.tabs.HotwireBottomTab

val mainTabs = listOf(
    HotwireBottomTab(
        itemId = R.id.bottom_nav_navigation,
        configuration = NavigatorConfiguration(
            name = "navigation",
            navigatorHostId = R.id.navigation_navigator_host,
            startLocation = Urls.navigationUrl
        )
    ),
    HotwireBottomTab(
        itemId = R.id.bottom_nav_bridge_components,
        configuration = NavigatorConfiguration(
            name = "navigation",
            navigatorHostId = R.id.bridge_components_navigator_host,
            startLocation = Urls.bridgeComponentsUrl
        )
    ),
    HotwireBottomTab(
        itemId = R.id.bottom_nav_resources,
        configuration = NavigatorConfiguration(
            name = "navigation",
            navigatorHostId = R.id.resources_navigator_host,
            startLocation = Urls.resourcesUrl
        )
    )
)