package dev.hotwire.navigation.destinations

import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

/**
 * Annotation for each Fragment that will be registered as a navigation destination.
 *
 * For example:
 *  `@HotwireDestinationDeepLink(uri = "turbo://fragment/search")`
 *  `class SearchFragment : TurboWebFragment()`
 *
 * @property uri The URI to be registered with the Android Navigation component nav graph.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class HotwireDestinationDeepLink(
    val uri: String
) {
    companion object {
        internal fun from(klass: KClass<out Any>): HotwireDestinationDeepLink {
            return requireNotNull(klass.findAnnotation()) {
                "A HotwireDestinationDeepLink annotation is required for the destination: ${klass.simpleName}"
            }
        }
    }
}
