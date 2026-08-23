package com.maurimax.core.network.dto

import com.maurimax.core.network.FlexibleInt
import com.maurimax.core.network.FlexibleString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlayerApiResponse(
    @SerialName("user_info") val userInfo: UserInfoDto? = null,
    @SerialName("server_info") val serverInfo: ServerInfoDto? = null,
)

@Serializable
data class UserInfoDto(
    @Serializable(with = FlexibleString::class) val username: String = "",
    @Serializable(with = FlexibleString::class) val status: String = "",
    /** 1 when the credentials are valid. Panels send it as int or string. */
    @Serializable(with = FlexibleInt::class) val auth: Int = 0,
    @SerialName("exp_date") @Serializable(with = FlexibleString::class) val expiryEpoch: String = "",
    @SerialName("is_trial") @Serializable(with = FlexibleString::class) val isTrial: String = "",
    @SerialName("active_cons") @Serializable(with = FlexibleString::class) val activeConnections: String = "",
    @SerialName("max_connections") @Serializable(with = FlexibleString::class) val maxConnections: String = "",
)

@Serializable
data class ServerInfoDto(
    @Serializable(with = FlexibleString::class) val url: String = "",
    @Serializable(with = FlexibleString::class) val port: String = "",
    @SerialName("https_port") @Serializable(with = FlexibleString::class) val httpsPort: String = "",
    @SerialName("server_protocol") @Serializable(with = FlexibleString::class) val protocol: String = "",
)

@Serializable
data class CategoryDto(
    @SerialName("category_id") @Serializable(with = FlexibleString::class) val id: String = "",
    @SerialName("category_name") @Serializable(with = FlexibleString::class) val name: String = "",
)

@Serializable
data class LiveStreamDto(
    @SerialName("stream_id") @Serializable(with = FlexibleInt::class) val streamId: Int = 0,
    @Serializable(with = FlexibleString::class) val name: String = "",
    @SerialName("stream_icon") @Serializable(with = FlexibleString::class) val icon: String = "",
    @SerialName("epg_channel_id") @Serializable(with = FlexibleString::class) val epgChannelId: String = "",
    @SerialName("category_id") @Serializable(with = FlexibleString::class) val categoryId: String = "",
    @SerialName("tv_archive") @Serializable(with = FlexibleInt::class) val hasArchive: Int = 0,
    @Serializable(with = FlexibleInt::class) val num: Int = 0,
)

@Serializable
data class VodStreamDto(
    @SerialName("stream_id") @Serializable(with = FlexibleInt::class) val streamId: Int = 0,
    @Serializable(with = FlexibleString::class) val name: String = "",
    @SerialName("stream_icon") @Serializable(with = FlexibleString::class) val icon: String = "",
    @SerialName("category_id") @Serializable(with = FlexibleString::class) val categoryId: String = "",
    @SerialName("container_extension") @Serializable(with = FlexibleString::class) val containerExtension: String = "",
    @Serializable(with = FlexibleString::class) val rating: String = "",
)

@Serializable
data class SeriesDto(
    @SerialName("series_id") @Serializable(with = FlexibleInt::class) val seriesId: Int = 0,
    @Serializable(with = FlexibleString::class) val name: String = "",
    @Serializable(with = FlexibleString::class) val cover: String = "",
    @SerialName("category_id") @Serializable(with = FlexibleString::class) val categoryId: String = "",
    @Serializable(with = FlexibleString::class) val plot: String = "",
    @Serializable(with = FlexibleString::class) val rating: String = "",
)
