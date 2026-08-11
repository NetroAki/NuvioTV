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
data class StremioDatastoreGetRequest(
    @param:Json(name = "authKey") val authKey: String,
    @param:Json(name = "collection") val collection: String = "libraryItem",
    @param:Json(name = "ids") val ids: List<String> = emptyList(),
    @param:Json(name = "all") val all: Boolean = true,
) {
    override fun toString(): String = "StremioDatastoreGetRequest(authKey=<redacted>, collection=$collection, ids=${ids.size}, all=$all)"
}

@JsonClass(generateAdapter = true)
data class StremioDatastorePutRequest(
    @param:Json(name = "authKey") val authKey: String,
    @param:Json(name = "collection") val collection: String = "libraryItem",
    @param:Json(name = "changes") val changes: List<StremioLibraryItemDto>,
) {
    override fun toString(): String = "StremioDatastorePutRequest(authKey=<redacted>, collection=$collection, changes=${changes.size})"
}

@JsonClass(generateAdapter = true)
data class StremioLibraryItemDto(
    @param:Json(name = "_id") val id: String,
    @param:Json(name = "name") val name: String,
    @param:Json(name = "type") val type: String,
    @param:Json(name = "poster") val poster: String? = null,
    @param:Json(name = "posterShape") val posterShape: String = "poster",
    @param:Json(name = "removed") val removed: Boolean = false,
    @param:Json(name = "temp") val temp: Boolean = false,
    @param:Json(name = "_ctime") val createdAt: String? = null,
    @param:Json(name = "_mtime") val modifiedAt: String,
    @param:Json(name = "state") val state: StremioLibraryItemStateDto = StremioLibraryItemStateDto(),
    @param:Json(name = "behaviorHints") val behaviorHints: Map<String, Any?> = emptyMap(),
)

@JsonClass(generateAdapter = true)
data class StremioLibraryItemStateDto(
    @param:Json(name = "lastWatched") val lastWatched: String? = null,
    @param:Json(name = "timeWatched") val timeWatched: Long = 0,
    @param:Json(name = "timeOffset") val timeOffset: Long = 0,
    @param:Json(name = "overallTimeWatched") val overallTimeWatched: Long = 0,
    @param:Json(name = "timesWatched") val timesWatched: Int = 0,
    @param:Json(name = "flaggedWatched") val flaggedWatched: Int = 0,
    @param:Json(name = "duration") val duration: Long = 0,
    @param:Json(name = "video_id") val videoId: String? = null,
    @param:Json(name = "watched") val watched: String? = null,
    @param:Json(name = "noNotif") val noNotifications: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class StremioSuccessDto(
    @param:Json(name = "success") val success: Boolean = false,
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
