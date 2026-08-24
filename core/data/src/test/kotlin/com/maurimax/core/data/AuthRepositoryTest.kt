package com.maurimax.core.data

import com.maurimax.core.model.Credentials
import com.maurimax.core.network.XtreamApi
import com.maurimax.core.network.dto.CategoryDto
import com.maurimax.core.network.dto.LiveStreamDto
import com.maurimax.core.network.dto.PlayerApiResponse
import com.maurimax.core.network.dto.SeriesDto
import com.maurimax.core.network.dto.UserInfoDto
import com.maurimax.core.network.dto.VodStreamDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class AuthRepositoryTest {

    private class FakeApi(
        private val response: (() -> PlayerApiResponse)? = null,
    ) : XtreamApi {
        override suspend fun login(username: String, password: String): PlayerApiResponse =
            response?.invoke() ?: throw IOException("no response configured")

        override suspend fun liveCategories(username: String, password: String) = emptyList<CategoryDto>()
        override suspend fun liveStreams(username: String, password: String, categoryId: String?) = emptyList<LiveStreamDto>()
        override suspend fun vodCategories(username: String, password: String) = emptyList<CategoryDto>()
        override suspend fun vodStreams(username: String, password: String, categoryId: String?) = emptyList<VodStreamDto>()
        override suspend fun seriesCategories(username: String, password: String) = emptyList<CategoryDto>()
        override suspend fun series(username: String, password: String, categoryId: String?) = emptyList<SeriesDto>()
    }

    private fun repo(api: XtreamApi, store: CredentialStore = InMemoryCredentialStore()) =
        AuthRepository(api, store) to store

    @Test
    fun `a valid active account signs in`() = runTest {
        val api = FakeApi {
            PlayerApiResponse(UserInfoDto(username = "bob", status = "Active", auth = 1))
        }
        val (auth, _) = repo(api)

        val result = auth.signIn("bob", "hunter2")

        assertTrue(result is LoginResult.Success)
        assertEquals("bob", (result as LoginResult.Success).account.username)
    }

    @Test
    fun `credentials are remembered on success`() = runTest {
        val api = FakeApi { PlayerApiResponse(UserInfoDto(username = "bob", status = "Active", auth = 1)) }
        val (auth, store) = repo(api)

        auth.signIn("bob", "hunter2")

        assertEquals(Credentials("bob", "hunter2"), store.load())
    }

    @Test
    fun `auth zero is rejected and nothing is stored`() = runTest {
        val api = FakeApi { PlayerApiResponse(UserInfoDto(auth = 0)) }
        val (auth, store) = repo(api)

        val result = auth.signIn("bob", "wrong")

        assertEquals(PortalFailure.BadCredentials, (result as LoginResult.Failure).reason)
        assertNull(store.load())
    }

    @Test
    fun `an expired account is reported separately from bad credentials`() = runTest {
        val api = FakeApi { PlayerApiResponse(UserInfoDto(username = "bob", status = "Expired", auth = 1)) }
        val (auth, store) = repo(api)

        val result = auth.signIn("bob", "hunter2")

        assertEquals(PortalFailure.Inactive("Expired"), (result as LoginResult.Failure).reason)
        assertNull(store.load())
    }

    @Test
    fun `a network failure is not reported as bad credentials`() = runTest {
        val (auth, _) = repo(FakeApi())

        val result = auth.signIn("bob", "hunter2")

        assertTrue((result as LoginResult.Failure).reason is PortalFailure.NoConnection)
    }

    @Test
    fun `signing out clears stored credentials`() = runTest {
        val store = InMemoryCredentialStore(Credentials("bob", "hunter2"))
        val auth = AuthRepository(FakeApi(), store)

        auth.signOut()

        assertNull(store.load())
    }
}
