package dev.hotwire.core.turbo.config

import dev.hotwire.core.turbo.nav.Presentation
import dev.hotwire.core.turbo.nav.PresentationContext

internal val recedeHistoricalLocationRule = PathConfigurationRule(
    patterns = listOf("/recede_historical_location"),
    properties = hashMapOf(
        "presentation" to Presentation.POP.name,
        "context" to PresentationContext.DEFAULT.name,
        "historical_location" to true
    )
)

internal val resumeHistoricalLocationRule = PathConfigurationRule(
    patterns = listOf("/resume_historical_location"),
    properties = hashMapOf(
        "presentation" to Presentation.NONE.name,
        "context" to PresentationContext.DEFAULT.name,
        "historical_location" to true
    )
)

internal val refreshHistoricalLocationRule = PathConfigurationRule(
    patterns = listOf("/refresh_historical_location"),
    properties = hashMapOf(
        "presentation" to Presentation.REFRESH.name,
        "context" to PresentationContext.DEFAULT.name,
        "historical_location" to true
    )
)

internal val historicalLocationRules = listOf(
    recedeHistoricalLocationRule,
    resumeHistoricalLocationRule,
    refreshHistoricalLocationRule
)
