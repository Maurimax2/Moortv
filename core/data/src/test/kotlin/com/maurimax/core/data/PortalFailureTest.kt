package com.maurimax.core.data

import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class PortalFailureTest {

    private fun http(code: Int) =
        HttpException(Response.error<Any>(code, "".toResponseBody("text/plain".toMediaType())))

    @Test
    fun `a rejected login is credentials, not a connection problem`() {
        // The bug this pins: every exception used to become "check your internet",
        // which sends someone with a wrong password looking in the wrong place.
        assertEquals(PortalFailure.BadCredentials, http(401).toPortalFailure())
        assertEquals(PortalFailure.BadCredentials, http(403).toPortalFailure())
    }

    @Test
    fun `a broken panel is reported as a server problem`() {
        assertTrue(http(500).toPortalFailure() is PortalFailure.ServerError)
        assertTrue(http(503).toPortalFailure() is PortalFailure.ServerError)
        assertEquals(502, (http(502).toPortalFailure() as PortalFailure.ServerError).code)
    }

    @Test
    fun `no network is a connection problem`() {
        assertEquals(PortalFailure.NoConnection, IOException("closed").toPortalFailure())
        assertEquals(PortalFailure.NoConnection, UnknownHostException("dns").toPortalFailure())
        assertEquals(PortalFailure.NoConnection, SocketTimeoutException("slow").toPortalFailure())
    }

    @Test
    fun `an error page instead of the api is its own case`() {
        assertEquals(
            PortalFailure.UnexpectedResponse,
            SerializationException("Unexpected JSON token").toPortalFailure(),
        )
        assertEquals(PortalFailure.UnexpectedResponse, http(418).toPortalFailure())
    }
}
