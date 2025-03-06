package dev.hotwire.navigation.destinations

import androidx.navigation.NavOptions
import androidx.navigation.navOptions
import dev.hotwire.core.turbo.config.PathConfigurationProperties
import dev.hotwire.core.turbo.config.animated
import dev.hotwire.core.turbo.config.context
import dev.hotwire.core.turbo.config.presentation
import dev.hotwire.core.turbo.nav.Presentation
import dev.hotwire.core.turbo.nav.PresentationContext
import dev.hotwire.core.turbo.visit.VisitAction
import dev.hotwire.navigation.R

class HotwireDestinationAnimations {
    companion object {
        fun defaultNavOptions(
            currentPathProperties: PathConfigurationProperties,
            newPathProperties: PathConfigurationProperties,
            action: VisitAction
        ): NavOptions {
            val navigatingToModalContext = currentPathProperties.context == PresentationContext.DEFAULT &&
                    newPathProperties.context == PresentationContext.MODAL

            val navigatingWithinModalContext = currentPathProperties.context == PresentationContext.MODAL &&
                    newPathProperties.context == PresentationContext.MODAL

            val dismissingModalContext = currentPathProperties.context == PresentationContext.MODAL &&
                    newPathProperties.context == PresentationContext.DEFAULT

            val animate = shouldAnimate(
                navigatingToModalContext = navigatingToModalContext,
                dismissingModalContext = dismissingModalContext,
                newPathProperties = newPathProperties,
                action = action
            )

            val clearAll = newPathProperties.presentation == Presentation.CLEAR_ALL

            return if (navigatingToModalContext || navigatingWithinModalContext || dismissingModalContext) {
                navOptions {
                    anim {
                        enter = if (animate) R.anim.enter_slide_in_bottom else 0
                        exit = R.anim.exit_slide_out_bottom
                        popEnter = R.anim.enter_slide_in_bottom
                        popExit = R.anim.exit_slide_out_bottom
                    }
                }
            } else {
                if (clearAll) {
                    navOptions {
                        anim {
                            enter = R.anim.exit_slide_out_left
                            exit = R.anim.exit_slide_out_right
                            popEnter = R.anim.enter_slide_in_left
                            popExit = R.anim.enter_slide_in_right
                        }
                    }
                } else {
                    navOptions {
                        anim {
                            enter = if (animate) R.anim.enter_slide_in_right else 0
                            exit = R.anim.exit_slide_out_left
                            popEnter = R.anim.enter_slide_in_left
                            popExit = R.anim.exit_slide_out_right
                        }
                    }
                }
            }
        }

        private fun shouldAnimate(
            navigatingToModalContext: Boolean,
            dismissingModalContext: Boolean,
            newPathProperties: PathConfigurationProperties,
            action: VisitAction
        ): Boolean {
            if (!newPathProperties.animated) {
                return false
            }

            if (navigatingToModalContext || dismissingModalContext) {
                return true
            }

            return action != VisitAction.REPLACE &&
                    newPathProperties.presentation != Presentation.REPLACE &&
                    newPathProperties.presentation != Presentation.REPLACE_ROOT
        }
    }
}
