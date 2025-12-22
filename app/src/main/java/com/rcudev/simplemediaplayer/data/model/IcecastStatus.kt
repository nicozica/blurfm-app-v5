package com.rcudev.simplemediaplayer.data.model

import com.google.gson.annotations.SerializedName
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.annotations.JsonAdapter

// Minimal model for Icecast status JSON

data class IcecastStatus(
    @SerializedName("icestats") val icestats: IceStats?
)

data class IceStats(
    @SerializedName("source")
    @JsonAdapter(IceSourceListAdapter::class)
    val source: List<IceSource>?
)

data class IceSource(
    @SerializedName("listenurl") val listenUrl: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("artist") val artist: String?,
    @SerializedName("server_name") val serverName: String?,
    @SerializedName("server_description") val serverDescription: String?,
    @SerializedName("stream_start_iso8601") val streamStart: String?,
    @SerializedName("genre") val genre: String?,
    @SerializedName("listeners") val listeners: Int?,
    @SerializedName("audio_info") val audioInfo: String?
)

/**
 * Allows Icecast "source" to be either a single object or an array.
 */
class IceSourceListAdapter : JsonDeserializer<List<IceSource>> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: java.lang.reflect.Type?,
        context: JsonDeserializationContext?
    ): List<IceSource>? {
        if (json == null || context == null) return null
        return when {
            json.isJsonArray -> json.asJsonArray.map { context.deserialize<IceSource>(it, IceSource::class.java) }
            json.isJsonObject -> listOf(context.deserialize<IceSource>(json, IceSource::class.java))
            else -> throw JsonParseException("Unexpected source format")
        }
    }
}
