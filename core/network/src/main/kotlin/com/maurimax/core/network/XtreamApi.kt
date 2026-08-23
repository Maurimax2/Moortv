package com.maurimax.core.network

import com.maurimax.core.network.dto.CategoryDto
import com.maurimax.core.network.dto.LiveStreamDto
import com.maurimax.core.network.dto.PlayerApiResponse
import com.maurimax.core.network.dto.SeriesDto
import com.maurimax.core.network.dto.VodStreamDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * The Xtream Codes player API. Every call carries the customer's credentials —
 * the panel has no session or token concept.
 */
interface XtreamApi {

    /** No action: the panel answers with account status, which is the login check. */
    @GET("player_api.php")
    suspend fun login(
        @Query("username") username: String,
        @Query("password") password: String,
    ): PlayerApiResponse

    @GET("player_api.php?action=get_live_categories")
    suspend fun liveCategories(
        @Query("username") username: String,
        @Query("password") password: String,
    ): List<CategoryDto>

    @GET("player_api.php?action=get_live_streams")
    suspend fun liveStreams(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("category_id") categoryId: String? = null,
    ): List<LiveStreamDto>

    @GET("player_api.php?action=get_vod_categories")
    suspend fun vodCategories(
        @Query("username") username: String,
        @Query("password") password: String,
    ): List<CategoryDto>

    @GET("player_api.php?action=get_vod_streams")
    suspend fun vodStreams(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("category_id") categoryId: String? = null,
    ): List<VodStreamDto>

    @GET("player_api.php?action=get_series_categories")
    suspend fun seriesCategories(
        @Query("username") username: String,
        @Query("password") password: String,
    ): List<CategoryDto>

    @GET("player_api.php?action=get_series")
    suspend fun series(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("category_id") categoryId: String? = null,
    ): List<SeriesDto>
}
