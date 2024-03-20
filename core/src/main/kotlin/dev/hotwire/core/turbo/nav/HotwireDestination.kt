package dev.hotwire.core.turbo.nav

import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

/**
 * Annotation for each Fragment that will be registered as a navigation destination.
 *
 * For example:
 *  `@HotwireDestination(uri = "turbo://fragment/search")`
 *  `class SearchFragment : TurboWebFragment()`
 *
 * @property uri The URI to be registered with the Android Navigation component nav graph.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class HotwireDestination(
    val uri: String
) {
    companion object {
        internal fun from(klass: KClass<out Any>): HotwireDestination {
            return requireNotNull(klass.findAnnotation()) {
                "A HotwireDestination annotation is required for the destination: ${klass.simpleName}"
            }
        }
    }
}
