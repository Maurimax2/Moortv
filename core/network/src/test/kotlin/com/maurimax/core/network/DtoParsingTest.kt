package com.maurimax.core.network

import com.maurimax.core.network.dto.LiveStreamDto
import com.maurimax.core.network.dto.PlayerApiResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Panels disagree about JSON types for the same fields. These cases are all
 * shapes real Xtream servers return.
 */
class DtoParsingTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Test
    fun `auth as an int parses`() {
        val body = """{"user_info":{"username":"bob","auth":1,"status":"Active"}}"""
        val parsed = json.decodeFromString<PlayerApiResponse>(body)
        assertEquals(1, parsed.userInfo?.auth)
        assertEquals("Active", parsed.userInfo?.status)
    }

    @Test
    fun `auth as a string parses to the same value`() {
        val body = """{"user_info":{"username":"bob","auth":"1","status":"Active"}}"""
        assertEquals(1, json.decodeFromString<PlayerApiResponse>(body).userInfo?.auth)
    }

    @Test
    fun `a numeric expiry date survives being typed as a string`() {
        val body = """{"user_info":{"exp_date":1790000000,"auth":1}}"""
        assertEquals("1790000000", json.decodeFromString<PlayerApiResponse>(body).userInfo?.expiryEpoch)
    }

    @Test
    fun `null fields fall back to empty rather than throwing`() {
        val body = """{"user_info":{"exp_date":null,"auth":1,"status":null}}"""
        val info = json.decodeFromString<PlayerApiResponse>(body).userInfo
        assertEquals("", info?.expiryEpoch)
        assertEquals("", info?.status)
    }

    @Test
    fun `stream ids parse whether quoted or not`() {
        val quoted = json.decodeFromString<LiveStreamDto>("""{"stream_id":"42","name":"BBC One"}""")
        val bare = json.decodeFromString<LiveStreamDto>("""{"stream_id":42,"name":"BBC One"}""")
        assertEquals(42, quoted.streamId)
        assertEquals(42, bare.streamId)
    }

    @Test
    fun `unknown fields on a stream are ignored`() {
        val body = """{"stream_id":9,"name":"Sky","some_new_panel_field":{"a":1}}"""
        assertEquals("Sky", json.decodeFromString<LiveStreamDto>(body).name)
    }
}
