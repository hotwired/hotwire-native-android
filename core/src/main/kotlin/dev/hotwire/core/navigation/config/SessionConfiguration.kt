package dev.hotwire.core.navigation.config

import androidx.annotation.IdRes

data class SessionConfiguration(
    val name: String,
    val startLocation: String,
    @IdRes val navHostFragmentId: Int,
)
