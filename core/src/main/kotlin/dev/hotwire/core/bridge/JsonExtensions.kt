package dev.hotwire.core.bridge

import dev.hotwire.core.logging.logError
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

internal fun String.parseToJsonElement() = json.parseToJsonElement(this)

internal inline fun <reified T> T.toJsonElement() = json.encodeToJsonElement(this)

internal inline fun <reified T> T.toJson() = json.encodeToString(this)

internal inline fun <reified T> JsonElement.decode(): T? = try {
    json.decodeFromJsonElement<T>(this)
} catch (e: Exception) {
    logError("jsonElementDecodeException", e)
    null
}

internal inline fun <reified T> String.decode(): T? = try {
    json.decodeFromString<T>(this)
} catch (e: Exception) {
    logError("jsonStringDecodeException", e)
    null
}

internal val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}
