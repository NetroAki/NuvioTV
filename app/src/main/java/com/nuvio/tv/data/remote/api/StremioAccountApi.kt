package com.nuvio.tv.data.remote.api

import com.nuvio.tv.data.remote.dto.StremioAddonCollectionRequest
import com.nuvio.tv.data.remote.dto.StremioAddonCollectionResultDto
import com.nuvio.tv.data.remote.dto.StremioApiEnvelope
import com.nuvio.tv.data.remote.dto.StremioLoginRequest
import com.nuvio.tv.data.remote.dto.StremioLoginResultDto
import com.nuvio.tv.data.remote.dto.StremioLogoutRequest
import com.nuvio.tv.data.remote.dto.StremioLogoutResultDto
import retrofit2.http.Body
import retrofit2.http.POST

interface StremioAccountApi {
    @POST("api/login")
    suspend fun login(
        @Body request: StremioLoginRequest,
    ): StremioApiEnvelope<StremioLoginResultDto>

    @POST("api/addonCollectionGet")
    suspend fun getAddonCollection(
        @Body request: StremioAddonCollectionRequest,
    ): StremioApiEnvelope<StremioAddonCollectionResultDto>

    @POST("api/logout")
    suspend fun logout(
        @Body request: StremioLogoutRequest,
    ): StremioApiEnvelope<StremioLogoutResultDto>
}
