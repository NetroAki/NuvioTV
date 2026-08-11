package com.nuvio.tv.data.remote

import com.nuvio.tv.data.remote.api.StremioAccountApi
import com.nuvio.tv.data.remote.dto.StremioAddonCollectionRequest
import com.nuvio.tv.data.remote.dto.StremioApiEnvelope
import com.nuvio.tv.data.remote.dto.StremioDatastoreGetRequest
import com.nuvio.tv.data.remote.dto.StremioDatastorePutRequest
import com.nuvio.tv.data.remote.dto.StremioLibraryItemDto
import com.nuvio.tv.data.remote.dto.StremioLoginRequest
import com.nuvio.tv.data.remote.dto.StremioLogoutRequest
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StremioAccountClient
    @Inject
    constructor(
        private val api: StremioAccountApi,
    ) {
        suspend fun login(
            email: String,
            password: String,
        ): Result<StremioSession> =
            runCatching {
                val normalizedEmail = email.trim()
                require(normalizedEmail.isNotEmpty()) { "Email is required" }
                require(password.isNotEmpty()) { "Password is required" }

                val envelope = api.login(StremioLoginRequest(email = normalizedEmail, password = password))
                val result = envelope.resultOrThrow("Stremio sign-in failed")
                val authKey = result.authKey?.trim().orEmpty()
                if (authKey.isEmpty()) {
                    throw StremioAccountException("Stremio returned an invalid session")
                }
                StremioSession(
                    authKey = authKey,
                    email =
                        result.user
                            ?.email
                            ?.trim()
                            ?.takeIf(String::isNotEmpty) ?: normalizedEmail,
                )
            }

        suspend fun getAddonUrls(authKey: String): Result<List<String>> =
            runCatching {
                require(authKey.isNotBlank()) { "Stremio session is required" }
                api
                    .getAddonCollection(StremioAddonCollectionRequest(authKey = authKey))
                    .resultOrThrow("Unable to sync Stremio addons")
                    .addons
                    .mapNotNull { descriptor -> descriptor.transportUrl?.validTransportUrlOrNull() }
                    .distinct()
            }

        suspend fun getLibraryItems(authKey: String): Result<List<StremioLibraryItemDto>> =
            runCatching {
                require(authKey.isNotBlank()) { "Stremio session is required" }
                api
                    .getLibraryItems(StremioDatastoreGetRequest(authKey = authKey))
                    .resultOrThrow("Unable to sync the Stremio library")
            }

        suspend fun putLibraryItems(
            authKey: String,
            changes: List<StremioLibraryItemDto>,
        ): Result<Unit> =
            runCatching {
                require(authKey.isNotBlank()) { "Stremio session is required" }
                if (changes.isEmpty()) return@runCatching
                val response =
                    api
                        .putLibraryItems(
                            StremioDatastorePutRequest(
                                authKey = authKey,
                                changes = changes,
                            ),
                        ).resultOrThrow("Unable to update the Stremio library")
                if (!response.success) throw StremioAccountException("Stremio rejected the library update")
            }

        suspend fun logout(authKey: String): Result<Unit> =
            runCatching {
                require(authKey.isNotBlank()) { "Stremio session is required" }
                api.logout(StremioLogoutRequest(authKey = authKey)).resultOrThrow("Stremio sign-out failed")
            }.map { Unit }

        private fun String.validTransportUrlOrNull(): String? {
            val candidate = trim()
            if (candidate.isEmpty()) return null
            return runCatching {
                val uri = URI(candidate)
                require(uri.scheme.equals("http", ignoreCase = true) || uri.scheme.equals("https", ignoreCase = true))
                require(!uri.host.isNullOrBlank())
                candidate
            }.getOrNull()
        }
    }

class StremioSession(
    val authKey: String,
    val email: String,
) {
    override fun toString(): String = "StremioSession(email=$email, authKey=<redacted>)"
}

class StremioAccountException(
    message: String,
) : Exception(message)

private fun <T> StremioApiEnvelope<T>.resultOrThrow(fallbackMessage: String): T {
    error?.message?.trim()?.takeIf(String::isNotEmpty)?.let { message ->
        throw StremioAccountException(message)
    }
    return result ?: throw StremioAccountException(fallbackMessage)
}
