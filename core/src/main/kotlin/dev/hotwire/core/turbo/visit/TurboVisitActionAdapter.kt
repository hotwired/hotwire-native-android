package dev.hotwire.core.turbo.visit

import android.annotation.SuppressLint
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonWriter

@SuppressLint("DefaultLocale")
internal class TurboVisitActionAdapter : TypeAdapter<TurboVisitAction>() {
    override fun read(reader: com.google.gson.stream.JsonReader): TurboVisitAction {
        return try {
            TurboVisitAction.valueOf(reader.nextString().uppercase())
        } catch (e: IllegalArgumentException) {
            TurboVisitAction.ADVANCE
        }
    }

    override fun write(writer: JsonWriter, action: TurboVisitAction) {
        writer.value(action.name.lowercase())
    }
}
