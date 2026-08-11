package com.nuvio.tv.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StremioApiEnvelope<T>(
    @param:Json(name = "result") val result: T? = null,
    @param:Json(name = "error") val error: StremioApiErrorDto? = null,
)

@JsonClass(generateAdapter = true)
data class StremioApiErrorDto(
    @param:Json(name = "message") val message: String? = null,
)

@JsonClass(generateAdapter = true)
data class StremioLoginRequest(
    @param:Json(name = "type") val type: String = "Auth",
    @param:Json(name = "email") val email: String,
    @param:Json(name = "password") val password: String,
) {
    override fun toString(): String = "StremioLoginRequest(email=$email, password=<redacted>)"
}

@JsonClass(generateAdapter = true)
data class StremioLoginResultDto(
    @param:Json(name = "authKey") val authKey: String? = null,
    @param:Json(name = "user") val user: StremioUserDto? = null,
)

@JsonClass(generateAdapter = true)
data class StremioUserDto(
    @param:Json(name = "_id") val id: String? = null,
    @param:Json(name = "email") val email: String? = null,
)

@JsonClass(generateAdapter = true)
data class StremioAddonCollectionRequest(
    @param:Json(name = "type") val type: String = "AddonCollectionGet",
    @param:Json(name = "authKey") val authKey: String,
    @param:Json(name = "update") val update: Boolean = true,
) {
    override fun toString(): String = "StremioAddonCollectionRequest(authKey=<redacted>, update=$update)"
}

@JsonClass(generateAdapter = true)
data class StremioAddonCollectionResultDto(
    @param:Json(name = "addons") val addons: List<StremioAddonDescriptorDto> = emptyList(),
    @param:Json(name = "lastModified") val lastModified: String? = null,
)

@JsonClass(generateAdapter = true)
data class StremioAddonDescriptorDto(
    @param:Json(name = "transportUrl") val transportUrl: String? = null,
)

@JsonClass(generateAdapter = true)
data class StremioLogoutRequest(
    @param:Json(name = "type") val type: String = "Logout",
    @param:Json(name = "authKey") val authKey: String,
) {
    override fun toString(): String = "StremioLogoutRequest(authKey=<redacted>)"
}

@JsonClass(generateAdapter = true)
class StremioLogoutResultDto
