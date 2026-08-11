package com.nuvio.tv.data.remote

import com.nuvio.tv.data.remote.api.StremioAccountApi
import com.nuvio.tv.data.remote.dto.StremioAddonCollectionRequest
import com.nuvio.tv.data.remote.dto.StremioAddonCollectionResultDto
import com.nuvio.tv.data.remote.dto.StremioAddonDescriptorDto
import com.nuvio.tv.data.remote.dto.StremioApiEnvelope
import com.nuvio.tv.data.remote.dto.StremioApiErrorDto
import com.nuvio.tv.data.remote.dto.StremioDatastoreGetRequest
import com.nuvio.tv.data.remote.dto.StremioDatastorePutRequest
import com.nuvio.tv.data.remote.dto.StremioLibraryItemDto
import com.nuvio.tv.data.remote.dto.StremioLoginRequest
import com.nuvio.tv.data.remote.dto.StremioLoginResultDto
import com.nuvio.tv.data.remote.dto.StremioLogoutRequest
import com.nuvio.tv.data.remote.dto.StremioLogoutResultDto
import com.nuvio.tv.data.remote.dto.StremioSuccessDto
import com.nuvio.tv.data.remote.dto.StremioUserDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StremioAccountClientTest {
    @Test
    fun `login returns redacted session and trims email`() =
        runTest {
            val api = FakeStremioAccountApi()
            val client = StremioAccountClient(api)

            val session = client.login("  viewer@example.com  ", "secret").getOrThrow()

            assertEquals("auth-key", session.authKey)
            assertEquals("viewer@example.com", session.email)
            assertFalse(session.toString().contains("auth-key"))
            assertFalse(api.loginRequest.toString().contains("secret"))
            assertEquals("viewer@example.com", api.loginRequest?.email)
        }

    @Test
    fun `login surfaces api error without creating a session`() =
        runTest {
            val api = FakeStremioAccountApi(loginError = "Invalid credentials")
            val result = StremioAccountClient(api).login("viewer@example.com", "wrong")

            assertTrue(result.isFailure)
            assertEquals("Invalid credentials", result.exceptionOrNull()?.message)
        }

    @Test
    fun `addon sync preserves order and ignores malformed transports`() =
        runTest {
            val api =
                FakeStremioAccountApi(
                    addonUrls =
                        listOf(
                            "https://first.example/manifest.json",
                            "not a URL",
                            " https://second.example/config/secret/manifest.json ",
                            "https://first.example/manifest.json",
                            "file:///tmp/manifest.json",
                        ),
                )
            val urls = StremioAccountClient(api).getAddonUrls("auth-key").getOrThrow()

            assertEquals(
                listOf(
                    "https://first.example/manifest.json",
                    "https://second.example/config/secret/manifest.json",
                ),
                urls,
            )
            assertFalse(api.collectionRequest.toString().contains("auth-key"))
        }

    @Test
    fun `addon sync fails when result is absent`() =
        runTest {
            val api = FakeStremioAccountApi(missingCollectionResult = true)
            val result = StremioAccountClient(api).getAddonUrls("auth-key")

            assertTrue(result.isFailure)
            assertEquals("Unable to sync Stremio addons", result.exceptionOrNull()?.message)
        }

    @Test
    fun `library datastore requests keep session and change details out of debug text`() =
        runTest {
            val item =
                StremioLibraryItemDto(
                    id = "tt1234567",
                    name = "Example",
                    type = "movie",
                    modifiedAt = "2026-08-11T12:00:00Z",
                )
            val api = FakeStremioAccountApi(libraryItems = listOf(item))
            val client = StremioAccountClient(api)

            assertEquals(listOf(item), client.getLibraryItems("auth-key").getOrThrow())
            client.putLibraryItems("auth-key", listOf(item)).getOrThrow()
            assertFalse(api.datastoreGetRequest.toString().contains("auth-key"))
            assertFalse(api.datastorePutRequest.toString().contains("auth-key"))
            assertFalse(api.datastorePutRequest.toString().contains("tt1234567"))
        }

    @Test
    fun `library write rejects an unsuccessful datastore response`() =
        runTest {
            val item =
                StremioLibraryItemDto(
                    id = "tt1234567",
                    name = "Example",
                    type = "movie",
                    modifiedAt = "2026-08-11T12:00:00Z",
                )
            val result = StremioAccountClient(FakeStremioAccountApi(putSuccess = false)).putLibraryItems("auth-key", listOf(item))

            assertTrue(result.isFailure)
            assertEquals("Stremio rejected the library update", result.exceptionOrNull()?.message)
        }

    @Test
    fun `logout revokes the supplied session without exposing it`() =
        runTest {
            val api = FakeStremioAccountApi()

            StremioAccountClient(api).logout("auth-key").getOrThrow()

            assertEquals("auth-key", api.logoutRequest?.authKey)
            assertFalse(api.logoutRequest.toString().contains("auth-key"))
        }
}

private class FakeStremioAccountApi(
    private val loginError: String? = null,
    private val addonUrls: List<String> = emptyList(),
    private val missingCollectionResult: Boolean = false,
    private val libraryItems: List<StremioLibraryItemDto> = emptyList(),
    private val putSuccess: Boolean = true,
) : StremioAccountApi {
    var loginRequest: StremioLoginRequest? = null
    var collectionRequest: StremioAddonCollectionRequest? = null
    var datastoreGetRequest: StremioDatastoreGetRequest? = null
    var datastorePutRequest: StremioDatastorePutRequest? = null
    var logoutRequest: StremioLogoutRequest? = null

    override suspend fun login(request: StremioLoginRequest): StremioApiEnvelope<StremioLoginResultDto> {
        loginRequest = request
        return if (loginError != null) {
            StremioApiEnvelope(error = StremioApiErrorDto(loginError))
        } else {
            StremioApiEnvelope(
                result =
                    StremioLoginResultDto(
                        authKey = "auth-key",
                        user = StremioUserDto(email = request.email),
                    ),
            )
        }
    }

    override suspend fun getAddonCollection(request: StremioAddonCollectionRequest): StremioApiEnvelope<StremioAddonCollectionResultDto> {
        collectionRequest = request
        if (missingCollectionResult) return StremioApiEnvelope()
        return StremioApiEnvelope(
            result =
                StremioAddonCollectionResultDto(
                    addons = addonUrls.map(::StremioAddonDescriptorDto),
                ),
        )
    }

    override suspend fun getLibraryItems(request: StremioDatastoreGetRequest): StremioApiEnvelope<List<StremioLibraryItemDto>> {
        datastoreGetRequest = request
        return StremioApiEnvelope(result = libraryItems)
    }

    override suspend fun putLibraryItems(request: StremioDatastorePutRequest): StremioApiEnvelope<StremioSuccessDto> {
        datastorePutRequest = request
        return StremioApiEnvelope(result = StremioSuccessDto(success = putSuccess))
    }

    override suspend fun logout(request: StremioLogoutRequest): StremioApiEnvelope<StremioLogoutResultDto> {
        logoutRequest = request
        return StremioApiEnvelope(result = StremioLogoutResultDto())
    }
}
