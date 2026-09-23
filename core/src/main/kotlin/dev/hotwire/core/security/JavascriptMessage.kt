package dev.hotwire.core.security

import dev.hotwire.core.bridge.decode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive

/**
 * A `{name, args}` envelope posted by the library's bundled JavaScript
 * through a [JavascriptChannel].
 */
@Serializable
internal data class JavascriptMessage(
    val name: String,
    val args: JsonArray = JsonArray(emptyList())
)

internal fun String.toJavascriptMessageOrNull(): JavascriptMessage? = decode<JavascriptMessage>()

internal fun JsonArray.stringAt(index: Int): String = this[index].jsonPrimitive.content
internal fun JsonArray.booleanAt(index: Int): Boolean = this[index].jsonPrimitive.boolean
internal fun JsonArray.intAt(index: Int): Int = this[index].jsonPrimitive.int
