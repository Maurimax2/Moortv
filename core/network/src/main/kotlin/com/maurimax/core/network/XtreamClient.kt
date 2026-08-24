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

    fun create(portalUrl: String): XtreamApi {
        val http = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            // A full catalogue can be tens of thousands of entries on a large
            // panel, so the read budget is generous.
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            // No custom User-Agent. Setting one made this panel answer with
            // something that is not the API, which broke a working catalogue —
            // the default agent is what it accepts.
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
