package dev.hotwire.core.turbo.visit

import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import dev.hotwire.core.turbo.util.toObject

data class VisitOptions(
    @SerializedName("action") val action: VisitAction = VisitAction.ADVANCE,
    @SerializedName("snapshotHTML") val snapshotHTML: String? = null,
    @SerializedName("response") val response: VisitResponse? = null
) {
    companion object {
        fun fromJSON(json: String?): VisitOptions? = try {
            json?.toObject(object : TypeToken<VisitOptions>() {})
        } catch (e: Exception) {
            null
        }
    }
}
