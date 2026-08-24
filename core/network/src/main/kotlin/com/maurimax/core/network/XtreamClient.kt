package com.maurimax.core.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/** Builds the API for one portal. The portal URL is fixed at build time. */
object XtreamClient {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    /**
     * Panels commonly sit behind filtering that rejects an unfamiliar client, and
     * OkHttp's default agent is a frequent casualty — the request never reaches
     * the API and the app looks like it has no catalogue. Presenting a player-like
     * agent is what every IPTV client does for the same reason.
     */
    private const val USER_AGENT = "MAURIMAX/1.0 (Android) ExoPlayer"

    fun create(portalUrl: String): XtreamApi {
        val http = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            // A full catalogue can be tens of thousands of entries on a large
            // panel, so the read budget is generous.
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", USER_AGENT)
                        .header("Accept", "application/json, text/plain, */*")
                        .build(),
                )
            }
            .build()

        return Retrofit.Builder()
            // Retrofit demands a trailing slash on the base URL.
            .baseUrl(portalUrl.trimEnd('/') + "/")
            .client(http)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(XtreamApi::class.java)
    }
}
