package com.maurimax.core.network.dto

import com.maurimax.core.network.EpisodesBySeason
import com.maurimax.core.network.FlexibleInt
import com.maurimax.core.network.FlexibleString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** What `get_series_info` answers with for one series. */
@Serializable
data class SeriesInfoResponse(
    val info: SeriesDetailDto? = null,
    /** Keyed by season number, as a string, because that is how panels send it. */
    @Serializable(with = EpisodesBySeason::class)
    val episodes: Map<String, List<EpisodeDto>> = emptyMap(),
)

@Serializable
data class SeriesDetailDto(
    @Serializable(with = FlexibleString::class) val name: String = "",
    @Serializable(with = FlexibleString::class) val cover: String = "",
    @Serializable(with = FlexibleString::class) val plot: String = "",
    @Serializable(with = FlexibleString::class) val rating: String = "",
)

@Serializable
data class EpisodeDto(
    /** The episode's own stream id — not the series id, which will not play. */
    @Serializable(with = FlexibleString::class) val id: String = "",
    @SerialName("episode_num") @Serializable(with = FlexibleInt::class) val episodeNum: Int = 0,
    @Serializable(with = FlexibleString::class) val title: String = "",
    @SerialName("container_extension") @Serializable(with = FlexibleString::class)
    val containerExtension: String = "",
    @Serializable(with = FlexibleInt::class) val season: Int = 0,
    val info: EpisodeInfoDto? = null,
)

@Serializable
data class EpisodeInfoDto(
    @SerialName("movie_image") @Serializable(with = FlexibleString::class) val image: String = "",
    @Serializable(with = FlexibleString::class) val plot: String = "",
    @SerialName("duration_secs") @Serializable(with = FlexibleInt::class) val durationSeconds: Int = 0,
)
