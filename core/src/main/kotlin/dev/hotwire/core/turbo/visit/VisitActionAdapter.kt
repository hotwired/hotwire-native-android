package dev.hotwire.core.turbo.visit

import android.annotation.SuppressLint
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

@SuppressLint("DefaultLocale")
internal class VisitActionAdapter : TypeAdapter<VisitAction>() {
    override fun read(reader: JsonReader): VisitAction {
        return try {
            VisitAction.valueOf(reader.nextString().uppercase())
        } catch (e: IllegalArgumentException) {
            VisitAction.ADVANCE
        }
    }

    override fun write(writer: JsonWriter, action: VisitAction) {
        writer.value(action.name.lowercase())
    }
}
