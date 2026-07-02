package dev.hotwire.core.turbo.visit

import android.os.Bundle
import dev.hotwire.core.turbo.config.PathConfigurationProperties

data class VisitProposal(
    val location: String,
    val options: VisitOptions,
    val properties: PathConfigurationProperties,
    val bundle: Bundle?
)
