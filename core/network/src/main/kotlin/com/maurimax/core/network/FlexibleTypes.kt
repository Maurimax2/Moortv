package com.maurimax.core.network

import com.maurimax.core.network.dto.EpisodeDto
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Xtream Codes panels are wildly inconsistent about JSON types: the same field
 * comes back as `"123"` on one server and `123` on another, and `null` on a
 * third. Every string field in the DTOs goes through this so a panel quirk
 * cannot crash the app.
 */
object FlexibleString : KSerializer<String> {
    override val descriptor = PrimitiveSerialDescriptor("FlexibleString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        val json = decoder as? JsonDecoder ?: return decoder.decodeString()
        val element = json.decodeJsonElement()
        if (element is JsonNull) return ""
        return (element as? JsonPrimitive)?.content ?: ""
    }

    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value)
}

/** Same problem for numbers: `"1"`, `1`, `""` and `null` all appear in the wild. */
object FlexibleInt : KSerializer<Int> {
    override val descriptor = PrimitiveSerialDescriptor("FlexibleInt", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Int {
        val json = decoder as? JsonDecoder ?: return decoder.decodeInt()
        val element = json.decodeJsonElement()
        if (element is JsonNull) return 0
        return (element as? JsonPrimitive)?.content?.trim()?.toIntOrNull() ?: 0
    }

    override fun serialize(encoder: Encoder, value: Int) = encoder.encodeInt(value)
}

/**
 * `episodes` is the least consistent field on the whole API.
 *
 * Most panels send an object keyed by season number. Some send an array, where
 * the index is the season. An empty series can arrive as `[]`, as `{}` or as
 * `null`. And a single malformed episode inside an otherwise good series is
 * common enough that it must not cost the customer the whole show — so each one
 * is decoded on its own and a bad one is dropped rather than thrown.
 */
object EpisodesBySeason : KSerializer<Map<String, List<EpisodeDto>>> {

    private val delegate = MapSerializer(String.serializer(), ListSerializer(EpisodeDto.serializer()))
    override val descriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): Map<String, List<EpisodeDto>> {
        val json = decoder as? JsonDecoder ?: return emptyMap()

        return when (val element = json.decodeJsonElement()) {
            is JsonObject -> element.mapNotNull { (season, value) ->
                val episodes = episodesIn(json, value)
                if (episodes.isEmpty()) null else season to episodes
            }.toMap()

            is JsonArray -> element.mapIndexedNotNull { index, value ->
                val episodes = episodesIn(json, value)
                if (episodes.isEmpty()) null else index.toString() to episodes
            }.toMap()

            else -> emptyMap()
        }
    }

    private fun episodesIn(json: JsonDecoder, value: JsonElement): List<EpisodeDto> {
        val array = value as? JsonArray ?: return emptyList()
        return array.mapNotNull { entry ->
            runCatching { json.json.decodeFromJsonElement(EpisodeDto.serializer(), entry) }.getOrNull()
        }
    }

    override fun serialize(encoder: Encoder, value: Map<String, List<EpisodeDto>>) =
        delegate.serialize(encoder, value)
}
