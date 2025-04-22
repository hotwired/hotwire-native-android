package dev.hotwire.core.turbo.util

import android.content.Context
import android.content.res.Configuration
import android.os.Handler
import android.webkit.WebResourceRequest
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.ToNumberPolicy
import com.google.gson.reflect.TypeToken
import dev.hotwire.core.turbo.visit.VisitAction
import dev.hotwire.core.turbo.visit.VisitActionAdapter
import java.io.File

val Context.isNightModeEnabled: Boolean get() {
    val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return currentNightMode == Configuration.UI_MODE_NIGHT_YES
}

internal fun WebResourceRequest.isHttpGetRequest(): Boolean {
    return method.equals("GET", ignoreCase = true) &&
            url.scheme?.startsWith("HTTP", ignoreCase = true) == true
}

internal fun Context.runOnUiThread(func: () -> Unit) {
    when (mainLooper.isCurrentThread) {
        true -> func()
        else -> Handler(mainLooper).post { func() }
    }
}

internal fun Context.contentFromAsset(filePath: String): String {
    return assets.open(filePath).use {
        String(it.readBytes())
    }
}

internal fun String.extract(patternRegex: String): String? {
    val regex = Regex(patternRegex, RegexOption.IGNORE_CASE)
    return regex.find(this)?.groups?.get(1)?.value
}

internal fun String.truncateMiddle(maxChars: Int): String {
    if (maxChars <= 1 || length <= maxChars) { return this }

    return "${take(maxChars / 2)} [...] ${takeLast(maxChars / 2)}"
}

internal fun String.withoutNewLineChars(): String {
    return this.replace("\n", "")
}

internal fun String.withoutRepeatingWhitespace(): String {
    return this.replace(Regex("\\s+"), " ")
}

internal fun File.deleteAllFilesInDirectory() {
    if (!isDirectory) return

    listFiles()?.forEach {
        it.delete()
    }
}

internal fun Any.toJson(): String {
    return gson.toJson(this)
}

internal fun <T> String.toObject(typeToken: TypeToken<T>): T {
    return gson.fromJson(this, typeToken.type)
}

private val gson: Gson = GsonBuilder()
    .registerTypeAdapter(VisitAction::class.java, VisitActionAdapter())
    .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
    .create()
