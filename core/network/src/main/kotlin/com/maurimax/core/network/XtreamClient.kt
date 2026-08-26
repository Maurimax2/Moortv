package com.maurimax.core.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.Dns
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
     * Reaches a dual-stack panel over IPv4 first.
     *
     * The panel sits behind a CDN that answers on both families, and the two
     * do not always behave the same way: a phone browser reaching the v4 edge
     * got the API while this client, which takes the v6 address the resolver
     * lists first, got a 404 for the same URL with the same credentials.
     *
     * IPv6 is kept behind it rather than dropped, so a network that has no
     * IPv4 at all still connects.
     */
    private object Ipv4First : Dns {
        override fun lookup(hostname: String): List<java.net.InetAddress> {
            val all = Dns.SYSTEM.lookup(hostname)
            val (v4, v6) = all.partition { it is java.net.Inet4Address }
            return v4 + v6
        }
    }

    fun create(portalUrl: String): XtreamApi {
        val http = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            // A full catalogue can be tens of thousands of entries on a large
            // panel, so the read budget is generous.
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .dns(Ipv4First)
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
