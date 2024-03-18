package dev.hotwire.demo.util

import dev.hotwire.core.turbo.config.TurboPathConfigurationProperties

val TurboPathConfigurationProperties.description: String?
    get() = get("description")
