package dev.hotwire.core.navigation.navigator

import androidx.annotation.IdRes

data class NavigatorConfiguration(
    val name: String,
    val startLocation: String,
    @IdRes val navigatorHostId: Int,
)
